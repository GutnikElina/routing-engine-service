# AGENTS.md

Shared agent context for Cursor / coding agents working in this repo.

## Project

Spring Boot **3.5** / Java **21** — `routing-engine-service`.  
Hexagonal (ports & adapters): create route orders, persist them, publish outbox events, compute distances/geometry via OSRM or straight-line fallback.

Stack: Web, JPA + Hibernate Spatial, Flyway, PostgreSQL/PostGIS, MapStruct, Lombok, Resilience4j, RestClient.

## Layout

```
src/main/java/com/logistics/routing/
├── adapter/in/web/           # REST + OpenAPI-generated models
├── adapter/out/osrm/         # OsrmRoutingClient (TRUCK/TRAIN/VESSEL)
├── adapter/out/straightline/ # StraightLineRoutingClient (PLANE)
├── adapter/out/routing/      # RoutingClient, RoutingEngineAdapter, properties
├── adapter/out/persistence/  # JPA + outbox
├── application/              # use cases, ports, routing DTOs
├── config/                   # RoutingClientsConfiguration, Clock, …
├── domain/                   # RouteOrder, exceptions, enums
└── RoutingEngineApplication.java

src/test/java/com/logistics/routing/
├── unit/                     # no Spring
├── integration/              # @SpringBootTest + Testcontainers / WireMock
└── testdata/                 # named factories (CreateRouteTestData, OsrmTestData)
```

## Domain shortcuts

- **Transport**: `TRUCK` / `TRAIN` / `VESSEL` → OSRM; `PLANE` → haversine + average speed.
- **Create route**: validate draft → persist → outbox `RouteCreatedEvent`.
- **Routing port**: `DistanceMatrix` + `RouteGeometry` via `RoutingEngineAdapter` → `Map<TransportType, RoutingClient>`.
- **Exceptions**: `RoutingEngineException`; connectivity → `RoutingEngineUnavailableException` (retries).

## Commands

```bash
mvn -B -ntp verify                          # CI
mvn generate-sources                        # after api/routing-api.yaml changes
mvn test -Dtest=OsrmRoutingClientTest
mvn test -Dtest="*IntegrationTest"          # needs Docker
```

On Windows PowerShell, quote multi-class tests: `"-Dtest=A,B"`.

## Agent rules

1. Match existing package style; no drive-by refactors or unsolicited markdown docs.
2. OpenAPI: `api/routing-api.yaml` → `target/generated-sources/openapi/` (`…adapter.in.web.generated.*`). Run `mvn generate-sources` if types are missing in the IDE.
3. MapStruct: `spring` component model, `unmappedTargetPolicy=ERROR`.
4. Lombok `@UtilityClass`: methods are package-private unless marked `public`.
5. Tests: nested by behavior, factories in `testdata`, AssertJ. Integration tests use Testcontainers PostGIS + `@ServiceConnection` (Docker required). Narrower rules may live in `.cursor/rules/*.mdc`.
6. OSRM unit tests: stub `RestClient` step-by-step; use `body(any(Object.class))` (fluent overload trap). Avoid `RETURNS_DEEP_STUBS` for RestClient.
7. OSRM resilience IT: WireMock + `@EnableWireMock` + profile `test` (`application-test.yml`). `@Retry` / `@CircuitBreaker` apply only via Spring beans — never `new OsrmRoutingClient(...)` for those tests.
8. Retry only `RoutingEngineUnavailableException` (network / reset / timeout). HTTP 4xx/5xx → `RoutingEngineException`, **no** retry. Circuit breaker records `RoutingEngineException`.
9. Connectivity: walk the cause chain for `ResourceAccessException`, `SocketException`, `SocketTimeoutException`.
10. Straight-line geometry: interpolate each leg (`INTERPOLATION_STEPS_PER_LEG = 16`); distance/duration from waypoints, not polyline length. `interpolate` returns endpoints when fraction `≤ 0` / `≥ 1`.

## Config pointers

| What | Where |
|------|--------|
| OSRM URLs / timeouts / plane speed | `routing.*` in `application.yml` |
| Retry / circuit breaker | `resilience4j.*` (`osrm` instance) |
| Fast resilience for IT | `src/test/resources/application-test.yml` |
| DB schema | Flyway, schema `routing` |
| Specs | `docs/architecture/…`, `docs/requirements/…` |
| CI | `.github/workflows/ci.yml` → `mvn -B -ntp verify` |
