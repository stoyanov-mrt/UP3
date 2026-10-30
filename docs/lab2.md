# Лабораторно упражнение 2 — Анализ и развитие на домейн модела

## Теория накратко

* **Видове контекст:** функционален, домейн, технологичен, архитектурен, от съществуващи артефакти.
* **Relevant context / scope:** даваме само информацията, нужна за конкретната задача, и ясно казваме какво остава извън нея.
* **Analysis before action:** първо анализ и предложение без промяна на кода, после реализация само на одобреното.
* **Impact analysis:** кои части на решението се засягат от промяната → по тях се подбира контекстът за реализацията.
* **Minimal change:** само промените, нужни за задачата.
* **Entity** има идентичност и жизнен цикъл; **value object** няма идентичност и принадлежи на друг обект; **enum** е затворено множество от стойности.

## Подбор на контекст за анализа

| Вид | Съдържание |
|---|---|
| Функционален | клиентът подава заявка; ON_SITE → адрес; URGENT → обосновка; служител стартира обработка SUBMITTED → PROCESSING |
| Домейн | заявка, клиент (име + email), видове услуга/изпълнение/приоритет, статус |
| Релевантни файлове | `ServiceRequest`, `CreateServiceRequestDto`, `ServiceRequestResponseDto`, `ServiceType`, `ExecutionMode`, `Priority`, `RequestStatus` |
| **Не** са подадени | controller, repository, `application.properties`, `pom.xml` — не описват домейна |
| Ненужна за анализа бъдеща информация | оферта, планиране и изпълнение на услугата (извън scope) |

## Домейн анализ (резултат от промпта „Analyse the following business requirements…“)

| Понятие | Отговорност | Класификация |
|---|---|---|
| `ServiceRequest` | заявката за услуга; пази данните и статуса си; има id и жизнен цикъл (SUBMITTED → PROCESSING → …) | **Entity** |
| Клиентска информация (име, email) | контактни данни на подателя *за тази заявка* | **Value object** (виж по-долу) |
| `ServiceType`, `ExecutionMode`, `Priority` | класификация на заявката | **Enum** — затворени множества |
| `RequestStatus` | етап от жизнения цикъл; трябва да се разшири с `PROCESSING` | **Enum** |
| `address`, `urgencyReason`, `description` | свободен текст, условно задължителни (address при ON_SITE, urgencyReason при URGENT) | атрибути (String) |

**Отделно entity `Customer` ли да е клиентът?** Не в текущия scope:
* в изискванията няма регистрация, вход, редакция на клиент или „всички заявки на клиент“ — няма самостоятелен жизнен цикъл;
* един и същ човек може да подаде заявки с различно име/email; данните описват подателя *към момента на заявката*;
* entity би довело до нова таблица, repository, връзка `@ManyToOne` и въпроси за уникалност по email — сложност без бизнес нужда.

Все пак групирането на име + email в отделно понятие е полезно: те винаги вървят заедно и представят една стойност („кой е подал заявката“). Затова — **value object**.

Отхвърлени/отложени идеи: entity `Customer` с repository; value object `Address` (няма изискване за структуриран адрес); моделиране на `Offer` и `ServiceExecution` (извън scope).

## Одобрено проектно решение

* `CustomerInfo` — immutable value object с `customerName` и `customerEmail`, принадлежи към `ServiceRequest`, не е entity.
* `address`, `description`, `urgencyReason` остават `String`.
* Външният API запазва плоските полета `customerName` и `customerEmail`.
* `Offer` и `ServiceExecution` не се реализират.

Решението съвпада с анализа по-горе.

## Impact analysis

| Засегнато | Защо |
|---|---|
| `entity/CustomerInfo` (нов) | новият value object |
| `entity/ServiceRequest` | `customerEmail` → `@Embedded CustomerInfo`; добавени `address`, `urgencyReason` |
| `dto/CreateServiceRequestDto` | нови входни полета `customerName`, `address`, `urgencyReason` |
| `dto/ServiceRequestResponseDto` | същите полета в изхода |
| `service/ServiceRequestService` | mapping DTO ↔ entity (тук е mapping-ът) — **допълнителен файл**, който не беше нужен за анализа, но е нужен за реализацията |
| Persistence | `@Embeddable` → колоните `customer_name`, `customer_email` остават в таблицата `service_requests`; няма нова таблица |
| **Без промяна** | controller, repository, enums, `pom.xml`, `application.properties` |

**Открит липсващ контекст:** одобреното решение казва „address и urgencyReason *остават* String“, но в модела от лаб 1 тези полета (и `customerName`) не съществуват — там бяха зададени само id, customerEmail, description, serviceType, executionMode, priority, status, submittedAt. Затова в тази итерация те се **добавят** като String полета. Условната им задължителност (ON_SITE/URGENT) не е част от одобрената промяна и е оставена за лаб 3.

## Реализация

* `CustomerInfo` е **Java record**, анотиран с `@Embeddable` (Hibernate 7 поддържа records като embeddable). Record е immutable по дефиниция — няма setters, полетата са final; не е нужен Lombok.
* Мястото му е в пакета `entity` — той е част от домейн/persistence модела, а зададената пакетна структура няма отделен пакет за value objects. Няма repository за него.
* `@Column(name = "customer_email")` запазва името на колоната от лаб 1.
* `customerName` е `@NotBlank` във входния DTO — името е задължително по бизнес контекста, а колоната е `NOT NULL`; без това липсващо име би дало 500 от базата вместо 400.
* `RequestStatus.PROCESSING` и операцията „започване на обработка“ **не** са добавени: присъстват в бизнес контекста, но не и в одобрената промяна. (Реализирани са в лаб 4.)

## Проверка

* Компилиране и стартиране — успешно.
* POST с `customerName`, `customerEmail`, `address`, `urgencyReason` → 201; GET `/api/requests/1` връща същите стойности → записването и четенето през `CustomerInfo` работят.
* Външният JSON продължава да съдържа плоските `customerName` и `customerEmail` (няма вложен обект `customerInfo`).
* Генериран DDL (Hibernate SQL лог): една таблица `service_requests` с колони `customer_name`, `customer_email`; няма отделна таблица за клиента.
