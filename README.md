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
