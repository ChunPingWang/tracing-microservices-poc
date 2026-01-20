# Weather Tracing PoC - 天氣查詢可觀測性展示系統

A demonstration system showcasing distributed tracing and observability patterns using a weather query application.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [System Architecture Diagram](#system-architecture-diagram)
- [Sequence Diagram](#sequence-diagram)
- [Class Diagram (Hexagonal Architecture)](#class-diagram-hexagonal-architecture)
- [Observability Concepts](#observability-concepts)
- [Technology Stack](#technology-stack)
- [Quick Start](#quick-start)
- [API Reference](#api-reference)
- [Project Structure](#project-structure)

---

## Overview

This project demonstrates modern observability practices through a simple weather query system. Users can query weather data for three Taiwan cities (Taipei, Taichung, Kaohsiung), and the system showcases:

- **Distributed Tracing**: Track requests across multiple services
- **Metrics Collection**: Monitor system performance and health
- **Log Aggregation**: Centralized logging for debugging
- **Dashboard Visualization**: Real-time monitoring through Grafana

### Key Features

- Query weather for 3 cities (台北, 台中, 高雄)
- View request trace chain through Jaeger UI
- Monitor system metrics through Grafana dashboards
- Support for 4 deployment modes (local, Docker Compose, K8s Ingress, K8s LoadBalancer)

---

## Architecture

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              USER LAYER                                  │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                       Web Browser                                │   │
│  │                   (Vue.js Frontend)                              │   │
│  │                    localhost:5173                                │   │
│  └─────────────────────────────┬───────────────────────────────────┘   │
└────────────────────────────────┼────────────────────────────────────────┘
                                 │ HTTP Request
                                 │ + W3C Trace Context
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            SERVICE LAYER                                 │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                    API Gateway                                    │  │
│  │              (Spring Cloud Gateway)                               │  │
│  │                   localhost:8080                                  │  │
│  │  ┌────────────────────────────────────────────────────────────┐  │  │
│  │  │ • Route Management    • CORS Configuration                 │  │  │
│  │  │ • Trace Propagation   • Request/Response Logging          │  │  │
│  │  └────────────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────┬──────────────────────────────────┘  │
│                                  │                                      │
│                                  ▼                                      │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                   Weather Service                                 │  │
│  │               (Spring Boot + Hexagonal)                           │  │
│  │                   localhost:8081                                  │  │
│  │  ┌────────────────────────────────────────────────────────────┐  │  │
│  │  │              Hexagonal Architecture                         │  │  │
│  │  │  ┌─────────────────────────────────────────────────────┐   │  │  │
│  │  │  │           Infrastructure Layer                       │   │  │  │
│  │  │  │  • WeatherController    • H2WeatherRepository       │   │  │  │
│  │  │  │  • GlobalExceptionHandler                           │   │  │  │
│  │  │  │  ┌─────────────────────────────────────────────┐    │   │  │  │
│  │  │  │  │         Application Layer                    │    │   │  │  │
│  │  │  │  │  • GetWeatherUseCase  • WeatherMapper       │    │   │  │  │
│  │  │  │  │  • DTOs (Request, Response, TraceInfo)      │    │   │  │  │
│  │  │  │  │  ┌─────────────────────────────────────┐    │    │   │  │  │
│  │  │  │  │  │         Domain Layer                 │    │    │   │  │  │
│  │  │  │  │  │  • City Entity                      │    │    │   │  │  │
│  │  │  │  │  │  • CityCode, Temperature, Rainfall  │    │    │   │  │  │
│  │  │  │  │  │  • WeatherCalculationService        │    │    │   │  │  │
│  │  │  │  │  │  • WeatherDataRepository (Port)     │    │    │   │  │  │
│  │  │  │  │  └─────────────────────────────────────┘    │    │   │  │  │
│  │  │  │  └─────────────────────────────────────────────┘    │   │  │  │
│  │  │  └─────────────────────────────────────────────────────┘   │  │  │
│  │  └────────────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────┬──────────────────────────────────┘  │
│                                  │                                      │
│                                  ▼                                      │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                      H2 Database                                  │  │
│  │                    (Embedded, In-Memory)                          │  │
│  │  ┌────────────────────────────────────────────────────────────┐  │  │
│  │  │  weather_data: TPE(台北), TXG(台中), KHH(高雄)             │  │  │
│  │  └────────────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
                                 │
                    Telemetry Data (OTLP)
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        OBSERVABILITY LAYER                               │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │              OpenTelemetry Collector                              │  │
│  │                 localhost:4317 (gRPC)                             │  │
│  │  ┌────────────────────────────────────────────────────────────┐  │  │
│  │  │ Receivers → Processors → Exporters                         │  │  │
│  │  │   OTLP       Batching     Jaeger + Prometheus              │  │  │
│  │  └────────────────────────────────────────────────────────────┘  │  │
│  └────────────────────┬────────────────────────┬────────────────────┘  │
│                       │                        │                        │
│            Traces     │                        │  Metrics               │
│                       ▼                        ▼                        │
│  ┌────────────────────────────┐  ┌────────────────────────────────┐   │
│  │         Jaeger             │  │        Prometheus               │   │
│  │    localhost:16686         │  │      localhost:9090             │   │
│  │  ┌──────────────────────┐  │  │  ┌──────────────────────────┐  │   │
│  │  │ • Trace Storage      │  │  │  │ • Time-Series DB         │  │   │
│  │  │ • Trace UI           │  │  │  │ • PromQL Queries         │  │   │
│  │  │ • Dependency Graph   │  │  │  │ • Alert Rules            │  │   │
│  │  └──────────────────────┘  │  │  └──────────────────────────┘  │   │
│  └────────────────────────────┘  └──────────────────┬─────────────┘   │
│                                                      │                  │
│                                                      ▼                  │
│                                    ┌────────────────────────────────┐  │
│                                    │          Grafana               │  │
│                                    │       localhost:3000           │  │
│                                    │  ┌──────────────────────────┐  │  │
│                                    │  │ • Dashboards             │  │  │
│                                    │  │ • Alerts                 │  │  │
│                                    │  │ • Data Exploration       │  │  │
│                                    │  └──────────────────────────┘  │  │
│                                    └────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Sequence Diagram

### Weather Query Flow with Distributed Tracing

```
┌────────┐      ┌─────────┐      ┌────────────────┐      ┌────────┐      ┌────────┐
│ Browser│      │ Gateway │      │Weather Service │      │   H2   │      │ Jaeger │
└───┬────┘      └────┬────┘      └───────┬────────┘      └───┬────┘      └───┬────┘
    │                │                   │                   │               │
    │  1. GET /api/weather/TPE           │                   │               │
    │  [Create Trace: trace-id-abc]      │                   │               │
    │───────────────>│                   │                   │               │
    │                │                   │                   │               │
    │                │  2. Forward with traceparent header   │               │
    │                │  [Create Span: gateway-span]          │               │
    │                │──────────────────>│                   │               │
    │                │                   │                   │               │
    │                │                   │  3. Query city data               │
    │                │                   │  [Create Span: db-span]           │
    │                │                   │──────────────────>│               │
    │                │                   │                   │               │
    │                │                   │  4. Return TPE baseline           │
    │                │                   │<──────────────────│               │
    │                │                   │                   │               │
    │                │                   │  5. Apply random variation        │
    │                │                   │  (±2°C temp, ±5mm rain)           │
    │                │                   │                   │               │
    │                │  6. Response + X-Trace-Id header      │               │
    │                │<──────────────────│                   │               │
    │                │                   │                   │               │
    │  7. Response with trace headers    │                   │               │
    │<───────────────│                   │                   │               │
    │                │                   │                   │               │
    │                │═══════════════════════════════════════════════════════│
    │                │        8. Export spans via OTLP (async)              │
    │                │═══════════════════════════════════════════════════════>
    │                │                   │                   │               │
    │  9. Click Trace ID                 │                   │               │
    │────────────────────────────────────────────────────────────────────────>
    │                │                   │                   │               │
    │  10. View trace with 4 spans:      │                   │               │
    │      - frontend-request            │                   │               │
    │      - gateway-proxy               │                   │               │
    │      - weather-service-handler     │                   │               │
    │      - h2-jdbc-query               │                   │               │
    │<────────────────────────────────────────────────────────────────────────
    │                │                   │                   │               │
┌───┴────┐      ┌────┴────┐      ┌───────┴────────┐      ┌───┴────┐      ┌───┴────┐
│ Browser│      │ Gateway │      │Weather Service │      │   H2   │      │ Jaeger │
└────────┘      └─────────┘      └────────────────┘      └────────┘      └────────┘
```

---

## Class Diagram (Hexagonal Architecture)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              DOMAIN LAYER (Inner)                                │
│                           Pure Business Logic                                    │
│                                                                                  │
│  ┌─────────────────────┐      ┌──────────────────────────────────────────────┐  │
│  │   <<Value Object>>  │      │              <<Entity>>                       │  │
│  │      CityCode       │      │                City                           │  │
│  ├─────────────────────┤      ├──────────────────────────────────────────────┤  │
│  │ - value: String     │      │ - code: CityCode                             │  │
│  │ + VALID_CODES: Set  │◄─────│ - name: String                               │  │
│  ├─────────────────────┤      │ - baseTemperature: Temperature               │  │
│  │ + of(code): CityCode│      │ - baseRainfall: Rainfall                     │  │
│  │ + isValid(code):bool│      ├──────────────────────────────────────────────┤  │
│  └─────────────────────┘      │ + code(): CityCode                           │  │
│                               │ + name(): String                              │  │
│  ┌─────────────────────┐      └──────────────────────────────────────────────┘  │
│  │   <<Value Object>>  │                                                        │
│  │    Temperature      │      ┌──────────────────────────────────────────────┐  │
│  ├─────────────────────┤      │           <<Domain Service>>                  │  │
│  │ - value: BigDecimal │      │      WeatherCalculationService               │  │
│  │ + MIN: 15.0         │      ├──────────────────────────────────────────────┤  │
│  │ + MAX: 35.0         │      │ - TEMP_VARIATION: ±2°C                       │  │
│  ├─────────────────────┤      │ - RAIN_VARIATION: ±5mm                       │  │
│  │ + of(val): Temp     │      ├──────────────────────────────────────────────┤  │
│  │ + add(var): Temp    │      │ + calculateCurrentWeather(City): WeatherData │  │
│  └─────────────────────┘      └──────────────────────────────────────────────┘  │
│                                                                                  │
│  ┌─────────────────────┐      ┌──────────────────────────────────────────────┐  │
│  │   <<Value Object>>  │      │              <<Port>>                         │  │
│  │      Rainfall       │      │       WeatherDataRepository                  │  │
│  ├─────────────────────┤      ├──────────────────────────────────────────────┤  │
│  │ - value: BigDecimal │      │ + findByCityCode(CityCode): Optional<City>   │  │
│  │ + MIN: 0.0          │      │ + findAll(): List<City>                      │  │
│  │ + MAX: 50.0         │      └──────────────────────────────────────────────┘  │
│  ├─────────────────────┤                           ▲                            │
│  │ + of(val): Rainfall │                           │ implements                 │
│  │ + add(var): Rainfall│                           │                            │
│  └─────────────────────┘                           │                            │
└────────────────────────────────────────────────────┼────────────────────────────┘
                                                     │
┌────────────────────────────────────────────────────┼────────────────────────────┐
│                         APPLICATION LAYER (Middle)  │                            │
│                        Use Cases & Orchestration    │                            │
│                                                     │                            │
│  ┌──────────────────────────────────────────────┐  │                            │
│  │              <<Use Case>>                     │  │                            │
│  │           GetWeatherUseCase                   │  │                            │
│  ├──────────────────────────────────────────────┤  │                            │
│  │ - repository: WeatherDataRepository          │──┘                            │
│  │ - calculationService: WeatherCalculationSvc  │                               │
│  │ - mapper: WeatherMapper                      │                               │
│  ├──────────────────────────────────────────────┤                               │
│  │ + execute(cityCode: String): WeatherResponse │                               │
│  └──────────────────────────────────────────────┘                               │
│                     │                                                            │
│                     │ uses                                                       │
│                     ▼                                                            │
│  ┌──────────────────────────────┐  ┌──────────────────────────────────────────┐ │
│  │       <<Mapper>>             │  │               <<DTO>>                     │ │
│  │     WeatherMapper            │  │          WeatherResponse                  │ │
│  ├──────────────────────────────┤  ├──────────────────────────────────────────┤ │
│  │ + toResponse(WeatherData):   │  │ - cityCode: String                       │ │
│  │   WeatherResponse            │  │ - cityName: String                       │ │
│  └──────────────────────────────┘  │ - temperature: BigDecimal                │ │
│                                    │ - rainfall: BigDecimal                   │ │
│  ┌──────────────────────────────┐  │ - updatedAt: LocalDateTime              │ │
│  │         <<DTO>>              │  └──────────────────────────────────────────┘ │
│  │       TraceInfo              │                                               │
│  ├──────────────────────────────┤  ┌──────────────────────────────────────────┐ │
│  │ - traceId: String (32 hex)   │  │               <<DTO>>                     │ │
│  │ - spanId: String (16 hex)    │  │           ApiResponse<T>                 │ │
│  │ - duration: long             │  ├──────────────────────────────────────────┤ │
│  ├──────────────────────────────┤  │ - success: boolean                       │ │
│  │ + fromCurrentSpan(): Trace   │  │ - data: T                                │ │
│  │ + withDuration(ms): Trace    │  │ - error: ErrorInfo                       │ │
│  └──────────────────────────────┘  │ - traceInfo: TraceInfo                   │ │
│                                    └──────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                        INFRASTRUCTURE LAYER (Outer)                              │
│                        Frameworks & External Systems                             │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                        <<REST Controller>>                                │   │
│  │                       WeatherController                                   │   │
│  ├──────────────────────────────────────────────────────────────────────────┤   │
│  │ - getWeatherUseCase: GetWeatherUseCase                                   │   │
│  ├──────────────────────────────────────────────────────────────────────────┤   │
│  │ + getWeather(@PathVariable cityCode): ResponseEntity<ApiResponse>        │   │
│  │   - Adds X-Trace-Id, X-Span-Id, X-Request-Duration headers              │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  ┌────────────────────────────────┐  ┌──────────────────────────────────────┐   │
│  │      <<Adapter>>               │  │          <<JPA Entity>>               │   │
│  │   H2WeatherRepository          │  │       WeatherDataEntity               │   │
│  ├────────────────────────────────┤  ├──────────────────────────────────────┤   │
│  │ implements WeatherDataRepository  │ - id: Long                            │   │
│  ├────────────────────────────────┤  │ - cityCode: String                    │   │
│  │ - jpaRepository                │  │ - cityName: String                    │   │
│  ├────────────────────────────────┤  │ - baseTemperature: BigDecimal         │   │
│  │ + findByCityCode(code): City   │  │ - baseRainfall: BigDecimal            │   │
│  │ + findAll(): List<City>        │  └──────────────────────────────────────┘   │
│  │ - toDomainEntity(entity): City │                                             │
│  └────────────────────────────────┘                                             │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                     <<Exception Handler>>                                 │   │
│  │                   GlobalExceptionHandler                                  │   │
│  ├──────────────────────────────────────────────────────────────────────────┤   │
│  │ + handleCityNotFound(ex): ResponseEntity<ApiResponse>     → 404          │   │
│  │ + handleInvalidCityCode(ex): ResponseEntity<ApiResponse>  → 400          │   │
│  │ + handleGenericException(ex): ResponseEntity<ApiResponse> → 500          │   │
│  │   - All responses include TraceInfo for debugging                        │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## Observability Concepts

### The Three Pillars of Observability

```
                    ┌─────────────────────────────────────────┐
                    │         OBSERVABILITY                    │
                    │   Understanding System Behavior          │
                    └───────────────┬─────────────────────────┘
                                    │
          ┌─────────────────────────┼─────────────────────────┐
          │                         │                         │
          ▼                         ▼                         ▼
   ┌─────────────┐          ┌─────────────┐          ┌─────────────┐
   │   TRACES    │          │   METRICS   │          │    LOGS     │
   │             │          │             │          │             │
   │ What path   │          │ What's the  │          │ What        │
   │ did this    │          │ system      │          │ happened    │
   │ request     │          │ doing now?  │          │ in detail?  │
   │ take?       │          │             │          │             │
   └──────┬──────┘          └──────┬──────┘          └──────┬──────┘
          │                        │                        │
          ▼                        ▼                        ▼
   ┌─────────────┐          ┌─────────────┐          ┌─────────────┐
   │   Jaeger    │          │ Prometheus  │          │   Grafana   │
   │             │          │   Grafana   │          │    Loki     │
   │ • Trace UI  │          │             │          │             │
   │ • Span view │          │ • QPS       │          │ • Structured│
   │ • Latency   │          │ • Latency   │          │ • Searchable│
   │ • Errors    │          │ • Errors    │          │ • Correlated│
   └─────────────┘          └─────────────┘          └─────────────┘
```

### 1. Distributed Tracing (分散式追蹤)

**What is it?**
Distributed tracing tracks the journey of a request as it flows through multiple services. Each service creates a "span" containing timing and context information.

**Key Concepts:**

| Concept | Description | Example |
|---------|-------------|---------|
| **Trace** | The complete journey of a request | A single weather query |
| **Span** | A single operation within a trace | Database query, HTTP call |
| **Trace ID** | Unique identifier for the entire trace | `4bf92f3577b34da6a3ce929d0e0e4736` |
| **Span ID** | Unique identifier for a single span | `00f067aa0ba902b7` |
| **Parent Span** | The span that initiated this span | Gateway span is parent of Service span |

**W3C Trace Context Headers:**
```http
traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
             │  │                                │                 │
             │  │                                │                 └── Flags (sampled)
             │  │                                └── Parent Span ID
             │  └── Trace ID
             └── Version
```

**In This Project:**
- Frontend → Gateway → Weather Service → H2 Database
- Each hop creates a new span linked to the same trace
- Trace ID visible in UI and response headers

### 2. Metrics (指標)

**What is it?**
Metrics are numerical measurements collected at regular intervals. They help understand system health and performance trends.

**Types of Metrics:**

| Type | Description | Example |
|------|-------------|---------|
| **Counter** | Monotonically increasing value | Total requests: 1000 |
| **Gauge** | Value that can go up or down | Active connections: 5 |
| **Histogram** | Distribution of values | Latency percentiles |
| **Summary** | Similar to histogram with quantiles | Request duration P99 |

**Key Metrics in This Project:**

```
# HTTP Request Metrics (auto-instrumented)
http_server_requests_seconds_count{uri="/weather/{cityCode}"} 150
http_server_requests_seconds_sum{uri="/weather/{cityCode}"} 4.5

# Custom Business Metrics
weather_queries_total{city="TPE"} 50
weather_queries_total{city="TXG"} 45
weather_queries_total{city="KHH"} 55

# JVM Metrics
jvm_memory_used_bytes{area="heap"} 125829120
jvm_threads_live 25

# Database Pool Metrics
hikaricp_connections_active 2
hikaricp_connections_idle 8
```

### 3. Logs (日誌)

**What is it?**
Logs are discrete events that record what happened in the system. When correlated with traces, they provide detailed context for debugging.

**Structured Logging Example:**
```json
{
  "timestamp": "2026-01-21T10:30:00.123Z",
  "level": "INFO",
  "service": "weather-service",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "spanId": "00f067aa0ba902b7",
  "message": "Weather query completed",
  "cityCode": "TPE",
  "duration_ms": 45
}
```

### OpenTelemetry Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         APPLICATION                                      │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                   OpenTelemetry SDK                              │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │   │
│  │  │   Tracer    │  │   Meter     │  │   Logger    │              │   │
│  │  │   Provider  │  │   Provider  │  │   Provider  │              │   │
│  │  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘              │   │
│  │         │                │                │                      │   │
│  │         └────────────────┴────────────────┘                      │   │
│  │                          │                                       │   │
│  │                   ┌──────▼──────┐                               │   │
│  │                   │   Exporter  │  OTLP Protocol                │   │
│  │                   └──────┬──────┘                               │   │
│  └──────────────────────────┼───────────────────────────────────────┘   │
└─────────────────────────────┼───────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    OpenTelemetry Collector                               │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────────────┐ │
│  │  Receivers  │───▶│ Processors  │───▶│        Exporters            │ │
│  │  (OTLP)     │    │  (Batch,    │    │  ┌─────────┐ ┌───────────┐ │ │
│  │             │    │   Filter)   │    │  │ Jaeger  │ │Prometheus │ │ │
│  └─────────────┘    └─────────────┘    │  └─────────┘ └───────────┘ │ │
│                                         └─────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

### Observability Best Practices

1. **Correlate All Signals**: Use trace IDs to link traces, metrics, and logs
2. **Define SLIs/SLOs**: Set measurable service level indicators
3. **Alert on Symptoms**: Alert on user-facing problems, not causes
4. **Use Dashboards**: Visualize system health at a glance
5. **Instrument Code**: Add custom spans for business-critical operations

---

## Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| **Frontend** | Vue.js | 3.4.x |
| | Vite | 5.x |
| | TypeScript | 5.3.x |
| | Axios | 1.6.x |
| **API Gateway** | Spring Cloud Gateway | 4.1.x |
| | Spring Boot | 3.2.x |
| **Backend** | Spring Boot | 3.2.x |
| | Spring Data JPA | 3.2.x |
| | Java | 21 (LTS) |
| **Database** | H2 | 2.x |
| **Tracing** | OpenTelemetry | 1.35+ |
| | Jaeger | 1.54+ |
| **Metrics** | Prometheus | 2.50+ |
| | Micrometer | 1.12+ |
| **Visualization** | Grafana | 10.x |

---

## Quick Start

### Mode 1: Local Development

```bash
# Start observability stack
docker compose -f docker-compose.dev.yml up -d

# Start backend services (in separate terminals)
cd weather-service && ./gradlew bootRun
cd gateway && ./gradlew bootRun

# Start frontend
cd frontend && npm install && npm run dev
```

### Mode 2: Docker Compose (Full Stack)

```bash
docker compose up -d --build
```

### Access Points

| Service | URL |
|---------|-----|
| Frontend | http://localhost:5173 |
| Gateway API | http://localhost:8080/api |
| Jaeger UI | http://localhost:16686 |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |

---

## API Reference

### GET /weather/{cityCode}

Query weather for a city.

**Request:**
```http
GET /api/weather/TPE HTTP/1.1
Host: localhost:8080
```

**Response:**
```json
{
  "success": true,
  "data": {
    "cityCode": "TPE",
    "cityName": "台北",
    "temperature": 26.5,
    "rainfall": 12.3,
    "updatedAt": "2026-01-21T10:30:00Z"
  },
  "traceInfo": {
    "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
    "spanId": "00f067aa0ba902b7",
    "duration": 45
  }
}
```

**Response Headers:**
```http
X-Trace-Id: 4bf92f3577b34da6a3ce929d0e0e4736
X-Span-Id: 00f067aa0ba902b7
X-Request-Duration: 45
```

---

## Project Structure

```
weather-4-observability-poc/
├── frontend/                  # Vue.js Frontend
│   ├── src/
│   │   ├── components/       # Vue components
│   │   ├── composables/      # Composition API hooks
│   │   ├── services/         # API clients
│   │   └── types/            # TypeScript types
│   └── package.json
│
├── gateway/                   # Spring Cloud Gateway
│   └── src/main/java/com/example/gateway/
│       ├── config/           # Route, CORS, Observability config
│       └── filter/           # Custom filters
│
├── weather-service/           # Spring Boot Backend (Hexagonal)
│   └── src/main/java/com/example/weather/
│       ├── domain/           # Entities, Value Objects, Ports
│       ├── application/      # Use Cases, DTOs, Mappers
│       └── infrastructure/   # Controllers, Adapters, Config
│
├── observability/            # Observability Stack Config
│   ├── otel-collector/       # OpenTelemetry Collector
│   ├── prometheus/           # Prometheus config
│   ├── jaeger/               # Jaeger config
│   └── grafana/              # Grafana dashboards
│
├── k8s/                      # Kubernetes manifests
├── scripts/                  # Deployment scripts
├── docker-compose.yml        # Full stack deployment
└── docker-compose.dev.yml    # Development (observability only)
```

---

## License

MIT License - See LICENSE file for details.
