# Tasks: Weather Tracing PoC

**Input**: Design documents from `/specs/001-weather-tracing-poc/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Tests are included per Constitution requirements (TDD non-negotiable, 80% coverage minimum, 100% for domain layer).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

This is a **web application** with multiple services:
- `frontend/src/` - Vue.js 3 frontend
- `gateway/src/main/java/com/example/gateway/` - Spring Cloud Gateway
- `weather-service/src/main/java/com/example/weather/` - Weather Service (Hexagonal)
- `observability/` - Observability stack configuration
- `k8s/` - Kubernetes configuration
- `scripts/` - Deployment scripts

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization, build configuration, and observability infrastructure

- [x] T001 Create root project structure with settings.gradle and docker-compose files
- [x] T002 [P] Initialize weather-service Gradle project with Spring Boot 3.2.x, Spring Data JPA, H2, OpenTelemetry dependencies in weather-service/build.gradle
- [x] T003 [P] Initialize gateway Gradle project with Spring Cloud Gateway 4.1.x, OpenTelemetry dependencies in gateway/build.gradle
- [x] T004 [P] Initialize frontend project with Vue.js 3.x, Vite 5.x, TypeScript, Axios in frontend/package.json
- [x] T005 [P] Configure Java 21 compilation settings and Gradle wrapper in gradle/wrapper/gradle-wrapper.properties
- [x] T006 [P] Configure TypeScript and Vite settings in frontend/tsconfig.json and frontend/vite.config.ts
- [x] T007 Create OpenTelemetry Collector configuration in observability/otel-collector/otel-collector-config.yaml
- [x] T008 [P] Create Prometheus configuration in observability/prometheus/prometheus.yml
- [x] T009 [P] Create Jaeger configuration in observability/jaeger/jaeger-config.yml
- [x] T010 [P] Create Grafana datasource configuration in observability/grafana/provisioning/datasources/datasource.yml
- [x] T011 Create docker-compose.dev.yml for observability stack (Mode 1)
- [x] T012 Create docker-compose.yml for full stack deployment (Mode 2)

**Checkpoint**: Project structure created, all services can be built independently

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**WARNING**: No user story work can begin until this phase is complete

### Weather Service Foundation

- [x] T013 Create WeatherServiceApplication.java main class in weather-service/src/main/java/com/example/weather/WeatherServiceApplication.java
- [x] T014 Create application.yml with H2, actuator, and OTel configuration in weather-service/src/main/resources/application.yml
- [x] T015 Create H2 database schema in weather-service/src/main/resources/schema.sql
- [x] T016 Create seed data for 3 cities (TPE, TXG, KHH) in weather-service/src/main/resources/data.sql
- [x] T017 Create ObservabilityConfig for OTel instrumentation in weather-service/src/main/java/com/example/weather/infrastructure/config/ObservabilityConfig.java
- [x] T018 Create GlobalExceptionHandler for error responses in weather-service/src/main/java/com/example/weather/infrastructure/web/GlobalExceptionHandler.java

### Gateway Foundation

- [x] T019 Create GatewayApplication.java main class in gateway/src/main/java/com/example/gateway/GatewayApplication.java
- [x] T020 Create application.yml with routes and OTel configuration in gateway/src/main/resources/application.yml
- [x] T021 Create RouteConfig for weather service routing in gateway/src/main/java/com/example/gateway/config/RouteConfig.java
- [x] T022 Create CorsConfig for frontend access in gateway/src/main/java/com/example/gateway/config/CorsConfig.java
- [x] T023 Create ObservabilityConfig for gateway tracing in gateway/src/main/java/com/example/gateway/config/ObservabilityConfig.java

### Frontend Foundation

- [x] T024 Create main.ts entry point in frontend/src/main.ts
- [x] T025 Create App.vue root component in frontend/src/App.vue
- [x] T026 Create TypeScript types for API responses in frontend/src/types/weather.ts
- [x] T027 Create axios instance with interceptors in frontend/src/services/weatherApi.ts

### Health Endpoint Verification

- [x] T027a [P] Integration test for Weather Service /actuator/health endpoint in weather-service/src/test/java/com/example/weather/infrastructure/HealthEndpointTest.java
- [x] T027b [P] Integration test for Gateway /actuator/health endpoint in gateway/src/test/java/com/example/gateway/HealthEndpointTest.java

**Checkpoint**: Foundation ready - all services start, database initialized, routing configured, health endpoints verified

---

## Phase 3: User Story 1 - Query City Weather (Priority: P1) MVP

**Goal**: Users can select one of three cities and view current weather (temperature, rainfall) with random variation per query

**Independent Test**: Click any city button → weather data appears with temperature (15-35°C) and rainfall (0-50mm). Repeated queries show different values.

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation (TDD Red-Green-Refactor)**

- [x] T028 [P] [US1] Unit test for CityCode value object validation in weather-service/src/test/java/com/example/weather/domain/value_objects/CityCodeTest.java
- [x] T029 [P] [US1] Unit test for Temperature value object with clamping in weather-service/src/test/java/com/example/weather/domain/value_objects/TemperatureTest.java
- [x] T030 [P] [US1] Unit test for Rainfall value object with clamping in weather-service/src/test/java/com/example/weather/domain/value_objects/RainfallTest.java
- [x] T031 [P] [US1] Unit test for City entity creation in weather-service/src/test/java/com/example/weather/domain/entities/CityTest.java
- [x] T032 [P] [US1] Unit test for WeatherCalculationService random variation in weather-service/src/test/java/com/example/weather/domain/services/WeatherCalculationServiceTest.java
- [x] T033 [P] [US1] Unit test for GetWeatherUseCase in weather-service/src/test/java/com/example/weather/application/use_cases/GetWeatherUseCaseTest.java
- [x] T034 [P] [US1] Unit test for WeatherMapper transformations in weather-service/src/test/java/com/example/weather/application/mappers/WeatherMapperTest.java
- [x] T035 [P] [US1] Integration test for H2WeatherRepository in weather-service/src/test/java/com/example/weather/infrastructure/adapters/H2WeatherRepositoryTest.java
- [x] T036 [P] [US1] Contract test for GET /weather/{cityCode} endpoint in weather-service/src/test/java/com/example/weather/infrastructure/web/WeatherControllerContractTest.java
- [x] T037 [P] [US1] Unit test for useWeather composable in frontend/src/composables/__tests__/useWeather.spec.ts

### Implementation for User Story 1

#### Domain Layer (Inner)

- [x] T038 [P] [US1] Implement CityCode value object with TPE/TXG/KHH validation in weather-service/src/main/java/com/example/weather/domain/value_objects/CityCode.java
- [x] T039 [P] [US1] Implement Temperature value object with clamping (15-35°C) in weather-service/src/main/java/com/example/weather/domain/value_objects/Temperature.java
- [x] T040 [P] [US1] Implement Rainfall value object with clamping (0-50mm) in weather-service/src/main/java/com/example/weather/domain/value_objects/Rainfall.java
- [x] T041 [US1] Implement City entity with value objects in weather-service/src/main/java/com/example/weather/domain/entities/City.java
- [x] T042 [US1] Create WeatherDataRepository port interface in weather-service/src/main/java/com/example/weather/domain/ports/WeatherDataRepository.java
- [x] T043 [US1] Implement WeatherCalculationService with random variation (±2°C, ±5mm) in weather-service/src/main/java/com/example/weather/domain/services/WeatherCalculationService.java

#### Application Layer (Middle)

- [x] T044 [P] [US1] Create WeatherRequest DTO in weather-service/src/main/java/com/example/weather/application/dto/WeatherRequest.java
- [x] T045 [P] [US1] Create WeatherResponse DTO in weather-service/src/main/java/com/example/weather/application/dto/WeatherResponse.java
- [x] T046 [P] [US1] Create ApiResponse generic wrapper in weather-service/src/main/java/com/example/weather/application/dto/ApiResponse.java
- [x] T047 [US1] Create WeatherMapper for entity-DTO transformation in weather-service/src/main/java/com/example/weather/application/mappers/WeatherMapper.java
- [x] T048 [US1] Implement GetWeatherUseCase orchestrating domain services in weather-service/src/main/java/com/example/weather/application/use_cases/GetWeatherUseCase.java

#### Infrastructure Layer (Outer)

- [x] T049 [US1] Create WeatherDataEntity JPA entity in weather-service/src/main/java/com/example/weather/infrastructure/persistence/WeatherDataEntity.java
- [x] T050 [US1] Implement H2WeatherRepository adapter in weather-service/src/main/java/com/example/weather/infrastructure/adapters/H2WeatherRepository.java
- [x] T051 [US1] Implement WeatherController with GET /weather/{cityCode} in weather-service/src/main/java/com/example/weather/infrastructure/web/WeatherController.java

#### Frontend

- [x] T052 [US1] Implement useWeather composable for API calls in frontend/src/composables/useWeather.ts
- [x] T053 [P] [US1] Create CitySelector component with 3 city buttons in frontend/src/components/CitySelector.vue
- [x] T054 [P] [US1] Create WeatherCard component for temperature/rainfall display in frontend/src/components/WeatherCard.vue
- [x] T055 [US1] Integrate weather components into App.vue in frontend/src/App.vue

**Checkpoint**: User Story 1 complete - users can query weather for any city with random variation

---

## Phase 4: User Story 2 - View Request Trace Information (Priority: P2)

**Goal**: Display Trace ID for each query, clickable link to Jaeger UI, show request duration

**Independent Test**: Make weather query → Trace ID appears → Click Trace ID → Opens Jaeger showing 4-hop trace chain

### Tests for User Story 2

- [ ] T056 [P] [US2] Unit test for TraceInfo DTO validation (32-char traceId, 16-char spanId) in weather-service/src/test/java/com/example/weather/application/dto/TraceInfoTest.java
- [ ] T057 [P] [US2] Unit test for TraceHeaderFilter extracting trace context in gateway/src/test/java/com/example/gateway/filter/TraceHeaderFilterTest.java
- [ ] T058 [P] [US2] Integration test for trace propagation across gateway→service in gateway/src/test/java/com/example/gateway/TracePropagationIntegrationTest.java
- [ ] T059 [P] [US2] Unit test for TraceInfo component rendering in frontend/src/components/__tests__/TraceInfo.spec.ts

### Implementation for User Story 2

#### Backend Trace Enhancement

- [ ] T060 [US2] Create TraceInfo DTO with traceId, spanId, duration in weather-service/src/main/java/com/example/weather/application/dto/TraceInfo.java
- [ ] T061 [US2] Update WeatherController to include TraceInfo in response in weather-service/src/main/java/com/example/weather/infrastructure/web/WeatherController.java
- [ ] T062 [US2] Add X-Trace-Id, X-Span-Id, X-Request-Duration response headers in weather-service/src/main/java/com/example/weather/infrastructure/web/WeatherController.java
- [ ] T063 [US2] Implement TraceHeaderFilter for trace context forwarding in gateway/src/main/java/com/example/gateway/filter/TraceHeaderFilter.java

#### Frontend Trace Display

- [ ] T064 [US2] Create TraceInfo component with clickable Trace ID link in frontend/src/components/TraceInfo.vue
- [ ] T065 [US2] Update useWeather to extract trace headers from response in frontend/src/composables/useWeather.ts
- [ ] T066 [US2] Update App.vue to display TraceInfo component after query in frontend/src/App.vue
- [ ] T067 [US2] Add Jaeger UI link configuration in frontend/src/config.ts

**Checkpoint**: User Story 2 complete - Trace ID displayed and links to Jaeger

---

## Phase 5: User Story 3 - Monitor System Metrics (Priority: P3)

**Goal**: Metrics dashboard showing QPS, latency percentiles (P50/P95/P99), error rates, filterable by service

**Independent Test**: Access Grafana dashboard → See request count updating within 15 seconds of queries → Filter by service

### Tests for User Story 3

- [ ] T068 [P] [US3] Integration test for Prometheus metrics endpoint in weather-service/src/test/java/com/example/weather/infrastructure/MetricsIntegrationTest.java
- [ ] T069 [P] [US3] Integration test for custom weather.queries.total metric in weather-service/src/test/java/com/example/weather/infrastructure/CustomMetricsTest.java

### Implementation for User Story 3

#### Metrics Configuration

- [ ] T070 [US3] Add custom metrics (weather.queries.total, weather.query.duration) in weather-service/src/main/java/com/example/weather/infrastructure/config/MetricsConfig.java
- [ ] T071 [US3] Update WeatherController to record custom metrics in weather-service/src/main/java/com/example/weather/infrastructure/web/WeatherController.java
- [ ] T072 [US3] Configure gateway metrics exposure in gateway/src/main/resources/application.yml
- [ ] T073 [US3] Update Prometheus scrape config for all services in observability/prometheus/prometheus.yml

#### Grafana Dashboard

- [ ] T074 [US3] Create Grafana dashboard JSON with QPS panel in observability/grafana/provisioning/dashboards/weather-service.json
- [ ] T075 [US3] Add latency histogram panel (P50, P95, P99) in observability/grafana/provisioning/dashboards/weather-service.json
- [ ] T076 [US3] Add error rate panel in observability/grafana/provisioning/dashboards/weather-service.json
- [ ] T077 [US3] Add service filter variable in observability/grafana/provisioning/dashboards/weather-service.json
- [ ] T078 [US3] Create dashboard provisioning config in observability/grafana/provisioning/dashboards/dashboard.yml

#### Frontend Quick Links

- [ ] T079 [US3] Add quick links section (Jaeger, Grafana, Prometheus) to App.vue in frontend/src/App.vue

**Checkpoint**: User Story 3 complete - metrics visible in Grafana, filterable by service

---

## Phase 6: Deployment & Infrastructure

**Purpose**: Complete deployment configurations for all 4 modes

### Dockerfiles

- [ ] T080 [P] Create weather-service Dockerfile with Java 21 in weather-service/Dockerfile
- [ ] T081 [P] Create gateway Dockerfile with Java 21 in gateway/Dockerfile
- [ ] T082 [P] Create frontend Dockerfile with nginx in frontend/Dockerfile

### Kubernetes Configurations

- [ ] T083 Create namespace and configmap in k8s/base/namespace.yaml and k8s/base/configmap.yaml
- [ ] T084 [P] Create weather-service deployment and service in k8s/base/weather-service/
- [ ] T085 [P] Create gateway deployment and service in k8s/base/gateway/
- [ ] T086 [P] Create frontend deployment and service in k8s/base/frontend/
- [ ] T087 [P] Create observability stack deployments in k8s/base/observability/
- [ ] T088 Create Kind cluster config for Ingress mode in k8s/kind/kind-config-ingress.yaml
- [ ] T089 Create Kind cluster config for LoadBalancer mode in k8s/kind/kind-config-lb.yaml
- [ ] T090 Create Ingress overlay with NGINX Ingress rules in k8s/overlays/ingress/
- [ ] T091 Create LoadBalancer overlay with MetalLB config in k8s/overlays/loadbalancer/

### Deployment Scripts

- [ ] T092 [P] Create local-dev.sh script for Mode 1 in scripts/local-dev.sh
- [ ] T093 [P] Create docker-compose-up.sh script for Mode 2 in scripts/docker-compose-up.sh
- [ ] T094 [P] Create k8s-deploy-ingress.sh script for Mode 3 in scripts/k8s-deploy-ingress.sh
- [ ] T095 [P] Create k8s-deploy-lb.sh script for Mode 4 in scripts/k8s-deploy-lb.sh
- [ ] T096 Create cleanup.sh script in scripts/cleanup.sh

**Checkpoint**: All 4 deployment modes operational

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final improvements affecting multiple user stories

- [ ] T097 [P] Create README.md with project overview and mode instructions
- [ ] T098 [P] Add Chinese (Traditional) labels to all frontend components per FR-001a
- [ ] T099 [P] Add loading indicator to WeatherCard component per FR-003 in frontend/src/components/WeatherCard.vue
- [ ] T100 [P] Add error display with retry option per FR-004 in frontend/src/components/WeatherCard.vue
- [ ] T101 Verify all acceptance scenarios pass per quickstart.md demo script
- [ ] T102 Run k6 performance test to verify P95 < 200ms
- [ ] T103 Verify trace propagation across all 4 hops in Jaeger

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-5)**: All depend on Foundational phase completion
  - US1 (P1): Can proceed immediately after Foundational
  - US2 (P2): Can proceed after Foundational (independent of US1 implementation)
  - US3 (P3): Can proceed after Foundational (independent of US1/US2)
- **Deployment (Phase 6)**: Depends on at least US1 completion for meaningful deployment
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

```
Foundational (Phase 2)
         │
         ├──────────────────┬──────────────────┐
         │                  │                  │
         ▼                  ▼                  ▼
   US1 (Phase 3)      US2 (Phase 4)     US3 (Phase 5)
   Weather Query      Trace Display     Metrics Dashboard
         │                  │                  │
         └──────────────────┴──────────────────┘
                           │
                           ▼
                  Deployment (Phase 6)
                           │
                           ▼
                    Polish (Phase 7)
```

### Within Each User Story

1. Tests written FIRST, must FAIL (TDD Red phase)
2. Domain layer (value objects → entities → services)
3. Application layer (DTOs → mappers → use cases)
4. Infrastructure layer (adapters → controllers)
5. Frontend (services → components → integration)
6. Tests must PASS (TDD Green phase)
7. Refactor if needed

### Parallel Opportunities

#### Phase 1 Setup (8 parallel groups)
```
T001 (sequential - root structure)
Then parallel:
  - T002 (weather-service gradle)
  - T003 (gateway gradle)
  - T004 (frontend npm)
  - T005 (Java config)
  - T006 (TS config)
Then parallel:
  - T007 (OTel collector)
  - T008 (Prometheus)
  - T009 (Jaeger)
  - T010 (Grafana)
Then:
  - T011, T012 (docker-compose files)
```

#### Phase 3 User Story 1 Tests (10 parallel)
```
T028, T029, T030, T031, T032, T033, T034, T035, T036, T037
```

#### Phase 3 User Story 1 Implementation
```
Parallel: T038, T039, T040 (value objects)
Then: T041 (City entity)
Parallel: T042 (port), T043 (calculation service)
Parallel: T044, T045, T046 (DTOs)
Then: T047, T048 (mapper, use case)
Then: T049, T050, T051 (infrastructure)
Parallel: T052, T053, T054 (frontend)
Then: T055 (integration)
```

---

## Parallel Example: User Story 1 Tests

```bash
# Launch all domain tests in parallel:
Task: "Unit test for CityCode value object"
Task: "Unit test for Temperature value object"
Task: "Unit test for Rainfall value object"
Task: "Unit test for City entity"
Task: "Unit test for WeatherCalculationService"

# Launch all application tests in parallel:
Task: "Unit test for GetWeatherUseCase"
Task: "Unit test for WeatherMapper"

# Launch all infrastructure tests in parallel:
Task: "Integration test for H2WeatherRepository"
Task: "Contract test for GET /weather/{cityCode}"

# Launch frontend test:
Task: "Unit test for useWeather composable"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Query weather for all 3 cities, verify random variation
5. Deploy with docker-compose (Mode 2) for demo

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → **MVP Ready!**
3. Add User Story 2 → Test trace display → Enhanced observability
4. Add User Story 3 → Test metrics dashboard → Full observability
5. Add Deployment (Phase 6) → All 4 modes operational
6. Polish (Phase 7) → Production-ready demo

### Parallel Team Strategy

With 3 developers after Foundational phase:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (Weather Query)
   - Developer B: User Story 2 (Trace Display)
   - Developer C: User Story 3 (Metrics Dashboard)
3. All reconvene for Phase 6 (Deployment) and Phase 7 (Polish)

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story is independently completable and testable
- TDD: Write tests first, verify they FAIL, then implement
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Weather data uses random variation (±2°C, ±5mm) per query per FR-010
- All UI text in Chinese (Traditional) per FR-001a
