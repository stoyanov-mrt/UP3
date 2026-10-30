# Лабораторно упражнение 4 — Итеративно уточняване на решение при работа с ИИ

## Теория накратко

* **Iterative refinement** — решението се уточнява постепенно: първоначално предложение → оценка → follow-up prompt → уточнено предложение.
* **Follow-up prompt** — не повтаря целия контекст, а казва само какво се променя/уточнява/коригира.
* **Conversational context** (какво е казано в сесията) ≠ **project context** (реалното състояние на файловете). Наличието на нещо в разговора не гарантира, че ще бъде приложено правилно.
* При всяка итерация: уточнението приложено ли е на правилното място, запазени ли са валидните изисквания, не се ли ползват отменени решения, няма ли нещо извън scope.

## Подаден контекст

`ServiceRequest`, `RequestStatus`, `ServiceRequestService`, `ServiceRequestController`, `GlobalExceptionHandler`, `ServiceRequestNotFoundException`. Не са подадени DTO валидаторът, `CustomerInfo`, repository, `pom.xml` — не участват в жизнения цикъл. Всички стъпки са в една сесия.

## Итерация 1 — първоначален анализ

**Преходи:**

```
SUBMITTED ──startProcessing──▶ PROCESSING ──createOffer──▶ OFFERED
                                   │
                                   └──reject(reason)──▶ REJECTED (крайно)
SUBMITTED/PROCESSING? ──deadline──▶ EXPIRED
```

| Слой | Отговорност |
|---|---|
| Domain (`ServiceRequest`) | проверява дали преходът е допустим и сменя статуса; пази rejectionReason |
| Service | зарежда заявката (404), вика домейн операцията, транзакция |
| Controller | endpoints за командите, вход (причина) |
| Exception handler | недопустим преход → 409 |

**Сега:** start processing, reject + причина, 409. **Отложено:** Offer/OFFERED (няма модел на офертата), EXPIRED.

**Липсваща информация (не е предполагана):**
* какъв е „определеният срок“ за EXPIRED — от кой момент, колко време, от кои статуси;
* кой/какво прави прехода към EXPIRED (scheduler? при четене?);
* какво съдържа офертата;
* дали служителят трябва да се идентифицира (няма автентикация).

Оценка: анализът не предлага реализация на Offer/EXPIRED в текущата итерация и не измисля срок за изтичане.

## Итерация 2 — уточнен scope (follow-up prompt)

Follow-up-ът стеснява scope-а до 4 точки и задава дизайна (правилата в домейна, service = load → domain op → persist). Уточненото предложение:

| | |
|---|---|
| Domain операции | `startProcessing()`, `reject(String reason)` → `InvalidStatusTransitionException` |
| Service операции | `startProcessing(id)`, `reject(id, reason)` — `@Transactional` |
| REST | `POST /api/requests/{id}/start-processing`, `POST /api/requests/{id}/reject` с `{"reason": "..."}` |
| Изключения → HTTP | `MethodArgumentNotValidException` → 400, `ServiceRequestNotFoundException` → 404, `InvalidStatusTransitionException` → 409 |
| Транзакция | един service метод = една транзакция; промяната се записва чрез JPA dirty checking при commit |

Сравнение с итерация 1: OFFERED/EXPIRED/Offer са премахнати; запазени са валидните решения (къде са правилата, 404/400 от лаб 3); не са добавени допълнителни статуси или endpoints.

### Защо `POST .../start-processing`, а не `PATCH {status}`
Преходите са **бизнес команди** с правила. `PATCH`/`PUT` на статуса би позволил на клиента да поиска произволен статус (напр. директно `REJECTED` без причина) и би преместил правилата към валидация на вход. Отделните действия правят допустимите операции явни.

## Разграничаване на грешките

| Ситуация | Код | Защо |
|---|---|---|
| празна причина за отхвърляне | 400 | невалидни **входни данни** — клиентът трябва да поправи заявката |
| несъществуващ id | 404 | **липсващ ресурс** |
| start processing на не-SUBMITTED | 409 | заявката е валидна, но **конфликтира с текущото състояние** на ресурса |
| reject на не-PROCESSING | 409 | същото |

Ред на проверките: входът се валидира преди зареждане на заявката, затова празна причина към несъществуваща/неподходяща заявка дава 400.

## Impact analysis

| Файл | Промяна |
|---|---|
| `entity/RequestStatus` | + `PROCESSING`, `REJECTED` |
| `entity/ServiceRequest` | + `rejectionReason`, `startProcessing()`, `reject()` |
| `entity/InvalidStatusTransitionException` (нов) | домейн изключение — хвърля се от entity-то, затова е в неговия пакет |
| `dto/RejectServiceRequestDto` (нов) | `@NotBlank @Size(max=255) reason` |
| `dto/ServiceRequestResponseDto` | + `rejectionReason` (добавено поле; съществуващите не са променени) |
| `service/ServiceRequestService` | 2 нови метода + общ `getRequest(id)` |
| `controller/ServiceRequestController` | 2 нови endpoints |
| `controller/GlobalExceptionHandler` | 409 mapping |
| **Непроменени** | create DTO и валидаторът, `CustomerInfo`, repository, конфигурация |

## Итерация 3 — оценка на домейн модела след реализацията

| Въпрос | Отговор след итерация 2 |
|---|---|
| Как може да се промени `status`? | през `startProcessing()`/`reject()` **и през публичния `setStatus()`** (Lombok `@Setter` на класа) |
| Може ли `rejectionReason` да се зададе извън `reject()`? | **да** — `setRejectionReason()` |
| Кой определя началния статус и `submittedAt`? | service-ът, чрез setters — домейнът не го гарантира |
| Може ли да се заобиколи lifecycle-ът? | **да** — `setStatus(REJECTED)` без причина, `setStatus(SUBMITTED)` на отхвърлена заявка |

**Установен проблем:** setters заобикалят правилата. **Follow-up (минимална корекция):**
* махнат е `@Setter`; no-args конструкторът е `protected` (нужен само на JPA);
* нов публичен конструктор приема само данните на клиента и сам задава `SUBMITTED` и `submittedAt`;
* service-ът създава заявката чрез конструктора.

Сега единствените начини за промяна на състоянието са конструкторът и двете lifecycle операции. Не е правен друг refactoring.

## Проверка на поведението (след итерация 3)

| Сценарий | Очаквано | Действително |
|---|---|---|
| start processing на SUBMITTED | 200, `PROCESSING` | 200, `PROCESSING` |
| повторно start processing | 409 | 409 |
| reject на PROCESSING с причина | 200, `REJECTED` + причина | 200 |
| reject с празна причина `"  "` | 400 | 400 (`errors.reason`) |
| reject без тяло | 400 | 400 |
| reject на SUBMITTED | 409 | 409 |
| reject / start на REJECTED (няма възстановяване) | 409 | 409 |
| start / reject на id 999 | 404 | 404 |
| GET след reject | статусът и причината са записани | да |

Сценариите са изпълнени и след итерация 2, и след корекцията от итерация 3 — с еднакви резултати.
