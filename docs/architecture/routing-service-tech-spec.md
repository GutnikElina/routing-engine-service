# Техническая реализация и Архитектура

**Документ:** `docs/architecture/routing-service-tech-spec.md`
**Статус:** Approved for Development
**Сервис:** routing-service (Поддомен: Strategic Routing & AI Intelligence)

## 1. Технологический Стек и Зависимости

| Компонент | Технология | Назначение / Обоснование |
| :--- | :--- | :--- |
| **Runtime** | Java 21 (Virtual Threads) | Использование Project Loom для неблокирующего ввода-вывода и эффективных параллельных вычислений. |
| **Framework** | Spring Boot 3.3+ | Базовый каркас приложения (Spring Data, Spring Security, Spring Kafka). |
| **Database** | PostgreSQL 16 + PostGIS | Реляционное хранилище для заказов и гео-данных (Point, LineString, Polygon). |
| **Routing Engine** | OSRM (Self-hosted) / GraphHopper | Локальные Docker-контейнеры с графами дорожных сетей. Быстрый расчет путей без платных внешних API. |
| **VRP Engine** | Google OR-Tools (Java Binding) | Нативная C++ библиотека для эффективного решения комбинаторных задач распределения заказов. |
| **Cache & Locks** | Redis (Redisson) | Кеширование матриц расстояний и использование распределенных блокировок (RLock). |
| **Messaging** | Apache Kafka (Avro Schema Registry) | Асинхронный обмен сообщениями между микросервисами платформы. |
| **Security** | Spring Security + Keycloak | OAuth2 / Resource Server с поддержкой контролей на базе атрибутов (ABAC). |

## 2. Архитектура Приложения (Hexagonal / Clean Architecture)

Сервис проектируется по принципу слоистой архитектуры Ports and Adapters:

```text
┌──────────────────────────────────────────────────────────────┐
│                     INBOUND ADAPTERS                         │
│  [REST Controllers]    [Kafka Consumers]    [gRPC Endpoints] │
└──────────────────────────────┬───────────────────────────────┘
                               │ (Inbound DTOs)
                               ▼
┌──────────────────────────────────────────────────────────────┐
│                      APPLICATION LAYER                       │
│   Use Cases: CreateRouteUseCase, ReRouteOnDelayUseCase       │
└──────────────────────────────┬───────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────┐
│                        DOMAIN LAYER                          │
│   Entities: RouteOrder, RouteSegment, Waypoint               │
│   Services: VRPSolverService, DistanceMatrixCalculator       │
└──────────────────────────────┬───────────────────────────────┘
                               │ (Ports Interfaces)
                               ▼
┌──────────────────────────────────────────────────────────────┐
│                     OUTBOUND ADAPTERS                        │
│  [Postgres Repository]    [OSRM Client]    [Kafka Outbox]    │
└──────────────────────────────────────────────────────────────┘
```

*   **Domain Layer:** Чистый Java-код без зависимостей от Spring. Содержит бизнес-правила и доменные сущности.
*   **Application Layer:** Сценарии использования (Use Cases). Координирует транзакции и вызывает порты инфраструктуры.
*   **Adapters Layer:** Внешние интеграции (контроллеры, базы данных, клиенты внешних API, Kafka-продюсеры).

## 3. Межсервисное Взаимодействие и Интеграции

### 3.1 Схема интеграционных связей

```text
                                       ┌─────────────────────────┐
                                       │   document-ocr-service  │
                                       └────────────┬────────────┘
                                                    │
                                         Kafka: DocumentParsedEvent
                                                    │
                                                    ▼
┌──────────────────────┐  REST/JSON    ┌─────────────────────────┐   Kafka: RouteUpdatedEvent    ┌─────────────────────────┐
│   React Frontend /   ├──────────────►│     routing-service     ├──────────────────────────────►│    telemetry-service    │
│   API Gateway        │               └────────────┬────────────┘                               └────────────┬────────────┘
└──────────────────────┘                            │                                                         │
                                                    │ Kafka: GeofenceCreatedEvent                             │ Kafka:
                                                    │                                                         │ ShipmentDeviatedEvent
                                                    ▼                                                         │
                                       ┌─────────────────────────┐                                            │
                                       │   geofencing-service    │◄───────────────────────────────────────────┘
                                       └─────────────────────────┘
```

### 3.2 Описание контрактов взаимодействия

**Входящие события (Kafka Consumers):**
*   `DocumentParsedEvent` (от `document-ocr-service`): Содержит извлеченные из PDF адреса и реквизиты. `routing-service` сверяет их с черновиком маршрута. При расхождениях переводит рейс в статус `BLOCKED`.
*   `ShipmentDeviatedEvent` (от `telemetry-service`): Сообщает, что транспорт отклонился от геометрии пути более чем на 2 км. `routing-service` автоматически запускает процесс Re-routing.

**Исходящие события (Kafka Producers):**
*   `RouteCreatedEvent` / `RouteUpdatedEvent`: Отправляется при создании или изменении ETA/пути. Потребляется сервисами уведомлений, диспетчерским пультом и клиентским порталом.
*   `GeofenceZoneCreatedEvent`: Отправляется при создании новых гео-зон складов для их регистрации в IoT-модуле трекинга.

## 4. Архитектурные Паттерны и Надежность

### 4.1 Гарантия отправки событий: Transactional Outbox Pattern
1.  Прямой вызов `KafkaTemplate.send()` внутри бизнес-транзакции ЗАПРЕЩЕН.
2.  Изменения доменного агрегата `RouteOrder` пишутся в основные таблицы PostgreSQL.
3.  В этой же единой ACID-транзакции событие в формате JSON записывается в таблицу `outbox_events`.
4.  Фоновый процесс (Debezium CDC или Scheduled Outbox Worker) считывает необработанные записи из `outbox_events`, публикует их в Kafka и помечает как отправленные.

### 4.2 Защита от состязательности (Race Conditions)
Для предотвращения параллельного изменения одного и того же маршрута двумя диспетчерами или автоматическим событием от GPS-трекера используется Distributed Lock на базе Redis (Redisson):
*   **Ключ блокировки:** `lock:route:{routeOrderId}`.
*   **Время ожидания блокировки (Wait Time):** 3 секунды.

### 4.3 Безопасность и разграничение доступа (ABAC)
*   Аутентификация выполняется на уровне Spring Security Client Credentials / OAuth2 JWT.
*   Реализуется Attribute-Based Access Control (ABAC). Диспетчер имеет доступ на редактирование маршрута только в случае, если атрибут `region` в его JWT-токене совпадает с регионом начальной или конечной точки маршрута.

## 5. Требования к Хранению Данных (Database Guidelines)

### 5.1 Пространственные данные (PostGIS)
*   Все координаты хранятся в системе координат WGS 84 (SRID 4326).
*   Для поиска гео-точек и пересечений полигонов ОБЯЗАТЕЛЬНО использование пространственных GiST-индексов (`USING GIST`).

### 5.2 Оптимизация и кеширование
*   **PostgreSQL:** Для исторически завершенных заказов (`COMPLETED` / `CLOSED`) настраивается декларативное партицирование таблиц по месяцам (`PARTITION BY RANGE (created_at)`).
*   **Redis:** Кеширование ответов матрицы расстояний от OSRM (ключ: `hash(points_array)`, TTL: 24 часа) для исключения дублирующих вычислений.

## 6. Observability и Эксплуатация

*   **Metrics (Micrometer + Prometheus):**
    *   `routing_calculation_duration_seconds` (timer) — время расчета маршрута OSRM/OR-Tools.
    *   `routing_replan_total` (counter) — количество аварийных пересчетов пути.
*   **Tracing (OpenTelemetry / Jaeger):** Проброс заголовка `traceparent` сквозь HTTP, gRPC и Kafka headers для сквозной трассировки запросов.
*   **Logging:** Логирование строго в формате Structured JSON (Logback + Logstash encoder) с обязательными полями `trace_id`, `span_id`, `order_number`.
