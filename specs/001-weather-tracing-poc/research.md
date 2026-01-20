# Research: Weather Tracing PoC

**Branch**: `001-weather-tracing-poc` | **Date**: 2026-01-21
**Status**: Complete (all decisions resolved from TECH.md and INFRA.md)

## Overview

This research document consolidates technical decisions for the Weather Tracing PoC. All decisions are derived from the provided technical specifications (TECH.md) and infrastructure specifications (INFRA.md).

---

## 1. Frontend Technology

### Decision
Vue.js 3.x with Vite 5.x and TypeScript

### Rationale
- Composition API provides clean reactive state management for weather queries
- Vite offers fast HMR (Hot Module Replacement) for rapid development
- TypeScript ensures type safety for API contracts
- Axios 1.x is well-established HTTP client with interceptor support for trace headers

### Alternatives Considered
| Alternative | Rejected Because |
|-------------|------------------|
| React | Vue.js selected for lighter bundle and simpler Composition API |
| Angular | Overkill for a PoC demonstration system |
| Plain JavaScript | TypeScript type safety needed for API contracts |

---

## 2. API Gateway

### Decision
Spring Cloud Gateway 4.1.x on Spring Boot 3.2.x

### Rationale
- Native reactive (WebFlux) architecture for high-throughput routing
- Built-in OpenTelemetry instrumentation support
- Spring ecosystem consistency with backend services
- Mature routing and filtering capabilities

### Alternatives Considered
| Alternative | Rejected Because |
|-------------|------------------|
| Kong | Additional operational complexity for a PoC |
| Envoy | Kubernetes-centric; Spring Gateway simpler for local dev |
| Nginx | Less trace propagation support out of the box |

---

## 3. Backend Framework

### Decision
Spring Boot 3.2.x with Spring Data JPA

### Rationale
- Java 21 LTS provides modern language features (virtual threads, pattern matching)
- Spring Boot auto-configuration reduces boilerplate
- Spring Data JPA simplifies database operations with automatic tracing
- OpenTelemetry Spring Boot Starter provides automatic instrumentation

### Alternatives Considered
| Alternative | Rejected Because |
|-------------|------------------|
| Quarkus | Less mature OpenTelemetry integration |
| Micronaut | Smaller community and tooling ecosystem |
| Node.js/Express | Java selected for demonstration of JVM observability patterns |

---

## 4. Database

### Decision
H2 Database 2.x (Embedded, in-memory)

### Rationale
- Zero configuration required for PoC
- Embedded mode simplifies deployment across all four modes
- Generates authentic JDBC spans for tracing demonstration
- Sufficient for 10 concurrent users and 3 cities

### Alternatives Considered
| Alternative | Rejected Because |
|-------------|------------------|
| PostgreSQL | Additional container/setup complexity for PoC |
| SQLite | Less JDBC tracing support |
| Redis | Not relational; wouldn't demonstrate JPA/JDBC tracing |

---

## 5. Observability Stack

### Decision
OpenTelemetry Collector → Jaeger (tracing) + Prometheus (metrics) + Grafana (visualization)

### Rationale
- OpenTelemetry is the CNCF standard for observability
- OTel Collector provides unified telemetry pipeline (traces + metrics)
- Jaeger is purpose-built for distributed tracing visualization
- Prometheus is the de facto standard for Kubernetes metrics
- Grafana provides flexible dashboarding with both data sources

### Alternatives Considered
| Alternative | Rejected Because |
|-------------|------------------|
| Zipkin | Jaeger has better UI for trace analysis |
| Datadog | Commercial; open-source stack preferred for PoC |
| New Relic | Same as Datadog |
| OpenTelemetry direct to Jaeger | Collector provides batching and processing pipeline |

---

## 6. Trace Propagation Protocol

### Decision
W3C Trace Context (with B3 fallback)

### Rationale
- W3C Trace Context is the industry standard
- Spring Cloud Gateway and OpenTelemetry natively support W3C
- B3 fallback ensures compatibility with older systems
- Headers: `traceparent`, `tracestate`

### Alternatives Considered
| Alternative | Rejected Because |
|-------------|------------------|
| B3 only | W3C is the newer standard |
| Custom headers | Non-standard; would break interoperability |

---

## 7. Deployment Modes

### Decision
Four deployment modes as specified in INFRA.md

| Mode | Description | Use Case |
|------|-------------|----------|
| Mode 1 | Local dev + Docker observability | Day-to-day development |
| Mode 2 | Docker Compose full stack | Quick demo / integration testing |
| Mode 3 | Kind + NGINX Ingress | Single-endpoint K8s demonstration |
| Mode 4 | Kind + MetalLB LoadBalancer | Multi-IP K8s demonstration |

### Rationale
- Mode 1 allows fast iteration with native IDE debugging
- Mode 2 provides one-command reproducible environment
- Mode 3 demonstrates K8s ingress routing patterns
- Mode 4 demonstrates production-like LoadBalancer services

### Alternatives Considered
| Alternative | Rejected Because |
|-------------|------------------|
| Minikube | Kind is lighter and has better LoadBalancer support |
| k3d | Kind is more widely documented |
| EKS/GKE | Cloud deployment out of scope for local PoC |

---

## 8. Hexagonal Architecture Implementation

### Decision
Weather Service follows strict hexagonal architecture:
- **Domain Layer**: City entity, value objects (Temperature, Rainfall, CityCode), domain services
- **Application Layer**: GetWeatherUseCase, DTOs, mappers
- **Infrastructure Layer**: H2 adapter, web controller, OTel config

### Rationale
- Aligns with Constitution principles (V. Hexagonal Architecture)
- Domain logic isolated from Spring/JPA/HTTP concerns
- Enables testability at each layer
- Mappers ensure clean data transformation between layers

### Alternatives Considered
| Alternative | Rejected Because |
|-------------|------------------|
| Traditional 3-tier (MVC) | Doesn't satisfy Constitution requirements |
| Onion Architecture | Hexagonal provides clearer port/adapter semantics |
| Direct JPA entities in API | Violates layer separation principle |

---

## 9. Testing Strategy

### Decision

| Layer | Test Type | Framework | Coverage Target |
|-------|-----------|-----------|-----------------|
| Domain | Unit | JUnit 5 + Mockito | 100% |
| Application | Unit | JUnit 5 + Mockito | 100% |
| Infrastructure | Integration | Spring Test + TestContainers | 80% |
| API | Contract | Spring MockMvc | 100% endpoints |
| E2E | Acceptance | Playwright | Critical paths |
| Performance | Load | k6 | P95 < 200ms |

### Rationale
- Testing pyramid: more unit tests, fewer E2E tests
- TestContainers enables real database testing without H2 quirks
- Playwright for cross-browser E2E testing
- k6 for performance validation

---

## 10. Metrics Design

### Decision
Prometheus metrics exposed via Spring Actuator + custom metrics

| Metric | Type | Description |
|--------|------|-------------|
| `http_server_requests_seconds` | Histogram | Request latency |
| `weather.queries.total` | Counter | Query count by city |
| `weather.query.duration` | Timer | Business operation timing |
| `hikaricp_connections_active` | Gauge | DB pool status |
| `jvm_memory_used_bytes` | Gauge | JVM memory |

### Rationale
- Spring Actuator provides JVM and HTTP metrics automatically
- Custom metrics track business-specific operations
- Histogram enables percentile calculations (P50, P95, P99)

---

## Summary

All technical decisions are resolved. No NEEDS CLARIFICATION items remain.

| Category | Decision | Source |
|----------|----------|--------|
| Frontend | Vue.js 3.x + Vite + TypeScript | TECH.md 1.2 |
| Gateway | Spring Cloud Gateway 4.1.x | TECH.md 1.2 |
| Backend | Spring Boot 3.2.x | TECH.md 1.2 |
| Database | H2 Embedded | TECH.md 1.2 |
| Tracing | Jaeger via OTel Collector | TECH.md 1.2, 4.x |
| Metrics | Prometheus + Grafana | TECH.md 1.2, 4.x |
| Deployment | 4 modes (local, compose, ingress, LB) | INFRA.md 1-6 |
| Architecture | Hexagonal (weather-service) | Constitution V |
| Testing | JUnit 5, Playwright, k6 | TECH.md 7.x |

**Next Phase**: Proceed to Phase 1 (data-model.md, contracts/, quickstart.md)
