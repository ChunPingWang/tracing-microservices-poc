# Weather Tracing PoC - 天氣查詢可觀測性展示系統

A comprehensive demonstration system showcasing distributed tracing and observability patterns using a weather query application.

> **學習目標**: 透過這個專案，您將學習到分散式追蹤、可觀測性三大支柱、Trace ID 與 Span ID 的概念，以及如何整合 OpenTelemetry、Jaeger、Prometheus 和 Grafana。

---

## Table of Contents

1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Observability Deep Dive](#observability-deep-dive)
   - [What is Observability?](#what-is-observability)
   - [Trace ID vs Span ID](#trace-id-vs-span-id)
   - [W3C Trace Context](#w3c-trace-context)
4. [Development Tools](#development-tools)
   - [Swagger API Documentation](#swagger-api-documentation)
   - [H2 Database Console](#h2-database-console)
5. [Test Examples](#test-examples)
   - [API Testing with cURL](#api-testing-with-curl)
   - [Integration Tests](#integration-tests)
6. [Architecture](#architecture)
7. [Technology Stack](#technology-stack)
8. [Project Structure](#project-structure)

---

## Overview

This project demonstrates modern observability practices through a simple weather query system. Users can query weather data for three Taiwan cities (Taipei, Taichung, Kaohsiung), and the system showcases:

- **Distributed Tracing**: Track requests across multiple services using OpenTelemetry
- **Metrics Collection**: Monitor system performance through Prometheus and Micrometer
- **API Documentation**: Interactive API testing with Swagger UI
- **Dashboard Visualization**: Real-time monitoring through Grafana

### Key Features

| Feature | Description |
|---------|-------------|
| Weather Query | Query weather for 3 cities: 台北 (TPE), 台中 (TXG), 高雄 (KHH) |
| Trace Visualization | View complete request chain through Jaeger UI |
| Metrics Dashboard | Monitor system metrics through Grafana |
| Swagger UI | Interactive API documentation and testing |
| H2 Console | In-browser database management |

---

## Quick Start

### Prerequisites

- Java 21+
- Node.js 18+
- Docker & Docker Compose
- Gradle 8.x

### Step 1: Clone and Build

```bash
# Clone the repository
git clone <repository-url>
cd weather-4-observability-poc

# Build the project
./gradlew build
```

### Step 2: Start Services

#### Option A: Docker Compose (Recommended)

```bash
# Start all services
docker compose up -d

# Check status
docker compose ps
```

#### Option B: Local Development

```bash
# Terminal 1: Start observability stack
docker compose -f docker-compose.dev.yml up -d

# Terminal 2: Start Weather Service
./gradlew :weather-service:bootRun

# Terminal 3: Start Gateway
./gradlew :gateway:bootRun

# Terminal 4: Start Frontend
cd frontend && npm install && npm run dev
```

### Step 3: Access Services

| Service | URL | Description |
|---------|-----|-------------|
| Frontend | http://localhost:5173 | Vue.js Web UI |
| Gateway API | http://localhost:8080/api | API Gateway |
| Weather Service | http://localhost:8081 | Backend Service |
| **Swagger UI** | http://localhost:8081/swagger-ui.html | API Documentation |
| **H2 Console** | http://localhost:8081/h2-console | Database Console |
| Jaeger UI | http://localhost:16686 | Trace Visualization |
| Prometheus | http://localhost:9090 | Metrics Database |
| Grafana | http://localhost:3000 | Dashboards |

---

## Observability Deep Dive

### What is Observability?

可觀測性 (Observability) 是指透過系統的外部輸出來理解系統內部狀態的能力。它建立在三大支柱之上：

```
                    ┌─────────────────────────────────────────┐
                    │           OBSERVABILITY                  │
                    │      Understanding System Behavior       │
                    └───────────────┬─────────────────────────┘
                                    │
          ┌─────────────────────────┼─────────────────────────┐
          │                         │                         │
          ▼                         ▼                         ▼
   ┌─────────────┐          ┌─────────────┐          ┌─────────────┐
   │   TRACES    │          │   METRICS   │          │    LOGS     │
   │   (追蹤)    │          │   (指標)    │          │   (日誌)    │
   ├─────────────┤          ├─────────────┤          ├─────────────┤
   │ What path   │          │ What's the  │          │ What        │
   │ did this    │          │ system's    │          │ happened    │
   │ request     │          │ health?     │          │ in detail?  │
   │ take?       │          │             │          │             │
   └──────┬──────┘          └──────┬──────┘          └──────┬──────┘
          │                        │                        │
          ▼                        ▼                        ▼
   ┌─────────────┐          ┌─────────────┐          ┌─────────────┐
   │   Jaeger    │          │ Prometheus  │          │   Grafana   │
   │             │          │   Grafana   │          │    Loki     │
   └─────────────┘          └─────────────┘          └─────────────┘
```

### Trace ID vs Span ID

這是理解分散式追蹤最重要的兩個概念：

#### Trace ID (追蹤識別碼)

- **長度**: 32 個十六進位字元 (128 bits)
- **範例**: `4bf92f3577b34da6a3ce929d0e0e4736`
- **意義**: 代表一個完整的請求生命週期
- **特性**: 在整個請求鏈路中保持不變

```
┌─────────────────────────────────────────────────────────────────┐
│                        一個 Trace                                │
│  Trace ID: 4bf92f3577b34da6a3ce929d0e0e4736                     │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ Request: GET /api/weather/TPE                              │  │
│  │                                                            │  │
│  │  Frontend → Gateway → Weather Service → Database           │  │
│  │                                                            │  │
│  │  (所有服務共享同一個 Trace ID)                              │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

#### Span ID (跨度識別碼)

- **長度**: 16 個十六進位字元 (64 bits)
- **範例**: `00f067aa0ba902b7`
- **意義**: 代表請求鏈路中的一個單獨操作
- **特性**: 每個操作有自己唯一的 Span ID

```
Trace ID: 4bf92f3577b34da6a3ce929d0e0e4736
├── Span ID: a1b2c3d4e5f6g7h8 (Gateway 處理)
│   └── Span ID: 00f067aa0ba902b7 (Weather Service 處理)
│       └── Span ID: 1234567890abcdef (Database 查詢)
```

#### 視覺化範例

```
時間軸 ─────────────────────────────────────────────────────────►

Trace: 4bf92f3577b34da6a3ce929d0e0e4736

Gateway Span
├─────────────────────────────────────────────────────┤
│ span_id: a1b2c3d4e5f6g7h8                           │
│ duration: 120ms                                      │
└─────────────────────────────────────────────────────┘
        │
        ▼
    Weather Service Span
    ├─────────────────────────────────────────┤
    │ span_id: 00f067aa0ba902b7               │
    │ parent_span_id: a1b2c3d4e5f6g7h8        │
    │ duration: 80ms                          │
    └─────────────────────────────────────────┘
            │
            ▼
        Database Span
        ├───────────────────┤
        │ span_id: 1234...  │
        │ duration: 15ms    │
        └───────────────────┘
```

### W3C Trace Context

W3C Trace Context 是分散式追蹤的標準協定，定義了如何在 HTTP 標頭中傳遞追蹤資訊：

```http
traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
             ││ │                                │                 │
             ││ │                                │                 └── Flags (01 = sampled)
             ││ │                                └── Parent Span ID (16 hex)
             ││ └── Trace ID (32 hex characters)
             │└── Version (currently always 00)
             └── Header name
```

#### 在本專案中的實際運作

```
1. 瀏覽器發送請求
   GET /api/weather/TPE
   (此時還沒有 traceparent)

2. Gateway 創建新的 Trace
   traceparent: 00-{new-trace-id}-{gateway-span-id}-01

3. Gateway 轉發到 Weather Service
   traceparent: 00-{same-trace-id}-{gateway-span-id}-01

4. Weather Service 創建子 Span
   parent_span_id = gateway-span-id
   span_id = new-span-id

5. 回應包含追蹤資訊
   X-Trace-Id: {trace-id}
   X-Span-Id: {span-id}
```

### OpenTelemetry 架構

OpenTelemetry 是目前最廣泛採用的可觀測性框架：

```
┌─────────────────────────────────────────────────────────────────────┐
│                         APPLICATION                                  │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                   OpenTelemetry SDK                          │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │   │
│  │  │   Tracer    │  │   Meter     │  │   Logger    │          │   │
│  │  │   Provider  │  │   Provider  │  │   Provider  │          │   │
│  │  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘          │   │
│  │         │ Traces         │ Metrics       │ Logs             │   │
│  │         └────────────────┴───────────────┘                  │   │
│  │                          │                                   │   │
│  │                   ┌──────▼──────┐                           │   │
│  │                   │   Exporter  │  (OTLP Protocol)          │   │
│  │                   └──────┬──────┘                           │   │
│  └──────────────────────────┼───────────────────────────────────┘   │
└─────────────────────────────┼───────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    OpenTelemetry Collector                           │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────────┐ │
│  │  Receivers  │───▶│ Processors  │───▶│       Exporters         │ │
│  │  (OTLP)     │    │  (Batch,    │    │  ┌───────┐ ┌─────────┐ │ │
│  │             │    │   Filter)   │    │  │Jaeger │ │Prometheus│ │ │
│  └─────────────┘    └─────────────┘    │  └───────┘ └─────────┘ │ │
│                                         └─────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Development Tools

### Swagger API Documentation

Swagger UI 提供互動式的 API 文件，讓您可以直接在瀏覽器中測試 API。

#### 存取方式

| 資源 | URL |
|------|-----|
| Swagger UI | http://localhost:8081/swagger-ui.html |
| OpenAPI JSON | http://localhost:8081/v3/api-docs |
| OpenAPI YAML | http://localhost:8081/v3/api-docs.yaml |

#### 使用步驟

1. **開啟 Swagger UI**: 瀏覽 http://localhost:8081/swagger-ui.html

2. **選擇 API 端點**: 點擊 `GET /weather/{cityCode}` 展開

3. **測試 API**:
   - 點擊 "Try it out" 按鈕
   - 在 `cityCode` 欄位輸入 `TPE`、`TXG` 或 `KHH`
   - 點擊 "Execute" 執行請求

4. **查看結果**:
   ```json
   {
     "success": true,
     "data": {
       "cityCode": "TPE",
       "cityName": "台北市",
       "temperature": 25.5,
       "rainfall": 2.3,
       "updatedAt": "2026-01-21T10:30:00"
     },
     "traceInfo": {
       "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
       "spanId": "00f067aa0ba902b7",
       "duration": 45
     }
   }
   ```

5. **查看追蹤**: 複製 `traceId`，前往 Jaeger UI 搜尋完整追蹤鏈路

#### Swagger 截圖示意

```
┌─────────────────────────────────────────────────────────────────┐
│  Swagger UI - Weather Tracing PoC API                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Weather                                                         │
│  天氣查詢 API - 提供台灣主要城市天氣資訊                         │
│                                                                  │
│  ▼ GET /weather/{cityCode}  查詢城市天氣                        │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ Parameters                                                   ││
│  │ ┌─────────────────────────────────────────────────────────┐ ││
│  │ │ cityCode *     string    path                           │ ││
│  │ │ 城市代碼 (TPE/TXG/KHH)                                  │ ││
│  │ │ ┌─────────────────────────────────────┐                 │ ││
│  │ │ │ TPE                               ▼ │                 │ ││
│  │ │ └─────────────────────────────────────┘                 │ ││
│  │ └─────────────────────────────────────────────────────────┘ ││
│  │                                                              ││
│  │ [Try it out]  [Execute]                                     ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### H2 Database Console

H2 是一個輕量級的嵌入式資料庫，提供 Web-based 的管理介面。

#### 存取方式

1. **開啟 H2 Console**: 瀏覽 http://localhost:8081/h2-console

2. **連線設定**:
   | 欄位 | 值 |
   |------|-----|
   | JDBC URL | `jdbc:h2:mem:weatherdb` |
   | User Name | `sa` |
   | Password | (空白) |

3. **點擊 "Connect" 連線**

#### 資料庫結構

```sql
-- 城市天氣基準資料表
CREATE TABLE weather_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    city_code VARCHAR(10) NOT NULL UNIQUE,  -- TPE, TXG, KHH
    city_name VARCHAR(50) NOT NULL,          -- 台北市, 台中市, 高雄市
    base_temperature DECIMAL(5,2) NOT NULL,  -- 基準溫度
    base_rainfall DECIMAL(5,2) NOT NULL,     -- 基準降雨量
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 預設資料

| city_code | city_name | base_temperature | base_rainfall |
|-----------|-----------|------------------|---------------|
| TPE | 台北市 | 25.0 | 10.0 |
| TXG | 台中市 | 26.0 | 8.0 |
| KHH | 高雄市 | 28.0 | 5.0 |

#### 常用 SQL 查詢

```sql
-- 查詢所有城市資料
SELECT * FROM weather_data;

-- 查詢特定城市
SELECT * FROM weather_data WHERE city_code = 'TPE';

-- 更新基準溫度
UPDATE weather_data SET base_temperature = 27.0 WHERE city_code = 'KHH';
```

---

## Test Examples

### API Testing with cURL

#### 1. 基本天氣查詢

```bash
# 查詢台北天氣
curl -X GET "http://localhost:8080/api/weather/TPE" \
  -H "Accept: application/json" | jq

# 預期回應
{
  "success": true,
  "data": {
    "cityCode": "TPE",
    "cityName": "台北市",
    "temperature": 25.5,
    "rainfall": 12.3,
    "updatedAt": "2026-01-21T10:30:00"
  },
  "traceInfo": {
    "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
    "spanId": "00f067aa0ba902b7",
    "duration": 45
  }
}
```

#### 2. 查詢所有城市

```bash
# 台北
curl -s "http://localhost:8080/api/weather/TPE" | jq '.data.cityName, .data.temperature'

# 台中
curl -s "http://localhost:8080/api/weather/TXG" | jq '.data.cityName, .data.temperature'

# 高雄
curl -s "http://localhost:8080/api/weather/KHH" | jq '.data.cityName, .data.temperature'
```

#### 3. 檢查回應標頭 (Trace Headers)

```bash
# 使用 -i 顯示回應標頭
curl -i "http://localhost:8080/api/weather/TPE"

# 預期標頭
# HTTP/1.1 200 OK
# X-Trace-Id: 4bf92f3577b34da6a3ce929d0e0e4736
# X-Span-Id: 00f067aa0ba902b7
# X-Request-Duration: 45
```

#### 4. 追蹤 ID 追蹤

```bash
# 發送請求並擷取 Trace ID
TRACE_ID=$(curl -s -i "http://localhost:8080/api/weather/TPE" | grep -i "x-trace-id" | cut -d' ' -f2 | tr -d '\r')

echo "Trace ID: $TRACE_ID"

# 在 Jaeger UI 查看追蹤
echo "View trace: http://localhost:16686/trace/$TRACE_ID"
```

#### 5. 錯誤處理測試

```bash
# 無效城市代碼 (預期 400 Bad Request)
curl -s "http://localhost:8080/api/weather/INVALID" | jq

# 預期回應
{
  "success": false,
  "error": {
    "code": "INVALID_CITY_CODE",
    "message": "Invalid city code: INVALID. Valid codes are: TPE, TXG, KHH"
  },
  "traceInfo": {
    "traceId": "...",
    "spanId": "...",
    "duration": 5
  }
}
```

#### 6. 健康檢查

```bash
# Weather Service 健康檢查
curl -s "http://localhost:8081/actuator/health" | jq

# Gateway 健康檢查
curl -s "http://localhost:8080/actuator/health" | jq
```

#### 7. Prometheus Metrics

```bash
# 查看 Weather Service Metrics
curl -s "http://localhost:8081/actuator/prometheus" | head -50

# 查看特定指標
curl -s "http://localhost:8081/actuator/prometheus" | grep "http_server_requests"
```

### Integration Tests

本專案提供完整的整合測試腳本。

#### 執行整合測試

```bash
# 執行完整測試
./scripts/integration-test.sh

# 等待服務就緒後執行
./scripts/integration-test.sh --wait

# 只執行健康檢查
./scripts/integration-test.sh --health-only

# 只執行 API 測試
./scripts/integration-test.sh --api-only

# 只執行追蹤測試
./scripts/integration-test.sh --trace-only

# 詳細輸出
./scripts/integration-test.sh --verbose
```

#### 煙霧測試 (快速驗證)

```bash
./scripts/smoke-test.sh
```

#### 測試項目清單

| 測試類別 | 測試項目 | 說明 |
|---------|---------|------|
| **健康檢查** | Weather Service Health | 驗證後端服務正常運作 |
| | Gateway Health | 驗證 API Gateway 正常運作 |
| | Jaeger Health | 驗證追蹤系統正常運作 |
| | Prometheus Health | 驗證指標系統正常運作 |
| | Grafana Health | 驗證儀表板正常運作 |
| **API 測試** | Weather API - TPE | 查詢台北天氣 |
| | Weather API - TXG | 查詢台中天氣 |
| | Weather API - KHH | 查詢高雄天氣 |
| | Invalid City | 驗證錯誤處理 |
| **追蹤測試** | Trace Header Present | 驗證回應包含追蹤標頭 |
| | Trace ID Format | 驗證 Trace ID 格式正確 |
| | Trace in Jaeger | 驗證追蹤資料已匯出至 Jaeger |
| **效能測試** | Response Time | 驗證回應時間低於閾值 |
| | Concurrent Requests | 驗證並發請求處理能力 |

#### 測試輸出範例

```
============================================================
  Weather Tracing PoC - Integration Test Suite
  整合測試套件
============================================================

Configuration:
  Gateway URL:         http://localhost:8080
  Weather Service URL: http://localhost:8081
  Jaeger URL:          http://localhost:16686

--- Health Check Tests / 健康檢查測試 ---

[INFO] Running test: Weather Service Health
[PASS] Test passed: Weather Service Health

[INFO] Running test: Gateway Health
[PASS] Test passed: Gateway Health

--- Weather API Tests / 天氣 API 測試 ---

[INFO] Running test: Weather API - Taipei (TPE)
[PASS] Test passed: Weather API - Taipei (TPE)

--- Trace Propagation Tests / 追蹤傳播測試 ---

[INFO] Running test: Trace Header Present
[PASS] Test passed: Trace Header Present

============================================================
  Test Summary / 測試摘要
============================================================

  Total tests:  15
  Passed:       15
  Failed:       0

[PASS] All tests passed! 所有測試通過!
```

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
│  │  │  │  • WeatherController (REST)                         │   │  │  │
│  │  │  │  • SwaggerConfig (API Docs)                         │   │  │  │
│  │  │  │  • H2WeatherRepository (Adapter)                    │   │  │  │
│  │  │  │  ┌─────────────────────────────────────────────┐    │   │  │  │
│  │  │  │  │         Application Layer                    │    │   │  │  │
│  │  │  │  │  • GetWeatherUseCase                        │    │   │  │  │
│  │  │  │  │  • WeatherMapper                            │    │   │  │  │
│  │  │  │  │  ┌─────────────────────────────────────┐    │    │   │  │  │
│  │  │  │  │  │         Domain Layer                 │    │    │   │  │  │
│  │  │  │  │  │  • City Entity                      │    │    │   │  │  │
│  │  │  │  │  │  • CityCode, Temperature, Rainfall  │    │    │   │  │  │
│  │  │  │  │  │  • WeatherCalculationService        │    │    │   │  │  │
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
│  └────────────────────────────┘  └──────────────────┬─────────────┘   │
│                                                      │                  │
│                                                      ▼                  │
│                                    ┌────────────────────────────────┐  │
│                                    │          Grafana               │  │
│                                    │       localhost:3000           │  │
│                                    └────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

### Sequence Diagram

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
    │  10. View complete trace with spans│                   │               │
    │<────────────────────────────────────────────────────────────────────────
    │                │                   │                   │               │
```

---

## Technology Stack

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| **Frontend** | Vue.js | 3.4.x | Reactive UI framework |
| | Vite | 5.x | Build tool |
| | TypeScript | 5.3.x | Type safety |
| | Axios | 1.6.x | HTTP client |
| **API Gateway** | Spring Cloud Gateway | 4.1.x | Routing & filtering |
| | Spring Boot | 3.2.x | Application framework |
| **Backend** | Spring Boot | 3.2.x | Application framework |
| | Spring Data JPA | 3.2.x | ORM |
| | SpringDoc OpenAPI | 2.3.x | **Swagger UI** |
| | Java | 21 (LTS) | Language runtime |
| **Database** | H2 | 2.x | Embedded database |
| **Tracing** | OpenTelemetry | 1.35+ | Instrumentation |
| | Jaeger | 1.54+ | Trace storage & UI |
| **Metrics** | Prometheus | 2.50+ | Time-series DB |
| | Micrometer | 1.12+ | Metrics facade |
| **Visualization** | Grafana | 10.x | Dashboards |

---

## Project Structure

```
weather-4-observability-poc/
├── frontend/                  # Vue.js Frontend
│   ├── src/
│   │   ├── components/       # Vue components
│   │   │   ├── CitySelector.vue
│   │   │   ├── WeatherCard.vue
│   │   │   └── TraceInfo.vue
│   │   ├── composables/      # Composition API hooks
│   │   │   └── useWeather.ts
│   │   ├── services/         # API clients
│   │   │   └── weatherApi.ts
│   │   └── types/            # TypeScript types
│   │       └── weather.ts
│   └── package.json
│
├── gateway/                   # Spring Cloud Gateway
│   └── src/main/java/com/example/gateway/
│       ├── config/           # Route, CORS config
│       └── GatewayApplication.java
│
├── weather-service/           # Spring Boot Backend (Hexagonal)
│   └── src/main/java/com/example/weather/
│       ├── domain/           # Domain Layer
│       │   ├── entities/     # City
│       │   ├── value_objects/# CityCode, Temperature, Rainfall
│       │   ├── services/     # WeatherCalculationService
│       │   └── ports/        # WeatherDataRepository
│       ├── application/      # Application Layer
│       │   ├── use_cases/    # GetWeatherUseCase
│       │   ├── mappers/      # WeatherMapper
│       │   └── dto/          # Request, Response, TraceInfo
│       └── infrastructure/   # Infrastructure Layer
│           ├── web/          # WeatherController
│           ├── adapters/     # H2WeatherRepository
│           ├── persistence/  # JPA entities
│           └── config/       # SwaggerConfig, ObservabilityConfig
│
├── observability/            # Observability Stack Config
│   ├── otel-collector/       # OpenTelemetry Collector
│   ├── prometheus/           # Prometheus config
│   ├── jaeger/               # Jaeger config
│   └── grafana/              # Grafana dashboards
│
├── scripts/                  # Test & Deployment scripts
│   ├── integration-test.sh   # Full integration tests
│   ├── smoke-test.sh         # Quick smoke tests
│   └── start-services.sh     # Service management
│
├── docker-compose.yml        # Full stack deployment
└── docker-compose.dev.yml    # Development (observability only)
```

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
    "cityName": "台北市",
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

**Error Response (400):**
```json
{
  "success": false,
  "error": {
    "code": "INVALID_CITY_CODE",
    "message": "Invalid city code: ABC. Valid codes are: TPE, TXG, KHH"
  },
  "traceInfo": {
    "traceId": "...",
    "spanId": "...",
    "duration": 5
  }
}
```

---

## Further Learning

### Recommended Resources

| Topic | Resource |
|-------|----------|
| OpenTelemetry | [opentelemetry.io](https://opentelemetry.io/) |
| W3C Trace Context | [W3C Specification](https://www.w3.org/TR/trace-context/) |
| Jaeger | [jaegertracing.io](https://www.jaegertracing.io/) |
| Prometheus | [prometheus.io](https://prometheus.io/) |
| Grafana | [grafana.com](https://grafana.com/) |
| Hexagonal Architecture | [Alistair Cockburn's Article](https://alistair.cockburn.us/hexagonal-architecture/) |

### Key Takeaways

1. **Trace ID** 是整個請求鏈路的唯一識別碼，跨越所有服務
2. **Span ID** 是單一操作的識別碼，形成 Trace 的樹狀結構
3. **OpenTelemetry** 提供統一的可觀測性標準和 SDK
4. **Hexagonal Architecture** 將業務邏輯與外部依賴隔離
5. **三大支柱** (Traces, Metrics, Logs) 共同提供完整的可觀測性

---

## License

MIT License - See LICENSE file for details.
