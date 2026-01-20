# Weather Tracing PoC - Technical Specification (TECH.md)

## 1. 技術架構總覽

### 1.1 系統架構圖

```
                                    ┌─────────────────────────────────────────────────────────┐
                                    │                    Observability Stack                   │
                                    │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │
                                    │  │   Jaeger    │  │ Prometheus  │  │    Grafana      │  │
                                    │  │   :16686    │  │   :9090     │  │     :3000       │  │
                                    │  └──────▲──────┘  └──────▲──────┘  └────────▲────────┘  │
                                    │         │                │                   │          │
                                    │         │    ┌───────────┴───────────┐      │          │
                                    │         │    │                       │      │          │
                                    │  ┌──────┴────┴───────────────────────┴──────┴──────┐   │
                                    │  │           OpenTelemetry Collector               │   │
                                    │  │           (OTLP Receiver → Exporters)           │   │
                                    │  │                    :4317/:4318                  │   │
                                    │  └──────────────────────▲──────────────────────────┘   │
                                    └──────────────────────────┼──────────────────────────────┘
                                                               │
                              ┌─────────────────────────────────┼─────────────────────────────────┐
                              │                                │          Application Stack       │
┌──────────────┐              │  ┌──────────────────┐    ┌─────┴──────────────┐                   │
│              │   HTTP       │  │                  │    │                    │                   │
│   Vue.js     │─────────────────▶  Spring Cloud   │────▶  Weather Service   │                   │
│   Frontend   │              │  │    Gateway      │    │   (Spring Boot)    │                   │
│    :5173     │              │  │     :8080       │    │      :8081         │                   │
│              │              │  │                  │    │                    │                   │
└──────────────┘              │  └──────────────────┘    └─────────┬──────────┘                   │
                              │         │                          │                              │
                              │         │                          │                              │
                              │         │                    ┌─────▼──────┐                       │
                              │         │                    │    H2      │                       │
                              │         │                    │  Database  │                       │
                              │         │                    │ (Embedded) │                       │
                              │         │                    └────────────┘                       │
                              └─────────┼──────────────────────────────────────────────────────────┘
                                        │
                                        ▼
                              Trace Context Propagation
                              (W3C Trace Context / B3)
```

### 1.2 技術選型

| 層級 | 技術 | 版本 | 說明 |
|------|------|------|------|
| **Frontend** | Vue.js | 3.x | SFC with Composition API |
| | Vite | 5.x | Build tool |
| | Axios | 1.x | HTTP Client |
| **Gateway** | Spring Cloud Gateway | 4.1.x | API Gateway |
| | Spring Boot | 3.2.x | Framework |
| **Backend** | Spring Boot | 3.2.x | Microservice Framework |
| | Spring Data JPA | 3.2.x | Data Access |
| | H2 Database | 2.x | Embedded Database |
| **Observability** | OpenTelemetry | 1.35+ | Telemetry Collection |
| | Jaeger | 1.54+ | Distributed Tracing |
| | Prometheus | 2.50+ | Metrics Storage |
| | Grafana | 10.x | Visualization |
| **Runtime** | Java | 21 (LTS) | JVM Runtime |
| | Node.js | 20.x (LTS) | Frontend Runtime |

---

## 2. 專案結構

### 2.1 Monorepo 結構
```
weather-tracing-poc/
├── docs/                           # 文檔
│   ├── PRD.md
│   ├── TECH.md
│   └── INFRA.md
├── frontend/                       # Vue.js 前端
│   ├── src/
│   │   ├── components/
│   │   │   ├── CitySelector.vue
│   │   │   ├── WeatherCard.vue
│   │   │   └── TraceInfo.vue
│   │   ├── composables/
│   │   │   └── useWeather.ts
│   │   ├── services/
│   │   │   └── weatherApi.ts
│   │   ├── types/
│   │   │   └── weather.ts
│   │   ├── App.vue
│   │   └── main.ts
│   ├── public/
│   ├── index.html
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   └── Dockerfile
├── gateway/                        # Spring Cloud Gateway
│   ├── src/main/java/
│   │   └── com/example/gateway/
│   │       ├── GatewayApplication.java
│   │       ├── config/
│   │       │   ├── RouteConfig.java
│   │       │   ├── CorsConfig.java
│   │       │   └── ObservabilityConfig.java
│   │       └── filter/
│   │           └── TraceHeaderFilter.java
│   ├── src/main/resources/
│   │   └── application.yml
│   ├── build.gradle
│   └── Dockerfile
├── weather-service/                # 天氣服務
│   ├── src/main/java/
│   │   └── com/example/weather/
│   │       ├── WeatherServiceApplication.java
│   │       ├── config/
│   │       │   └── ObservabilityConfig.java
│   │       ├── controller/
│   │       │   └── WeatherController.java
│   │       ├── service/
│   │       │   └── WeatherService.java
│   │       ├── repository/
│   │       │   └── WeatherRepository.java
│   │       ├── entity/
│   │       │   └── WeatherData.java
│   │       └── dto/
│   │           ├── WeatherResponse.java
│   │           └── TraceInfo.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── schema.sql
│   │   └── data.sql
│   ├── build.gradle
│   └── Dockerfile
├── observability/                  # 可觀測性配置
│   ├── otel-collector/
│   │   └── otel-collector-config.yaml
│   ├── prometheus/
│   │   └── prometheus.yml
│   ├── grafana/
│   │   ├── provisioning/
│   │   │   ├── dashboards/
│   │   │   │   ├── dashboard.yml
│   │   │   │   └── weather-service.json
│   │   │   └── datasources/
│   │   │       └── datasource.yml
│   │   └── grafana.ini
│   └── jaeger/
│       └── jaeger-config.yml
├── k8s/                            # Kubernetes 配置
│   ├── base/
│   │   ├── namespace.yaml
│   │   ├── frontend/
│   │   ├── gateway/
│   │   ├── weather-service/
│   │   └── observability/
│   ├── overlays/
│   │   ├── ingress/
│   │   │   ├── kustomization.yaml
│   │   │   └── ingress.yaml
│   │   └── loadbalancer/
│   │       ├── kustomization.yaml
│   │       └── services.yaml
│   └── kind/
│       ├── kind-config-ingress.yaml
│       └── kind-config-lb.yaml
├── scripts/                        # 部署腳本
│   ├── local-dev.sh               # 本機開發啟動
│   ├── docker-compose-up.sh       # Docker Compose 啟動
│   ├── k8s-deploy-ingress.sh      # K8s Ingress 部署
│   ├── k8s-deploy-lb.sh           # K8s LoadBalancer 部署
│   └── cleanup.sh                 # 清理腳本
├── docker-compose.yml              # Docker Compose 配置
├── docker-compose.dev.yml          # 開發用 (僅 Observability)
├── settings.gradle
└── README.md
```

---

## 3. 元件詳細設計

### 3.1 Frontend (Vue.js)

#### 3.1.1 技術配置
```typescript
// vite.config.ts
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

#### 3.1.2 Trace Context 處理
前端需要從 Response Header 取得 Trace ID 並顯示：

```typescript
// services/weatherApi.ts
import axios from 'axios';

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 10000
});

export interface WeatherResponse {
  success: boolean;
  data?: WeatherData;
  error?: ErrorInfo;
  traceInfo: TraceInfo;
}

export interface TraceInfo {
  traceId: string;
  spanId: string;
  duration: number;
}

export const getWeather = async (cityCode: string): Promise<WeatherResponse> => {
  const startTime = Date.now();
  const response = await apiClient.get(`/weather/${cityCode}`);
  
  // 從 Response Header 取得 Trace ID（備用）
  const traceId = response.headers['x-trace-id'] || response.data.traceInfo?.traceId;
  
  return {
    ...response.data,
    traceInfo: {
      ...response.data.traceInfo,
      traceId,
      duration: Date.now() - startTime
    }
  };
};
```

#### 3.1.3 元件設計
```vue
<!-- components/TraceInfo.vue -->
<template>
  <div class="trace-info" v-if="traceInfo">
    <h3>🔍 追蹤資訊</h3>
    <div class="trace-details">
      <div class="trace-id">
        <span class="label">Trace ID:</span>
        <code>{{ shortTraceId }}</code>
        <a :href="jaegerUrl" target="_blank" class="view-link">
          查看詳情 →
        </a>
      </div>
      <div class="duration">
        <span class="label">耗時:</span>
        <span>{{ traceInfo.duration }}ms</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { TraceInfo } from '@/types/weather';

const props = defineProps<{
  traceInfo: TraceInfo | null;
}>();

const shortTraceId = computed(() => 
  props.traceInfo?.traceId?.substring(0, 16) + '...'
);

const jaegerUrl = computed(() => 
  `http://localhost:16686/trace/${props.traceInfo?.traceId}`
);
</script>
```

### 3.2 Spring Cloud Gateway

#### 3.2.1 依賴配置 (build.gradle)
```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.2'
    id 'io.spring.dependency-management' version '1.1.4'
}

java {
    sourceCompatibility = '21'
}

dependencies {
    // Spring Cloud Gateway
    implementation 'org.springframework.cloud:spring-cloud-starter-gateway'
    
    // Actuator for health & metrics
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    
    // OpenTelemetry - 使用 Spring Boot Starter（推薦方式）
    implementation platform('io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:2.1.0')
    implementation 'io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter'
    
    // Micrometer for Prometheus metrics
    implementation 'io.micrometer:micrometer-registry-prometheus'
    
    // Micrometer Tracing Bridge to OpenTelemetry
    implementation 'io.micrometer:micrometer-tracing-bridge-otel'
}

dependencyManagement {
    imports {
        mavenBom 'org.springframework.cloud:spring-cloud-dependencies:2023.0.0'
        mavenBom 'io.opentelemetry:opentelemetry-bom:1.35.0'
    }
}
```

#### 3.2.2 應用配置 (application.yml)
```yaml
server:
  port: 8080

spring:
  application:
    name: weather-gateway
  cloud:
    gateway:
      routes:
        - id: weather-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/weather/**
          filters:
            - RewritePath=/api/weather/(?<segment>.*), /weather/${segment}
            - name: AddResponseHeader
              args:
                name: X-Gateway-Processed
                value: "true"

# OpenTelemetry 配置
otel:
  exporter:
    otlp:
      endpoint: http://localhost:4318
      protocol: http/protobuf
  resource:
    attributes:
      service.name: weather-gateway
      service.version: 1.0.0
      deployment.environment: development

# Actuator 配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: always
  metrics:
    tags:
      application: ${spring.application.name}
  tracing:
    sampling:
      probability: 1.0  # 100% 取樣（PoC 用途）

logging:
  level:
    org.springframework.cloud.gateway: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%X{traceId:-},%X{spanId:-}] %-5level %logger{36} - %msg%n"
```

#### 3.2.3 Trace Header Filter
```java
package com.example.gateway.filter;

import io.opentelemetry.api.trace.Span;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TraceHeaderFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            Span currentSpan = Span.current();
            
            if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
                String traceId = currentSpan.getSpanContext().getTraceId();
                String spanId = currentSpan.getSpanContext().getSpanId();
                
                response.getHeaders().add("X-Trace-Id", traceId);
                response.getHeaders().add("X-Span-Id", spanId);
            }
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
```

### 3.3 Weather Service

#### 3.3.1 依賴配置 (build.gradle)
```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.2'
    id 'io.spring.dependency-management' version '1.1.4'
}

java {
    sourceCompatibility = '21'
}

dependencies {
    // Spring Boot Starters
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    
    // H2 Database
    runtimeOnly 'com.h2database:h2'
    
    // OpenTelemetry - Spring Boot Starter
    implementation platform('io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:2.1.0')
    implementation 'io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter'
    
    // 額外的 OpenTelemetry 自動儀器化（JDBC）
    implementation 'io.opentelemetry.instrumentation:opentelemetry-jdbc'
    
    // Micrometer for Prometheus metrics
    implementation 'io.micrometer:micrometer-registry-prometheus'
    implementation 'io.micrometer:micrometer-tracing-bridge-otel'
    
    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    
    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

dependencyManagement {
    imports {
        mavenBom 'io.opentelemetry:opentelemetry-bom:1.35.0'
    }
}
```

#### 3.3.2 應用配置 (application.yml)
```yaml
server:
  port: 8081

spring:
  application:
    name: weather-service
  
  # H2 Database 配置
  datasource:
    url: jdbc:h2:mem:weatherdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password: 
    # 使用 OpenTelemetry JDBC wrapper
    hikari:
      connection-timeout: 20000
      maximum-pool-size: 10
      minimum-idle: 5
      idle-timeout: 300000
      pool-name: WeatherHikariPool
  
  h2:
    console:
      enabled: true
      path: /h2-console
  
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: true
    properties:
      hibernate:
        format_sql: true
    defer-datasource-initialization: true
  
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql

# OpenTelemetry 配置
otel:
  exporter:
    otlp:
      endpoint: http://localhost:4318
      protocol: http/protobuf
  resource:
    attributes:
      service.name: weather-service
      service.version: 1.0.0
      deployment.environment: development
  instrumentation:
    jdbc:
      enabled: true

# Actuator 配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: always
  metrics:
    tags:
      application: ${spring.application.name}
  tracing:
    sampling:
      probability: 1.0

logging:
  level:
    com.example.weather: DEBUG
    org.hibernate.SQL: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%X{traceId:-},%X{spanId:-}] %-5level %logger{36} - %msg%n"
```

#### 3.3.3 資料庫 Schema (schema.sql)
```sql
-- 天氣資料表
CREATE TABLE IF NOT EXISTS weather_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    city_code VARCHAR(10) NOT NULL UNIQUE,
    city_name VARCHAR(50) NOT NULL,
    base_temperature DECIMAL(5,2) NOT NULL,
    base_rainfall DECIMAL(5,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 查詢歷史記錄表（用於展示更多 DB 操作）
CREATE TABLE IF NOT EXISTS query_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    city_code VARCHAR(10) NOT NULL,
    trace_id VARCHAR(64),
    queried_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    response_time_ms BIGINT
);

-- 建立索引
CREATE INDEX IF NOT EXISTS idx_weather_city_code ON weather_data(city_code);
CREATE INDEX IF NOT EXISTS idx_history_trace_id ON query_history(trace_id);
```

#### 3.3.4 初始資料 (data.sql)
```sql
-- 插入三個城市的基準天氣資料
INSERT INTO weather_data (city_code, city_name, base_temperature, base_rainfall) VALUES
('TPE', '台北', 25.0, 15.0),
('TXG', '台中', 27.0, 10.0),
('KHH', '高雄', 29.0, 8.0);
```

#### 3.3.5 Entity 類別
```java
package com.example.weather.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "weather_data")
@Data
public class WeatherData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "city_code", nullable = false, unique = true, length = 10)
    private String cityCode;
    
    @Column(name = "city_name", nullable = false, length = 50)
    private String cityName;
    
    @Column(name = "base_temperature", nullable = false, precision = 5, scale = 2)
    private BigDecimal baseTemperature;
    
    @Column(name = "base_rainfall", nullable = false, precision = 5, scale = 2)
    private BigDecimal baseRainfall;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

#### 3.3.6 Service 類別（含手動 Span 建立）
```java
package com.example.weather.service;

import com.example.weather.dto.WeatherResponse;
import com.example.weather.entity.WeatherData;
import com.example.weather.repository.WeatherRepository;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {
    
    private final WeatherRepository weatherRepository;
    private final Tracer tracer;
    private final Random random = new Random();
    
    @WithSpan("WeatherService.getWeather")
    public Optional<WeatherResponse> getWeather(
            @SpanAttribute("city.code") String cityCode) {
        
        log.info("Fetching weather for city: {}", cityCode);
        
        // 從資料庫查詢基準資料
        Optional<WeatherData> weatherDataOpt = fetchFromDatabase(cityCode);
        
        if (weatherDataOpt.isEmpty()) {
            log.warn("City not found: {}", cityCode);
            return Optional.empty();
        }
        
        WeatherData weatherData = weatherDataOpt.get();
        
        // 計算模擬天氣（加入隨機變化）
        WeatherResponse response = calculateSimulatedWeather(weatherData);
        
        // 記錄 Span 屬性
        Span currentSpan = Span.current();
        currentSpan.setAttribute("weather.temperature", response.getTemperature().doubleValue());
        currentSpan.setAttribute("weather.rainfall", response.getRainfall().doubleValue());
        
        return Optional.of(response);
    }
    
    @WithSpan("WeatherService.fetchFromDatabase")
    private Optional<WeatherData> fetchFromDatabase(
            @SpanAttribute("db.query.city") String cityCode) {
        
        // 這裡 JPA 查詢會被 OpenTelemetry JDBC 自動追蹤
        return weatherRepository.findByCityCode(cityCode);
    }
    
    private WeatherResponse calculateSimulatedWeather(WeatherData baseData) {
        // 手動建立 Span 展示更細粒度的追蹤
        Span calculationSpan = tracer.spanBuilder("calculateSimulatedWeather")
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("calculation.type", "weather_simulation")
                .startSpan();
        
        try (Scope scope = calculationSpan.makeCurrent()) {
            // 模擬計算延遲（展示用）
            simulateProcessingDelay();
            
            // 加入隨機變化
            BigDecimal tempVariation = BigDecimal.valueOf(random.nextDouble() * 4 - 2)
                    .setScale(1, RoundingMode.HALF_UP);
            BigDecimal rainVariation = BigDecimal.valueOf(random.nextDouble() * 10 - 5)
                    .setScale(1, RoundingMode.HALF_UP);
            
            BigDecimal finalTemp = baseData.getBaseTemperature().add(tempVariation);
            BigDecimal finalRain = baseData.getBaseRainfall().add(rainVariation)
                    .max(BigDecimal.ZERO);  // 雨量不能為負
            
            calculationSpan.setAttribute("variation.temperature", tempVariation.doubleValue());
            calculationSpan.setAttribute("variation.rainfall", rainVariation.doubleValue());
            
            return WeatherResponse.builder()
                    .cityCode(baseData.getCityCode())
                    .cityName(baseData.getCityName())
                    .temperature(finalTemp)
                    .rainfall(finalRain)
                    .updatedAt(LocalDateTime.now())
                    .build();
                    
        } finally {
            calculationSpan.end();
        }
    }
    
    private void simulateProcessingDelay() {
        try {
            // 模擬 10-50ms 的處理時間
            Thread.sleep(10 + random.nextInt(40));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

#### 3.3.7 Controller 類別
```java
package com.example.weather.controller;

import com.example.weather.dto.ApiResponse;
import com.example.weather.dto.TraceInfo;
import com.example.weather.dto.WeatherResponse;
import com.example.weather.service.WeatherService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
@Slf4j
public class WeatherController {
    
    private final WeatherService weatherService;
    
    @GetMapping("/{cityCode}")
    @WithSpan("WeatherController.getWeather")
    public ResponseEntity<ApiResponse<WeatherResponse>> getWeather(
            @PathVariable String cityCode,
            HttpServletResponse response) {
        
        long startTime = System.currentTimeMillis();
        
        // 取得當前 Trace 資訊
        Span currentSpan = Span.current();
        String traceId = currentSpan.getSpanContext().getTraceId();
        String spanId = currentSpan.getSpanContext().getSpanId();
        
        // 設定 Response Headers
        response.setHeader("X-Trace-Id", traceId);
        response.setHeader("X-Span-Id", spanId);
        
        log.info("Received weather request for city: {}, traceId: {}", cityCode, traceId);
        
        return weatherService.getWeather(cityCode.toUpperCase())
                .map(weather -> {
                    long duration = System.currentTimeMillis() - startTime;
                    
                    TraceInfo traceInfo = TraceInfo.builder()
                            .traceId(traceId)
                            .spanId(spanId)
                            .duration(duration)
                            .build();
                    
                    ApiResponse<WeatherResponse> apiResponse = ApiResponse.<WeatherResponse>builder()
                            .success(true)
                            .data(weather)
                            .traceInfo(traceInfo)
                            .build();
                    
                    return ResponseEntity.ok(apiResponse);
                })
                .orElseGet(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    
                    TraceInfo traceInfo = TraceInfo.builder()
                            .traceId(traceId)
                            .spanId(spanId)
                            .duration(duration)
                            .build();
                    
                    ApiResponse<WeatherResponse> errorResponse = ApiResponse.<WeatherResponse>builder()
                            .success(false)
                            .errorCode("CITY_NOT_FOUND")
                            .errorMessage("找不到指定的城市: " + cityCode)
                            .traceInfo(traceInfo)
                            .build();
                    
                    return ResponseEntity.status(404).body(errorResponse);
                });
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
```

---

## 4. 可觀測性設計（核心）

### 4.1 Trace Context 傳播機制

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        Trace Context Propagation                           │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│   Request Flow:                                                            │
│                                                                            │
│   ┌──────────┐      ┌──────────┐      ┌──────────┐      ┌──────────┐      │
│   │ Frontend │─────▶│ Gateway  │─────▶│ Weather  │─────▶│    H2    │      │
│   │          │      │          │      │ Service  │      │ Database │      │
│   └──────────┘      └──────────┘      └──────────┘      └──────────┘      │
│        │                 │                 │                 │             │
│        │                 │                 │                 │             │
│   ┌────▼────┐       ┌────▼────┐       ┌────▼────┐       ┌────▼────┐       │
│   │ Span A  │       │ Span B  │       │ Span C  │       │ Span D  │       │
│   │ (root)  │──────▶│(child A)│──────▶│(child B)│──────▶│(child C)│       │
│   └─────────┘       └─────────┘       └─────────┘       └─────────┘       │
│                                                                            │
│   Headers Propagated:                                                      │
│   ┌────────────────────────────────────────────────────────────────────┐  │
│   │ traceparent: 00-{traceId}-{spanId}-{flags}                         │  │
│   │ tracestate: (optional vendor-specific data)                        │  │
│   └────────────────────────────────────────────────────────────────────┘  │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Trace ID 與 Span ID 設計

| 識別碼 | 格式 | 長度 | 說明 |
|--------|------|------|------|
| Trace ID | 32 hex characters | 128-bit | 整個請求鏈路的唯一識別碼 |
| Span ID | 16 hex characters | 64-bit | 單一操作的識別碼 |
| Parent Span ID | 16 hex characters | 64-bit | 父 Span 識別碼 |

**範例：**
```
Trace ID: 4bf92f3577b34da6a3ce929d0e0e4736
├── Span ID: 00f067aa0ba902b7 (Gateway - root span)
│   └── Span ID: a2fb4a1d1a96d312 (Weather Service)
│       ├── Span ID: b3c4d5e6f7a8b9c0 (DB Query)
│       └── Span ID: c4d5e6f7a8b9c0d1 (Calculation)
```

### 4.3 Jaeger 追蹤視覺化

預期在 Jaeger UI 看到的 Span 結構：
```
weather-gateway: GET /api/weather/TPE                    [45ms]
└── weather-service: GET /weather/TPE                    [40ms]
    ├── WeatherService.getWeather                        [35ms]
    │   ├── WeatherService.fetchFromDatabase             [15ms]
    │   │   └── SELECT weather_data                      [10ms]  ← JDBC auto-instrumented
    │   └── calculateSimulatedWeather                    [18ms]
    └── (response processing)                            [2ms]
```

### 4.4 Prometheus Metrics 設計

#### 4.4.1 預設 Spring Boot Metrics
| Metric | Type | Description |
|--------|------|-------------|
| `http_server_requests_seconds` | Histogram | HTTP 請求延遲 |
| `jvm_memory_used_bytes` | Gauge | JVM 記憶體使用 |
| `hikaricp_connections_active` | Gauge | 活躍 DB 連線數 |
| `hikaricp_connections_pending` | Gauge | 等待中的連線請求 |

#### 4.4.2 自定義 Metrics
```java
@Configuration
public class CustomMetricsConfig {
    
    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
            .commonTags("application", "weather-service");
    }
}

// In WeatherService
@Autowired
private MeterRegistry meterRegistry;

// 計數器：查詢次數
Counter.builder("weather.queries.total")
    .tag("city", cityCode)
    .register(meterRegistry)
    .increment();

// 計時器：查詢耗時
Timer.builder("weather.query.duration")
    .tag("city", cityCode)
    .register(meterRegistry)
    .record(() -> performQuery());
```

### 4.5 Grafana Dashboard 設計

#### 4.5.1 儀表板面板
| Panel | Visualization | Query |
|-------|---------------|-------|
| Request Rate | Time Series | `rate(http_server_requests_seconds_count[1m])` |
| Error Rate | Stat | `sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))` |
| P95 Latency | Time Series | `histogram_quantile(0.95, http_server_requests_seconds_bucket)` |
| DB Pool Status | Gauge | `hikaricp_connections_active` |
| JVM Heap | Time Series | `jvm_memory_used_bytes{area="heap"}` |

---

## 5. API 介面規格

### 5.1 RESTful API 設計

#### Gateway API Routes
| Method | Path | Target | Description |
|--------|------|--------|-------------|
| GET | `/api/weather/{cityCode}` | Weather Service | 查詢城市天氣 |
| GET | `/actuator/health` | Gateway | Gateway 健康檢查 |
| GET | `/actuator/prometheus` | Gateway | Prometheus metrics |

#### Weather Service Internal API
| Method | Path | Description |
|--------|------|-------------|
| GET | `/weather/{cityCode}` | 查詢城市天氣 |
| GET | `/actuator/health` | 服務健康檢查 |
| GET | `/actuator/prometheus` | Prometheus metrics |

### 5.2 DTO 定義

```java
// ApiResponse.java
@Data
@Builder
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String errorCode;
    private String errorMessage;
    private TraceInfo traceInfo;
}

// WeatherResponse.java
@Data
@Builder
public class WeatherResponse {
    private String cityCode;
    private String cityName;
    private BigDecimal temperature;
    private BigDecimal rainfall;
    private LocalDateTime updatedAt;
}

// TraceInfo.java
@Data
@Builder
public class TraceInfo {
    private String traceId;
    private String spanId;
    private Long duration;
}
```

---

## 6. 錯誤處理

### 6.1 錯誤代碼定義

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `CITY_NOT_FOUND` | 404 | 城市代碼不存在 |
| `INVALID_CITY_CODE` | 400 | 城市代碼格式錯誤 |
| `SERVICE_UNAVAILABLE` | 503 | 服務暫時不可用 |
| `INTERNAL_ERROR` | 500 | 內部錯誤 |

### 6.2 Global Exception Handler

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        Span currentSpan = Span.current();
        String traceId = currentSpan.getSpanContext().getTraceId();
        
        // 記錄錯誤到 Span
        currentSpan.recordException(ex);
        currentSpan.setStatus(StatusCode.ERROR, ex.getMessage());
        
        log.error("Unhandled exception, traceId: {}", traceId, ex);
        
        return ResponseEntity.status(500)
            .body(ApiResponse.<Void>builder()
                .success(false)
                .errorCode("INTERNAL_ERROR")
                .errorMessage("內部錯誤，請稍後再試")
                .traceInfo(TraceInfo.builder()
                    .traceId(traceId)
                    .build())
                .build());
    }
}
```

---

## 7. 測試策略

### 7.1 測試類型

| Type | Scope | Tools |
|------|-------|-------|
| Unit Tests | Service Layer | JUnit 5, Mockito |
| Integration Tests | API Endpoints | Spring Test, TestContainers |
| E2E Tests | Full Flow | Playwright (Frontend) |
| Performance Tests | Load Testing | k6 |

### 7.2 測試用例

```java
@SpringBootTest
@AutoConfigureMockMvc
class WeatherControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldReturnWeatherForValidCity() throws Exception {
        mockMvc.perform(get("/weather/TPE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.cityCode").value("TPE"))
            .andExpect(jsonPath("$.traceInfo.traceId").exists());
    }
    
    @Test
    void shouldReturn404ForInvalidCity() throws Exception {
        mockMvc.perform(get("/weather/XXX"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errorCode").value("CITY_NOT_FOUND"));
    }
}
```

---

## 8. 版本歷史

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-01-20 | Architect | Initial version |
