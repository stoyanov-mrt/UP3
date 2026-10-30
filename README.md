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

## API (след лаб 5)

| Метод | Път | Описание |
|---|---|---|
| POST | `/api/requests` | подаване на заявка (→ `SUBMITTED`) |
| GET | `/api/requests` | всички заявки |
| GET | `/api/requests/{id}` | заявка по id |
| POST | `/api/requests/{id}/start-processing` | `SUBMITTED` → `PROCESSING` |
| POST | `/api/requests/{id}/reject` | `PROCESSING` → `REJECTED`, тяло `{"reason": "..."}` |
| POST | `/api/requests/{id}/offer` | `PROCESSING` → `OFFERED`, тяло `{"description": "..."}`; цената се изчислява |

Грешки (ProblemDetail): 400 невалиден вход, 404 липсваща заявка, 409 недопустима операция за статуса, 500 неочаквана грешка.
