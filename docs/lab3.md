# Лабораторно упражнение 3 — Ограничения и критична оценка при разработване с ИИ

## Теория накратко

* **Constraints** — условия и граници за решението (бизнес правила, технологии, API договор, архитектура).
* **Negative constraints** — какво *не* трябва да се прави (напр. „запази REST API-то“).
* Изискванията могат да са непълни или **противоречиви** → интерпретацията на ИИ се оценява критично, не се приема сляпо.
* **Structured output** — еднаква структура на отговора за всеки анализиран елемент; улеснява откриването на пропуски.
* **Input validation** (данните спазват правилата) ≠ **verification** (решението отговаря на изискванията) ≠ **software validation** (решението покрива реалната нужда).

## Преглед на текущото решение (преди промени)

| Въпрос | Състояние след лаб 2 |
|---|---|
| Къде се валидира входът | `CreateServiceRequestDto` (record) |
| Bean Validation | `@NotBlank` за name/email/description, `@Email`, `@NotNull` за трите enum-а |
| `@Valid` в контролера | да, на `POST /api/requests` |
| Бизнес проверки в service | няма |
| Обработка на изключения | няма собствена; `ResponseStatusException(404)` в service-а, всичко друго — default на Spring Boot |
| HTTP кодове / тела (проверено с curl) | невалиден вход, лош enum и счупен JSON → 400; липсващ id → 404; **ON_SITE без адрес → 201** (правилото липсва); тялото е само `{timestamp,status,error,path}` — не казва кое поле е грешно |

Подаден контекст за анализа: `CreateServiceRequestDto`, `ServiceRequestController`, `ServiceRequestService`, `ServiceRequest`. Не са нужни repository, enums (имената им са видими от DTO), `pom.xml`.

## Анализ на изискванията (structured output)

| Изискване | Класификация | Къде | Обосновка |
|---|---|---|---|
| customerEmail задължителен и валиден | Bean Validation | request DTO (`@NotBlank @Email`) | ограничение върху едно поле, не зависи от състояние |
| description задължително, min/max дължина | Bean Validation | request DTO (`@NotBlank @Size`) | едно поле; **конкретните граници не са дадени** — виж по-долу |
| ON_SITE → непразен address | domain/business validation — **двусмислено** | service или class-level constraint | зависи от комбинация от полета; може да е и Bean Validation (class-level) |
| URGENT → непразен urgencyReason | като горното | като горното | като горното |
| нова заявка → SUBMITTED | system-managed value | service (при създаване) | клиентът не подава статус — DTO-то няма такова поле |
| submittedAt от системата | system-managed value | service | същото |
| липсващ ресурс → 404 | API exception-mapping | `@RestControllerAdvice` | service хвърля домейн изключение, HTTP кодът е грижа на API слоя |
| невалиден вход → 400 | API exception-mapping | handler за `MethodArgumentNotValidException` | |
| счупен JSON / невалиден enum → 400 | API exception-mapping | handler за `HttpMessageNotReadableException` | грешката е при десериализация — *преди* Bean Validation, затова `@Valid` не може да я хване |
| неочаквана грешка → 500 без вътрешно съобщение | API exception-mapping | catch-all `@ExceptionHandler(Exception.class)` | съобщението се логва, но не се връща |

**Поле vs комбинация от полета:** `@NotBlank @Email` на `customerEmail` проверява само самото поле. `ON_SITE → address` не може да се изрази с анотация на едно поле — нужен е достъп до целия обект. Примерът `INSTALLATION → ON_SITE` от заданието **не** е изискване и не е реализиран.

**Бизнес изискване ≠ технически механизъм:** това, че правилото идва от бизнеса, не значи автоматично, че трябва да е в service слоя — зависи от това какви данни са нужни за проверката (тук: само входните данни, без база и без състояние).

## Експеримент с противоречиво изискване

> *Invalid customer input should return HTTP 500 Internal Server Error.*

* **Противоречи** на вече зададеното „invalid input data returns HTTP 400“.
* Последствия: 5xx значи „сървърът сбърка“ — клиентът не разбира, че трябва да поправи данните; мониторингът брои грешки на клиента като сривове; retry логики в клиенти/proxy-та ще повтарят заявки, които никога няма да минат; нарушава HTTP семантиката.
* Решение: изискването се **отхвърля** и не влиза в реализацията. Като агент не предлагам промяна на кода — беше поискан само анализ.

## Trade-offs: service-level vs class-level Bean Validation

| Критерий | Service-level проверка | Class-level custom constraint на DTO |
|---|---|---|
| Отговорност | правилото е до бизнес логиката; service-ът знае за ExecutionMode/Priority | правилото е до останалите входни правила; DTO-то описва „валиден вход“ |
| Повторна употреба | работи за всеки извикващ service-а (REST, бъдещ batch, тестове) | важи само там, където DTO-то се валидира (`@Valid`) |
| Докладване на грешки | отделно изключение и handler; полевите и условните грешки идват в **два** отделни отговора | всички грешки в **един** 400 отговор, във формата на полевите грешки |
| Сложност | ниска — два `if`-а + изключение + mapping | средна — анотация + `ConstraintValidator` + `addPropertyNode` |
| Поддръжка | лесно се намира в service-а; рискът е service-ът да се напълни с входни проверки | правилата за входа са на едно място; при нов DTO (напр. update) трябва да се сложи анотацията и там |

Оценка на сравнението: няма unsupported claims — „повторна употреба“ при service-а е реално предимство само ако има други входни точки освен REST, каквито в момента няма.

### Избран подход: class-level custom Bean Validation constraint

* Правилата зависят **само от входните данни** — без база и без текущо състояние на заявката.
* Клиентът получава **всички** грешки наведнъж (напр. невалиден email + липсващ адрес + липсваща причина) в един и същ формат.
* Няма друга входна точка освен REST API-то, така че предимството на service-level за повторна употреба не се използва.
* Изборът не е универсално правило: правило, зависещо от данни в базата или от статуса (напр. lifecycle в лаб 4), принадлежи на домейна/service-а.

## Impact analysis

| Файл | Промяна |
|---|---|
| `dto/CreateServiceRequestDto` | `@Size` ограничения + `@ConditionalRequestFields` |
| `dto/validation/ConditionalRequestFields` (нов) | анотацията |
| `dto/validation/ConditionalRequestFieldsValidator` (нов) | логиката за ON_SITE/URGENT |
| `service/ServiceRequestNotFoundException` (нов) | домейн изключение вместо `ResponseStatusException` |
| `service/ServiceRequestService` | хвърля новото изключение |
| `controller/GlobalExceptionHandler` (нов) | 400 / 404 / 500 като ProblemDetail |
| **Непроменени** | controller endpoints, entity, persistence, response DTO, repository |

## Реализация — решения

* **Граници на description: 10–2000.** Заданието казва „минимална и максимална дължина“ без стойности → **липсващ контекст**. Максимумът е равен на дължината на колоната (2000); минимумът 10 е предположение („поправка“ не е описание), отбелязано тук и лесно за промяна.
* **`@Size(max = 255)` за name, email, address, urgencyReason** — колоните са `varchar(255)`; без ограничението твърде дълъг вход би стигнал до базата и дал 500, което нарушава „невалиден вход → 400“.
* **Условните грешки са закачени за конкретното поле** (`addPropertyNode("address")`), затова в отговора изглеждат като обикновени полеви грешки.
* **ProblemDetail (RFC 9457)** за всички грешки — стандартният формат на Spring 7; полевите грешки са в свойството `errors`.
* **`ResponseStatusException` → `ServiceRequestNotFoundException`** — HTTP кодът вече не е в service слоя; mapping-ът е в API слоя, където е и останалата обработка на грешки.
* **`HttpMessageNotReadableException` се презаписва** с общо съобщение — оригиналното съдържа вътрешни имена на класове (Jackson/enum).
* `ResponseEntityExceptionHandler` се наследява, за да запазят стандартните си 4xx кодове всички други MVC грешки (грешен тип на id, непознат път и др.), вместо да паднат в catch-all 500.

## Проверка на API поведението

| Сценарий | Очаквано | Действително | Тяло |
|---|---|---|---|
| валидна заявка | 201 | 201 | заявката, `SUBMITTED` |
| невалиден email | 400 | 400 | `errors.customerEmail` |
| кратко description | 400 | 400 | `errors.description` |
| ON_SITE с адрес `"   "` | 400 | 400 | `errors.address` |
| URGENT без причина | 400 | 400 | `errors.urgencyReason` |
| три грешки наведнъж | 400 | 400 | и трите полета в един отговор |
| невалиден enum (`PAINTING`) | 400 | 400 | общо съобщение, без имена на класове |
| счупен JSON | 400 | 400 | общо съобщение |
| несъществуващ id | 404 | 404 | `Service request 999 not found` |
| id = `abc` | 400 | 400 | стандартен ProblemDetail |
| неочаквано изключение* | 500 | 500 | `An unexpected error occurred` — вътрешното съобщение не се показва |

\* За 500 временно беше добавен endpoint, който хвърля изключение с „тайно“ съобщение; след проверката е изтрит и не е в commit-а.

Няма несъответствия между очаквано и действително поведение, затова не беше нужен root-cause analysis.
