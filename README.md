# Учебна практика 3 — Service Request Management System

Spring Boot REST приложение за управление на заявки за услуги, разработвано по
[лабораторните упражнения на курса](https://programmingfundamental.github.io/courses/bg/uchebna-praktika-3/)
с ИИ агент (Claude Code).

## Структура

| Път | Съдържание |
|---|---|
| `ServiceRequestManagementSystem/` | Spring Boot 4 / Java 26 / Maven приложението |
| `docs/labN.md` | бележки за всяко упражнение: промени, нова функционалност, решения, проверка |

## Изисквания и стартиране

* JDK 26 (Maven не е нужен — използва се Maven Wrapper)

```bash
cd ServiceRequestManagementSystem
./mvnw spring-boot:run
```

Приложението слуша на `http://localhost:8080`; примерни заявки има в `ServiceRequestManagementSystem/api-requests.http`.

## Упражнения

* [Лаб 1](docs/lab1.md) — начално приложение (POST/GET заявки)
* [Лаб 2](docs/lab2.md) — домейн анализ, value object `CustomerInfo`
* [Лаб 3](docs/lab3.md) — валидация (вкл. условни правила) и обработка на грешки
* [Лаб 4](docs/lab4.md) — жизнен цикъл: започване на обработка, отхвърляне, HTTP 409
* [Лаб 5](docs/lab5.md) — оферта към заявката с изчисляване на цена
* [Лаб 6](docs/lab6.md) — приемане/отказ/изтичане, планиране, изпълнение, отмяна, loyalty отстъпка; review и gap analysis
* [Лаб 7](docs/lab7.md) — unit тестове (`WorkingDays`, `ServiceRequestService` с mock repository)

Тестове: `./mvnw test` (в `ServiceRequestManagementSystem/`).

## API (след лаб 6)

| Метод | Път | Описание |
|---|---|---|
| POST | `/api/requests` | подаване на заявка (→ `SUBMITTED`) |
| GET | `/api/requests` | всички заявки |
| GET | `/api/requests/{id}` | заявка по id (с `execution`, ако е планирана) |
| POST | `/api/requests/{id}/start-processing` | `SUBMITTED` → `PROCESSING` |
| POST | `/api/requests/{id}/reject` | `PROCESSING` → `REJECTED`, тяло `{"reason": "..."}` |
| POST | `/api/requests/{id}/offer` | `PROCESSING` → `OFFERED`, тяло `{"description": "..."}`; цената се изчислява |
| POST | `/api/requests/{id}/offer/accept` | `OFFERED` → `ACCEPTED` (след `validUntil` → 409 и `EXPIRED`) |
| POST | `/api/requests/{id}/offer/decline` | `OFFERED` → `REJECTED` (след `validUntil` → 409 и `EXPIRED`) |
| POST | `/api/requests/{id}/schedule` | `ACCEPTED` → `SCHEDULED`, тяло `{"expectedStart": "...", "expectedEnd": "..."}` |
| POST | `/api/requests/{id}/execution/start` | `SCHEDULED` → `IN_PROGRESS` |
| POST | `/api/requests/{id}/execution/complete` | `IN_PROGRESS` → `COMPLETED`, тяло `{"result": "..."}` |
| POST | `/api/requests/{id}/cancel` | → `CANCELLED` преди изпълнение, тяло `{"reason": "..."}` |

Грешки (ProblemDetail): 400 невалиден вход или неспазено правило за графика, 404 липсваща заявка, 409 недопустима операция за статуса/момента, 500 неочаквана грешка.

Настройки за ръчна проверка: `app.clock.offset` (напр. `P4D`) измества часовника; `app.offer-expiration.interval` (по подразбиране `PT1M`) е интервалът за изтичане на оферти.
