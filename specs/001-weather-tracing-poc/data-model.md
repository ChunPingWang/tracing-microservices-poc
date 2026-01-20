# Data Model: Weather Tracing PoC

**Branch**: `001-weather-tracing-poc` | **Date**: 2026-01-21
**Status**: Complete

## Overview

This document defines the data model following Domain-Driven Design principles with hexagonal architecture layer separation.

---

## Domain Layer Entities

### City (Entity)

The core domain entity representing a queryable city.

```
City
├── identity: CityCode (Value Object)
├── name: String (Chinese name, e.g., "台北")
├── baseTemperature: Temperature (Value Object)
└── baseRainfall: Rainfall (Value Object)
```

**Invariants**:
- CityCode must be one of: TPE, TXG, KHH
- Name must be non-empty
- baseTemperature must be within 15-35°C
- baseRainfall must be within 0-50mm

**Supported Cities**:

| Code | Chinese Name | English Name | Base Temp | Base Rainfall |
|------|--------------|--------------|-----------|---------------|
| TPE | 台北 | Taipei | 25°C | 15mm |
| TXG | 台中 | Taichung | 27°C | 10mm |
| KHH | 高雄 | Kaohsiung | 29°C | 8mm |

---

## Domain Layer Value Objects

### CityCode

Immutable identifier for a city.

```
CityCode
├── value: String (TPE | TXG | KHH)
└── validation: Must be in allowed set
```

**Factory Method**: `CityCode.of(String code)` throws `InvalidCityCodeException`

### Temperature

Immutable value representing temperature in Celsius.

```
Temperature
├── value: BigDecimal
├── unit: "CELSIUS" (constant)
├── validation: 15.0 <= value <= 35.0
└── operations:
    ├── add(variation: BigDecimal): Temperature
    └── clamp(min, max): Temperature
```

### Rainfall

Immutable value representing rainfall in millimeters.

```
Rainfall
├── value: BigDecimal
├── unit: "MM" (constant)
├── validation: 0.0 <= value <= 50.0
└── operations:
    ├── add(variation: BigDecimal): Rainfall
    └── clamp(min, max): Rainfall
```

---

## Domain Layer Services

### WeatherCalculationService

Stateless domain service for calculating weather with random variation.

```
WeatherCalculationService
└── calculateCurrentWeather(city: City): WeatherData
    ├── Input: City with baseline values
    ├── Process:
    │   ├── Generate random temp variation: ±2°C
    │   ├── Generate random rainfall variation: ±5mm
    │   ├── Add variation to baseline
    │   └── Clamp to valid ranges
    └── Output: WeatherData with calculated values
```

---

## Domain Layer Ports (Interfaces)

### WeatherDataRepository (Port)

Interface for retrieving city data. Implemented by infrastructure layer.

```java
public interface WeatherDataRepository {
    Optional<City> findByCityCode(CityCode code);
    List<City> findAll();
}
```

---

## Application Layer DTOs

### WeatherRequest

```
WeatherRequest
└── cityCode: String
```

### WeatherResponse

```
WeatherResponse
├── cityCode: String
├── cityName: String (Chinese)
├── temperature: BigDecimal
├── rainfall: BigDecimal
└── updatedAt: LocalDateTime
```

### TraceInfo

```
TraceInfo
├── traceId: String (32 hex chars)
├── spanId: String (16 hex chars)
└── duration: Long (milliseconds)
```

### ApiResponse<T>

Generic wrapper for all API responses.

```
ApiResponse<T>
├── success: Boolean
├── data: T (nullable)
├── errorCode: String (nullable)
├── errorMessage: String (nullable)
└── traceInfo: TraceInfo
```

---

## Application Layer Mappers

### WeatherMapper

```
WeatherMapper
├── toResponse(city: City, weatherData: WeatherData): WeatherResponse
├── toApiResponse(response: WeatherResponse, traceInfo: TraceInfo): ApiResponse<WeatherResponse>
└── toErrorResponse(errorCode: String, message: String, traceInfo: TraceInfo): ApiResponse<Void>
```

---

## Infrastructure Layer Entities (Persistence)

### WeatherDataEntity (JPA Entity)

Database representation for H2 persistence.

```sql
CREATE TABLE weather_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    city_code VARCHAR(10) NOT NULL UNIQUE,
    city_name VARCHAR(50) NOT NULL,
    base_temperature DECIMAL(5,2) NOT NULL,
    base_rainfall DECIMAL(5,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Mapping to Domain**:
```
WeatherDataEntity (Infrastructure)
        ↓ (H2WeatherRepository adapter)
City (Domain Entity)
```

### QueryHistory (JPA Entity - Optional)

Audit table for trace demonstration.

```sql
CREATE TABLE query_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    city_code VARCHAR(10) NOT NULL,
    trace_id VARCHAR(64),
    queried_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    response_time_ms BIGINT
);
```

---

## Entity Relationships

```
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                            │
│                                                              │
│   ┌──────────────┐    contains    ┌─────────────────────┐   │
│   │    City      │───────────────▶│  CityCode (VO)      │   │
│   │   (Entity)   │                └─────────────────────┘   │
│   │              │    contains    ┌─────────────────────┐   │
│   │              │───────────────▶│  Temperature (VO)   │   │
│   │              │                └─────────────────────┘   │
│   │              │    contains    ┌─────────────────────┐   │
│   │              │───────────────▶│  Rainfall (VO)      │   │
│   └──────────────┘                └─────────────────────┘   │
│          │                                                   │
│          │ loaded by                                         │
│          ▼                                                   │
│   ┌──────────────────────────┐                              │
│   │ WeatherDataRepository    │  ◀─── Port (Interface)       │
│   │        (Port)            │                              │
│   └──────────────────────────┘                              │
└──────────────────────────────────────────────────────────────┘
                    │
                    │ implemented by
                    ▼
┌─────────────────────────────────────────────────────────────┐
│                  Infrastructure Layer                        │
│                                                              │
│   ┌──────────────────────────┐    maps to   ┌────────────┐  │
│   │  H2WeatherRepository     │─────────────▶│   City     │  │
│   │      (Adapter)           │              │  (Domain)  │  │
│   └──────────────────────────┘              └────────────┘  │
│          │                                                   │
│          │ uses                                              │
│          ▼                                                   │
│   ┌──────────────────────────┐                              │
│   │  WeatherDataEntity       │  ◀─── JPA Entity             │
│   │      (JPA)               │                              │
│   └──────────────────────────┘                              │
└─────────────────────────────────────────────────────────────┘
```

---

## State Transitions

### Weather Query Flow

```
[Initial] ─────▶ [Query Received]
                       │
                       ▼
              [City Code Validated]
                  │         │
         (valid)  │         │ (invalid)
                  ▼         ▼
        [Fetch from DB]   [Return 404 Error]
                  │
                  ▼
        [Apply Random Variation]
                  │
                  ▼
        [Clamp to Valid Range]
                  │
                  ▼
        [Build Response with TraceInfo]
                  │
                  ▼
              [Return 200 OK]
```

---

## Validation Rules

| Entity/VO | Field | Rule |
|-----------|-------|------|
| CityCode | value | Must be "TPE", "TXG", or "KHH" |
| Temperature | value | 15.0 <= value <= 35.0 |
| Rainfall | value | 0.0 <= value <= 50.0 |
| City | name | Non-empty, max 50 chars |
| TraceInfo | traceId | 32 hexadecimal characters |
| TraceInfo | spanId | 16 hexadecimal characters |

---

## Error Codes

| Code | HTTP Status | Message (Chinese) |
|------|-------------|-------------------|
| CITY_NOT_FOUND | 404 | 找不到指定的城市 |
| INVALID_CITY_CODE | 400 | 城市代碼格式錯誤 |
| SERVICE_UNAVAILABLE | 503 | 服務暫時不可用 |
| INTERNAL_ERROR | 500 | 內部錯誤，請稍後再試 |
