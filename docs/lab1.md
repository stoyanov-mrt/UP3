# Лабораторно упражнение 1 — Влияние на контекста върху генерирането на софтуер с ИИ

> ИИ агент в упражнението: Claude (Claude Code), вместо DevoxxGenie + Ollama/Ministral 3 3B от заданието.
> Принципите (задача → контекст → анализ → решение → реализация → проверка → уточняване) не зависят от конкретния асистент.

## Теория накратко

| Понятие | Смисъл |
|---|---|
| Prompt vs контекст | Промптът казва *какво* се иска; контекстът дава *информацията*, нужна за изпълнението. |
| Structured prompting | Task / Context / Constraints / Expected output. |
| Assumption | Избор, който моделът прави при липсваща информация. Не е непременно грешка, но трябва да се разпознае. |
| Unsupported claim / hallucination | Твърдение без опора / измислена информация, представена като факт. |
| Непоискано решение | Функционалност или компонент, които не са изискани (дори да са технически коректни). |
| Verification | Резултатът се приема чак след преглед на кода, компилиране, стартиране и проверка на поведението. |

## Експерименти 1 и 2 (само анализ)

По избор при старта на работата експерименти 1 и 2 са документирани като анализ, без код в хранилището (заданието позволява резултатите им да не се приемат).

### Експеримент 1 — `Create REST app that handles client requests.`

Без контекст всеки съществен избор е **предположение** на агента:

| Аспект | Какво бих избрал като агент | Произтича от задачата? |
|---|---|---|
| Език / framework | Java + Spring Boot (или Node/Express, Python/Flask — промптът не казва) | Не — assumption |
| Build tool | Maven или Gradle | Не — assumption |
| Значение на „client requests“ | Двусмислено: HTTP заявки от клиенти или бизнес „заявки на клиенти“ | Не — assumption |
| Модел на заявката | Generic `Request {id, clientName, description, status, createdAt}` | Не — assumption |
| Операции | Пълен CRUD (GET/POST/PUT/DELETE) | Само „handles“ — CRUD е непоискано решение |
| Съхранение | In-memory списък или H2 | Не — assumption |

**Липсваща информация:** технологии, предметна област, полета на модела, нужни операции, архитектура, кой какво може да прави.

### Експеримент 2 — технологичен контекст (Java 26, Maven, Spring Web, JPA, Lombok, H2, validation)

* **Ограничени предположения:** език, build tool, framework, база данни, ORM, наличие на валидация, името на приложението.
* **Остават предположения:** полетата на ServiceRequest (напр. title, customerName, phone), стойностите на статуса (OPEN/IN_PROGRESS/CLOSED…), пакетна структура (по слоеве или по feature), използване на DTO или директно entity, набор от endpoints (обикновено пълен CRUD — PUT/DELETE не са искани), обработка на грешки.
* **Извод:** технологичният контекст определя *с какво* се строи, но не и *какво* се строи.

## Експеримент 3 — функционален + технологичен + архитектурен контекст (реализиран)

Начално състояние: минимален Maven проект (`com.example:ServiceRequestManagementSystem`, Java 26, без зависимости) — отделен commit.
Подаден е структурираният промпт от заданието с `pom.xml` в контекста.

### Какво е създадено

| Файл | Роля |
|---|---|
| `pom.xml` | Spring Boot 4.1.1 parent; webmvc, data-jpa, validation, H2, Lombok 1.18.46 |
| `ServiceRequestManagementSystemApplication` | входна точка |
| `entity/ServiceRequest` | JPA entity с точно зададените 8 свойства |
| `entity/ServiceType, ExecutionMode, Priority, RequestStatus` | enums със зададените стойности (`RequestStatus` има само `SUBMITTED`) |
| `dto/CreateServiceRequestDto` | вход от клиента — **без** id, status, submittedAt |
| `dto/ServiceRequestResponseDto` | изход към клиента |
| `repository/ServiceRequestRepository` | `JpaRepository<ServiceRequest, Long>` |
| `service/ServiceRequestService` | create / findAll / findById; задава `SUBMITTED` и `submittedAt` |
| `controller/ServiceRequestController` | `POST /api/requests`, `GET /api/requests`, `GET /api/requests/{id}` |
| `application.properties` | in-memory H2 база |

### Решения и защо

* **Пакети `controller, dto, entity, repository, service` директно под `com.example`** — точно по зададения архитектурен контекст. Enums са в `entity`, защото не е зададен отделен пакет за тях и са част от модела.
* **DTO като Java `record`** — immutable, без boilerplate; Lombok се използва за entity (`@Getter/@Setter/@NoArgsConstructor`, нужни на JPA) и за constructor injection (`@RequiredArgsConstructor`).
* **Системните стойности се задават в service слоя** — входният DTO физически няма тези полета, затова клиентът не може да ги подаде (проверено: подадените `id: 99` и `status: PROCESSING` се игнорират).
* **Валидация:** минимални анотации (`@NotBlank @Email`, `@NotNull` за enums) + `@Valid` — в контекста е поискано „validation“; по-подробни правила идват в лаб 3.
* **404 чрез `ResponseStatusException`** — най-простият начин GET по несъществуващ id да не връща 500; без глобален exception handler (не е искан).
* **`annotationProcessorPaths` за Lombok** — от JDK 23 annotation processing не се пуска неявно; без това Lombok не генерира getters/setters.
* **Spring Boot 4.1.1** — актуалната версия с поддръжка на Java 26; стартерът за REST в Boot 4 е `spring-boot-starter-webmvc`.

### Оценка спрямо критериите

* Всички поискани endpoints и свойства са реализирани; няма PUT/DELETE, Swagger, H2 console, security и др. непоискани неща.
* Остават самостоятелни решения на агента: дължина на `description` колоната (2000), HTTP 201 при създаване, `LocalDateTime` за `submittedAt`, `IDENTITY` генериране на id, формат на грешките (default на Spring).

## Проверка (verification)

Компилиране с `./mvnw package`, стартиране и ръчни заявки (curl; файлът `api-requests.http` съдържа същите сценарии за IntelliJ/Postman):

| Сценарий | Резултат |
|---|---|
| POST валидна заявка (с опит за подаване на `id`/`status`) | 201, `status=SUBMITTED`, id и време от системата |
| GET /api/requests | 200, списък с 1 заявка |
| GET /api/requests/1 | 200 |
| GET /api/requests/42 | 404 |
| POST с невалиден email и липсващи полета | 400 |

**Открит проблем при проверката:** `submittedAt` в отговора на POST беше с наносекунди (`...29.7210387`), а след четене от базата — с микросекунди (`...29.721039`).
*Root cause:* H2 `TIMESTAMP` пази 6 знака, а `LocalDateTime.now()` на Windows/JDK 26 дава 7.
*Минимална корекция:* `LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)` в service-а. Повторна проверка: POST и GET връщат една и съща стойност.

## Стартиране

```bash
cd ServiceRequestManagementSystem
./mvnw spring-boot:run
```
