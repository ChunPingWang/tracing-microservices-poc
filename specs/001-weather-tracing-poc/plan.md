# Implementation Plan: Weather Tracing PoC

**Branch**: `001-weather-tracing-poc` | **Date**: 2026-01-21 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-weather-tracing-poc/spec.md`

## Summary

Build a weather query demonstration system with full distributed tracing and metrics observability. The system displays weather data for three Taiwan cities (Taipei, Taichung, Kaohsiung) with simulated data stored in H2 database. Each query generates unique trace chains visible in Jaeger UI, with metrics displayed in Grafana dashboards. Supports four deployment modes: local development, Docker Compose, Kubernetes with Ingress, and Kubernetes with LoadBalancer.

## Technical Context

**Language/Version**:
- Backend: Java 21 (LTS)
- Frontend: TypeScript with Node.js 20.x (LTS)

**Primary Dependencies**:
- Frontend: Vue.js 3.x, Vite 5.x, Axios 1.x
- Gateway: Spring Cloud Gateway 4.1.x, Spring Boot 3.2.x
- Backend: Spring Boot 3.2.x, Spring Data JPA 3.2.x

**Storage**: H2 Database 2.x (Embedded, in-memory)

**Testing**:
- Backend: JUnit 5, Mockito, Spring Test, TestContainers
- Frontend: Vitest (unit), Playwright (E2E)
- Performance: k6

**Target Platform**:
- Linux server (Docker/Kubernetes)
- macOS/Windows (local development)

**Project Type**: Web application (frontend + backend microservices)

**Performance Goals**:
- Response time: < 200ms (P95)
- Concurrent users: 10 (demonstration scale)

**Constraints**:
- < 200ms P95 latency
- 100% trace propagation across service boundaries
- Metrics visible within 15 seconds

**Scale/Scope**:
- 10 concurrent users
- 3 cities
- 4 deployment modes

**Observability Stack**:
- OpenTelemetry 1.35+ (telemetry collection)
- Jaeger 1.54+ (distributed tracing)
- Prometheus 2.50+ (metrics storage)
- Grafana 10.x (visualization)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Compliance | Notes |
|-----------|------------|-------|
| I. TDD (NON-NEGOTIABLE) | [x] Pass | Tests will be written before implementation per Red-Green-Refactor cycle |
| II. BDD | [x] Pass | Spec contains Given-When-Then acceptance scenarios for all user stories |
| III. DDD | [x] Pass | Domain entities (City, WeatherData) isolated from infrastructure |
| IV. SOLID | [x] Pass | Dependency inversion via ports/adapters pattern |
| V. Hexagonal Architecture | [x] Pass | Three-layer architecture with ports in domain/application layers |
| VI. Code Quality | [x] Pass | Target 80% coverage, small functions, linting enforced |
| Layer Separation | [x] Pass | Domain/Application access Infrastructure via interfaces only |
| Data Mapping | [x] Pass | DTOs and mappers for cross-layer data transfer |

## Project Structure

### Documentation (this feature)

```text
specs/001-weather-tracing-poc/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (OpenAPI specs)
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
frontend/                        # Vue.js Frontend (Infrastructure Layer - UI)
├── src/
│   ├── components/              # UI Components
│   │   ├── CitySelector.vue
│   │   ├── WeatherCard.vue
│   │   └── TraceInfo.vue
│   ├── composables/             # Composition API hooks
│   │   └── useWeather.ts
│   ├── services/                # API clients (Infrastructure - External)
│   │   └── weatherApi.ts
│   ├── types/                   # TypeScript types
│   │   └── weather.ts
│   ├── App.vue
│   └── main.ts
├── public/
├── index.html
├── package.json
├── vite.config.ts
├── tsconfig.json
└── Dockerfile

gateway/                         # Spring Cloud Gateway (Infrastructure Layer)
├── src/main/java/com/example/gateway/
│   ├── GatewayApplication.java
│   ├── config/
│   │   ├── RouteConfig.java
│   │   ├── CorsConfig.java
│   │   └── ObservabilityConfig.java
│   └── filter/
│       └── TraceHeaderFilter.java
├── src/main/resources/
│   └── application.yml
├── src/test/java/               # Tests
├── build.gradle
└── Dockerfile

weather-service/                 # Weather Service (Hexagonal Architecture)
├── src/main/java/com/example/weather/
│   ├── WeatherServiceApplication.java
│   │
│   ├── domain/                  # Domain Layer (Inner)
│   │   ├── entities/
│   │   │   └── City.java
│   │   ├── value_objects/
│   │   │   ├── Temperature.java
│   │   │   ├── Rainfall.java
│   │   │   └── CityCode.java
│   │   ├── services/
│   │   │   └── WeatherCalculationService.java
│   │   └── ports/
│   │       └── WeatherDataRepository.java   # Port interface
│   │
│   ├── application/             # Application Layer (Middle)
│   │   ├── use_cases/
│   │   │   └── GetWeatherUseCase.java
│   │   ├── dto/
│   │   │   ├── WeatherRequest.java
│   │   │   ├── WeatherResponse.java
│   │   │   └── TraceInfo.java
│   │   └── mappers/
│   │       └── WeatherMapper.java
│   │
│   └── infrastructure/          # Infrastructure Layer (Outer)
│       ├── adapters/
│       │   └── H2WeatherRepository.java   # Adapter implementing port
│       ├── persistence/
│       │   └── WeatherDataEntity.java     # JPA Entity
│       ├── web/
│       │   ├── WeatherController.java
│       │   └── GlobalExceptionHandler.java
│       └── config/
│           └── ObservabilityConfig.java
│
├── src/main/resources/
│   ├── application.yml
│   ├── schema.sql
│   └── data.sql
├── src/test/java/               # Tests (mirror structure)
│   ├── domain/
│   ├── application/
│   └── infrastructure/
├── build.gradle
└── Dockerfile

observability/                   # Observability Stack Configuration
├── otel-collector/
│   ├── otel-collector-config.yaml
│   └── otel-collector-config-dev.yaml
├── prometheus/
│   ├── prometheus.yml
│   └── prometheus-dev.yml
├── grafana/
│   ├── provisioning/
│   │   ├── dashboards/
│   │   │   ├── dashboard.yml
│   │   │   └── weather-service.json
│   │   └── datasources/
│   │       └── datasource.yml
│   └── grafana.ini
└── jaeger/
    └── jaeger-config.yml

k8s/                             # Kubernetes Configuration
├── base/
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── frontend/
│   ├── gateway/
│   ├── weather-service/
│   └── observability/
├── overlays/
│   ├── ingress/
│   │   ├── kustomization.yaml
│   │   └── ingress.yaml
│   └── loadbalancer/
│       ├── kustomization.yaml
│       └── services.yaml
└── kind/
    ├── kind-config-ingress.yaml
    └── kind-config-lb.yaml

scripts/                         # Deployment Scripts
├── local-dev.sh
├── docker-compose-up.sh
├── k8s-deploy-ingress.sh
├── k8s-deploy-lb.sh
└── cleanup.sh

docker-compose.yml               # Full stack
docker-compose.dev.yml           # Dev mode (observability only)
settings.gradle
README.md
```

**Structure Decision**: Web application with microservices architecture following hexagonal architecture in weather-service. The frontend (Vue.js) and gateway (Spring Cloud Gateway) are in the infrastructure layer. The weather-service follows strict hexagonal architecture with domain/application/infrastructure separation. Observability tools and Kubernetes configs are deployment infrastructure.

## Complexity Tracking

> No violations requiring justification. All principles are followed.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| N/A | - | - |
