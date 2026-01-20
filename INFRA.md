# Weather Tracing PoC - Infrastructure Specification (INFRA.md)

## 1. 部署模式總覽

本 PoC 支援四種部署模式，適應不同開發和展示場景：

| 模式 | 適用場景 | 啟動方式 | 複雜度 |
|------|----------|----------|--------|
| **Mode 1** | 本機開發 | 手動啟動應用 + Docker 觀測工具 | ⭐ |
| **Mode 2** | 快速展示/整合測試 | Docker Compose 一鍵啟動 | ⭐⭐ |
| **Mode 3** | K8s Ingress 展示 | Kind + Ingress Controller | ⭐⭐⭐ |
| **Mode 4** | K8s LoadBalancer 展示 | Kind + MetalLB | ⭐⭐⭐⭐ |

---

## 2. 共用元件配置

### 2.1 OpenTelemetry Collector 配置

```yaml
# observability/otel-collector/otel-collector-config.yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318
        cors:
          allowed_origins:
            - "http://localhost:*"
            - "http://127.0.0.1:*"

processors:
  batch:
    timeout: 1s
    send_batch_size: 1024
  
  memory_limiter:
    check_interval: 1s
    limit_mib: 512
    spike_limit_mib: 128

  # 增加服務資訊
  resource:
    attributes:
      - key: environment
        value: development
        action: upsert

exporters:
  # 輸出到 Jaeger
  otlp/jaeger:
    endpoint: jaeger:4317
    tls:
      insecure: true
  
  # 輸出到 Prometheus（透過 Remote Write）
  prometheus:
    endpoint: "0.0.0.0:8889"
    namespace: weather_poc
    const_labels:
      environment: development
  
  # Debug 輸出（開發用）
  debug:
    verbosity: detailed

extensions:
  health_check:
    endpoint: 0.0.0.0:13133
  
  zpages:
    endpoint: 0.0.0.0:55679

service:
  extensions: [health_check, zpages]
  pipelines:
    traces:
      receivers: [otlp]
      processors: [memory_limiter, batch, resource]
      exporters: [otlp/jaeger, debug]
    
    metrics:
      receivers: [otlp]
      processors: [memory_limiter, batch]
      exporters: [prometheus]
    
    logs:
      receivers: [otlp]
      processors: [memory_limiter, batch]
      exporters: [debug]
```

### 2.2 Prometheus 配置

```yaml
# observability/prometheus/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    monitor: 'weather-tracing-poc'

scrape_configs:
  # OpenTelemetry Collector metrics
  - job_name: 'otel-collector'
    static_configs:
      - targets: ['otel-collector:8889']
    metrics_path: /metrics

  # Spring Cloud Gateway
  - job_name: 'weather-gateway'
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['gateway:8080']
    # 開發模式使用
    # static_configs:
    #   - targets: ['host.docker.internal:8080']

  # Weather Service
  - job_name: 'weather-service'
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['weather-service:8081']
    # 開發模式使用
    # static_configs:
    #   - targets: ['host.docker.internal:8081']

  # Prometheus 自身監控
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
```

### 2.3 Grafana 配置

#### 2.3.1 資料來源配置
```yaml
# observability/grafana/provisioning/datasources/datasource.yml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: false
    jsonData:
      timeInterval: "15s"

  - name: Jaeger
    type: jaeger
    access: proxy
    url: http://jaeger:16686
    editable: false
```

#### 2.3.2 儀表板配置
```yaml
# observability/grafana/provisioning/dashboards/dashboard.yml
apiVersion: 1

providers:
  - name: 'Weather Tracing PoC'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    editable: true
    options:
      path: /etc/grafana/provisioning/dashboards
```

#### 2.3.3 儀表板 JSON（簡化版）
```json
// observability/grafana/provisioning/dashboards/weather-service.json
{
  "annotations": {
    "list": []
  },
  "title": "Weather Service Dashboard",
  "uid": "weather-service-dashboard",
  "version": 1,
  "panels": [
    {
      "title": "Request Rate (QPS)",
      "type": "timeseries",
      "gridPos": { "h": 8, "w": 12, "x": 0, "y": 0 },
      "targets": [
        {
          "expr": "sum(rate(http_server_requests_seconds_count{application=~\"weather.*\"}[1m])) by (application, uri)",
          "legendFormat": "{{application}} - {{uri}}"
        }
      ]
    },
    {
      "title": "Response Time P95",
      "type": "timeseries",
      "gridPos": { "h": 8, "w": 12, "x": 12, "y": 0 },
      "targets": [
        {
          "expr": "histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{application=~\"weather.*\"}[5m])) by (le, application))",
          "legendFormat": "{{application}} P95"
        }
      ]
    },
    {
      "title": "Error Rate",
      "type": "stat",
      "gridPos": { "h": 4, "w": 6, "x": 0, "y": 8 },
      "targets": [
        {
          "expr": "sum(rate(http_server_requests_seconds_count{status=~\"5..\", application=~\"weather.*\"}[5m])) / sum(rate(http_server_requests_seconds_count{application=~\"weather.*\"}[5m])) * 100",
          "legendFormat": "Error %"
        }
      ]
    },
    {
      "title": "JVM Heap Memory",
      "type": "timeseries",
      "gridPos": { "h": 8, "w": 12, "x": 0, "y": 12 },
      "targets": [
        {
          "expr": "jvm_memory_used_bytes{area=\"heap\", application=~\"weather.*\"} / 1024 / 1024",
          "legendFormat": "{{application}} Heap (MB)"
        }
      ]
    },
    {
      "title": "HikariCP Active Connections",
      "type": "gauge",
      "gridPos": { "h": 4, "w": 6, "x": 6, "y": 8 },
      "targets": [
        {
          "expr": "hikaricp_connections_active{application=\"weather-service\"}",
          "legendFormat": "Active"
        }
      ]
    }
  ],
  "time": {
    "from": "now-15m",
    "to": "now"
  },
  "refresh": "5s"
}
```

---

## 3. Mode 1: 本機開發模式

### 3.1 架構圖
```
┌─────────────────────────────────────────────────────────────────┐
│                         Host Machine                             │
│  ┌───────────────┐  ┌───────────────┐                           │
│  │    Vue.js     │  │    Gateway    │                           │
│  │  (npm run dev)│  │ (./gradlew    │                           │
│  │   :5173       │  │  bootRun)     │                           │
│  │               │  │   :8080       │                           │
│  └───────────────┘  └───────────────┘                           │
│          │                  │          ┌───────────────┐        │
│          │                  │          │Weather Service│        │
│          │                  │          │(./gradlew     │        │
│          │                  │          │ bootRun)      │        │
│          │                  │          │   :8081       │        │
│          │                  │          └───────────────┘        │
└──────────┼──────────────────┼────────────────────┼──────────────┘
           │                  │                    │
           │                  │    OTLP (4317/4318)│
           │                  │                    │
┌──────────▼──────────────────▼────────────────────▼──────────────┐
│                    Docker Containers                             │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              OpenTelemetry Collector :4317/:4318           │ │
│  └────────────────────────────────────────────────────────────┘ │
│           │                              │                       │
│  ┌────────▼───────┐  ┌──────────────────▼─────────┐             │
│  │   Jaeger       │  │     Prometheus              │             │
│  │   :16686       │  │       :9090                 │             │
│  └────────────────┘  └─────────────┬───────────────┘             │
│                                    │                             │
│                         ┌──────────▼───────────┐                 │
│                         │      Grafana         │                 │
│                         │       :3000          │                 │
│                         └──────────────────────┘                 │
└──────────────────────────────────────────────────────────────────┘
```

### 3.2 Docker Compose (開發模式 - 僅觀測工具)

```yaml
# docker-compose.dev.yml
version: '3.8'

services:
  # OpenTelemetry Collector
  otel-collector:
    image: otel/opentelemetry-collector-contrib:0.96.0
    container_name: otel-collector
    command: ["--config=/etc/otel-collector-config.yaml"]
    volumes:
      - ./observability/otel-collector/otel-collector-config-dev.yaml:/etc/otel-collector-config.yaml:ro
    ports:
      - "4317:4317"   # OTLP gRPC
      - "4318:4318"   # OTLP HTTP
      - "8889:8889"   # Prometheus metrics
      - "13133:13133" # Health check
    depends_on:
      - jaeger
    networks:
      - observability

  # Jaeger All-in-One
  jaeger:
    image: jaegertracing/all-in-one:1.54
    container_name: jaeger
    environment:
      - COLLECTOR_OTLP_ENABLED=true
    ports:
      - "16686:16686" # Jaeger UI
      - "14268:14268" # Jaeger collector HTTP
      - "14250:14250" # Jaeger collector gRPC
    networks:
      - observability

  # Prometheus
  prometheus:
    image: prom/prometheus:v2.50.1
    container_name: prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus-dev.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.enable-lifecycle'
    volumes:
      - ./observability/prometheus/prometheus-dev.yml:/etc/prometheus/prometheus-dev.yml:ro
      - prometheus-data:/prometheus
    ports:
      - "9090:9090"
    extra_hosts:
      - "host.docker.internal:host-gateway"
    networks:
      - observability

  # Grafana
  grafana:
    image: grafana/grafana:10.3.3
    container_name: grafana
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_USERS_ALLOW_SIGN_UP=false
    volumes:
      - ./observability/grafana/provisioning:/etc/grafana/provisioning:ro
      - grafana-data:/var/lib/grafana
    ports:
      - "3000:3000"
    depends_on:
      - prometheus
    networks:
      - observability

networks:
  observability:
    driver: bridge

volumes:
  prometheus-data:
  grafana-data:
```

### 3.3 開發模式 Prometheus 配置

```yaml
# observability/prometheus/prometheus-dev.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'otel-collector'
    static_configs:
      - targets: ['otel-collector:8889']

  - job_name: 'weather-gateway'
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['host.docker.internal:8080']

  - job_name: 'weather-service'
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['host.docker.internal:8081']

  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
```

### 3.4 開發模式 OTel Collector 配置

```yaml
# observability/otel-collector/otel-collector-config-dev.yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318
        cors:
          allowed_origins:
            - "*"

processors:
  batch:
    timeout: 1s
    send_batch_size: 512

exporters:
  otlp/jaeger:
    endpoint: jaeger:4317
    tls:
      insecure: true
  
  prometheus:
    endpoint: "0.0.0.0:8889"
    namespace: weather_poc

  debug:
    verbosity: detailed

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [otlp/jaeger, debug]
    
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [prometheus]
```

### 3.5 啟動腳本

```bash
#!/bin/bash
# scripts/local-dev.sh

set -e

echo "🚀 Starting Weather Tracing PoC - Local Development Mode"
echo "========================================================="

# 顏色定義
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 1. 啟動觀測工具
echo -e "${YELLOW}Step 1: Starting observability stack...${NC}"
docker-compose -f docker-compose.dev.yml up -d

# 等待服務就緒
echo "Waiting for services to be ready..."
sleep 10

# 2. 檢查服務狀態
echo -e "${YELLOW}Step 2: Checking service status...${NC}"
docker-compose -f docker-compose.dev.yml ps

# 3. 顯示啟動指令
echo ""
echo -e "${GREEN}✅ Observability stack is ready!${NC}"
echo ""
echo "========================================================="
echo "📋 Next Steps - Run these commands in separate terminals:"
echo "========================================================="
echo ""
echo "Terminal 1 - Start Gateway:"
echo "  cd gateway && ./gradlew bootRun"
echo ""
echo "Terminal 2 - Start Weather Service:"
echo "  cd weather-service && ./gradlew bootRun"
echo ""
echo "Terminal 3 - Start Frontend:"
echo "  cd frontend && npm install && npm run dev"
echo ""
echo "========================================================="
echo "🔗 Access URLs:"
echo "========================================================="
echo "  Frontend:    http://localhost:5173"
echo "  Gateway:     http://localhost:8080"
echo "  Jaeger UI:   http://localhost:16686"
echo "  Grafana:     http://localhost:3000 (admin/admin)"
echo "  Prometheus:  http://localhost:9090"
echo "========================================================="
```

---

## 4. Mode 2: Docker Compose 完整模式

### 4.1 架構圖
```
┌─────────────────────────────────────────────────────────────────┐
│                    Docker Compose Network                        │
│                                                                  │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐       │
│  │   Frontend    │  │    Gateway    │  │Weather Service│       │
│  │   (nginx)     │──│   :8080       │──│    :8081      │       │
│  │    :80        │  │               │  │               │       │
│  └───────────────┘  └───────────────┘  └───────┬───────┘       │
│                            │                    │                │
│                            │     OTLP           │                │
│                            ▼                    ▼                │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              OpenTelemetry Collector                       │ │
│  │                   :4317/:4318                              │ │
│  └────────────────────────────────────────────────────────────┘ │
│           │                              │                       │
│  ┌────────▼───────┐  ┌──────────────────▼─────────┐             │
│  │   Jaeger       │  │     Prometheus              │             │
│  │   :16686       │  │       :9090                 │             │
│  └────────────────┘  └─────────────┬───────────────┘             │
│                                    │                             │
│                         ┌──────────▼───────────┐                 │
│                         │      Grafana         │                 │
│                         │       :3000          │                 │
│                         └──────────────────────┘                 │
└──────────────────────────────────────────────────────────────────┘

Exposed Ports:
  - :80    → Frontend
  - :8080  → Gateway API
  - :16686 → Jaeger UI
  - :9090  → Prometheus
  - :3000  → Grafana
```

### 4.2 Docker Compose 配置

```yaml
# docker-compose.yml
version: '3.8'

services:
  # ============================================
  # Application Services
  # ============================================
  
  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: weather-frontend
    ports:
      - "80:80"
    depends_on:
      - gateway
    networks:
      - app-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:80"]
      interval: 30s
      timeout: 10s
      retries: 3

  gateway:
    build:
      context: ./gateway
      dockerfile: Dockerfile
    container_name: weather-gateway
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4318
      - OTEL_SERVICE_NAME=weather-gateway
      - OTEL_METRICS_EXPORTER=otlp
      - OTEL_LOGS_EXPORTER=otlp
      - WEATHER_SERVICE_URL=http://weather-service:8081
    depends_on:
      otel-collector:
        condition: service_healthy
      weather-service:
        condition: service_healthy
    networks:
      - app-network
      - observability
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5

  weather-service:
    build:
      context: ./weather-service
      dockerfile: Dockerfile
    container_name: weather-service
    ports:
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4318
      - OTEL_SERVICE_NAME=weather-service
      - OTEL_METRICS_EXPORTER=otlp
      - OTEL_LOGS_EXPORTER=otlp
    depends_on:
      otel-collector:
        condition: service_healthy
    networks:
      - app-network
      - observability
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5

  # ============================================
  # Observability Stack
  # ============================================

  otel-collector:
    image: otel/opentelemetry-collector-contrib:0.96.0
    container_name: otel-collector
    command: ["--config=/etc/otel-collector-config.yaml"]
    volumes:
      - ./observability/otel-collector/otel-collector-config.yaml:/etc/otel-collector-config.yaml:ro
    ports:
      - "4317:4317"   # OTLP gRPC
      - "4318:4318"   # OTLP HTTP
      - "8889:8889"   # Prometheus metrics
    depends_on:
      - jaeger
    networks:
      - observability
    healthcheck:
      test: ["CMD", "wget", "--spider", "-q", "http://localhost:13133"]
      interval: 10s
      timeout: 5s
      retries: 5

  jaeger:
    image: jaegertracing/all-in-one:1.54
    container_name: jaeger
    environment:
      - COLLECTOR_OTLP_ENABLED=true
    ports:
      - "16686:16686" # Jaeger UI
      - "14250:14250" # gRPC
    networks:
      - observability
    healthcheck:
      test: ["CMD", "wget", "--spider", "-q", "http://localhost:16686"]
      interval: 10s
      timeout: 5s
      retries: 5

  prometheus:
    image: prom/prometheus:v2.50.1
    container_name: prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.enable-lifecycle'
    volumes:
      - ./observability/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prometheus-data:/prometheus
    ports:
      - "9090:9090"
    networks:
      - observability
    healthcheck:
      test: ["CMD", "wget", "--spider", "-q", "http://localhost:9090/-/healthy"]
      interval: 10s
      timeout: 5s
      retries: 5

  grafana:
    image: grafana/grafana:10.3.3
    container_name: grafana
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_USERS_ALLOW_SIGN_UP=false
    volumes:
      - ./observability/grafana/provisioning:/etc/grafana/provisioning:ro
      - grafana-data:/var/lib/grafana
    ports:
      - "3000:3000"
    depends_on:
      - prometheus
      - jaeger
    networks:
      - observability
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:3000/api/health"]
      interval: 10s
      timeout: 5s
      retries: 5

networks:
  app-network:
    driver: bridge
  observability:
    driver: bridge

volumes:
  prometheus-data:
  grafana-data:
```

### 4.3 Frontend Dockerfile

```dockerfile
# frontend/Dockerfile
# Build stage
FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# Production stage
FROM nginx:alpine

# 複製 nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 複製建置結果
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

### 4.4 Frontend Nginx 配置

```nginx
# frontend/nginx.conf
server {
    listen 80;
    server_name localhost;
    
    root /usr/share/nginx/html;
    index index.html;

    # Gzip 壓縮
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;

    # SPA 路由支援
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 代理
    location /api/ {
        proxy_pass http://gateway:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 傳遞 Trace Headers
        proxy_pass_header X-Trace-Id;
        proxy_pass_header X-Span-Id;
    }

    # 健康檢查
    location /health {
        return 200 'OK';
        add_header Content-Type text/plain;
    }
}
```

### 4.5 Backend Dockerfile

```dockerfile
# gateway/Dockerfile 和 weather-service/Dockerfile（相似結構）
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# 複製 Gradle 配置
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# 下載依賴
RUN ./gradlew dependencies --no-daemon

# 複製原始碼並建置
COPY src ./src
RUN ./gradlew bootJar --no-daemon

# Production stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 安裝 curl（用於健康檢查）
RUN apk add --no-cache curl

# 複製 JAR
COPY --from=builder /app/build/libs/*.jar app.jar

# JVM 調優參數
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 4.6 Docker Compose 啟動腳本

```bash
#!/bin/bash
# scripts/docker-compose-up.sh

set -e

echo "🚀 Starting Weather Tracing PoC - Docker Compose Mode"
echo "======================================================"

# 顏色定義
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# 清理舊的容器
echo -e "${YELLOW}Step 1: Cleaning up old containers...${NC}"
docker-compose down -v --remove-orphans 2>/dev/null || true

# 建置映像
echo -e "${YELLOW}Step 2: Building images...${NC}"
docker-compose build --parallel

# 啟動服務
echo -e "${YELLOW}Step 3: Starting services...${NC}"
docker-compose up -d

# 等待服務就緒
echo -e "${YELLOW}Step 4: Waiting for services to be healthy...${NC}"
echo "This may take 1-2 minutes..."

# 檢查各服務狀態
services=("otel-collector" "jaeger" "prometheus" "grafana" "weather-service" "gateway" "frontend")
for service in "${services[@]}"; do
    echo -n "  Waiting for $service..."
    timeout=60
    while [ $timeout -gt 0 ]; do
        if docker-compose ps | grep "$service" | grep -q "healthy\|Up"; then
            echo -e " ${GREEN}✓${NC}"
            break
        fi
        sleep 2
        timeout=$((timeout - 2))
    done
    if [ $timeout -le 0 ]; then
        echo -e " ${RED}✗ (timeout)${NC}"
    fi
done

# 顯示服務狀態
echo ""
echo -e "${YELLOW}Service Status:${NC}"
docker-compose ps

echo ""
echo -e "${GREEN}✅ All services are up!${NC}"
echo ""
echo "========================================================="
echo "🔗 Access URLs:"
echo "========================================================="
echo "  Frontend:    http://localhost"
echo "  Gateway:     http://localhost:8080"
echo "  Jaeger UI:   http://localhost:16686"
echo "  Grafana:     http://localhost:3000 (admin/admin)"
echo "  Prometheus:  http://localhost:9090"
echo "========================================================="
echo ""
echo "📋 Useful commands:"
echo "  View logs:     docker-compose logs -f [service]"
echo "  Stop all:      docker-compose down"
echo "  Restart:       docker-compose restart [service]"
echo "========================================================="
```

---

## 5. Mode 3: Kubernetes with Ingress

### 5.1 架構圖
```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Kind Kubernetes Cluster                           │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                     NGINX Ingress Controller                           │ │
│  │                        (NodePort: 80, 443)                             │ │
│  └───────────────────────────────┬────────────────────────────────────────┘ │
│                                  │                                           │
│         ┌────────────────────────┼────────────────────────┐                 │
│         │                        │                        │                  │
│         ▼                        ▼                        ▼                  │
│  ┌─────────────┐    ┌─────────────────┐    ┌─────────────────────────┐     │
│  │  /          │    │  /api/*         │    │  /jaeger, /grafana      │     │
│  │  Frontend   │    │  Gateway        │    │  Observability          │     │
│  │  Service    │    │  Service        │    │  Services               │     │
│  └──────┬──────┘    └────────┬────────┘    └───────────┬─────────────┘     │
│         │                    │                         │                    │
│  ┌──────▼──────┐    ┌────────▼────────┐    ┌──────────▼──────────┐        │
│  │  Frontend   │    │    Gateway      │    │    Jaeger/Grafana   │        │
│  │    Pod      │    │      Pod        │    │       Pods          │        │
│  └─────────────┘    └────────┬────────┘    └─────────────────────┘        │
│                              │                                              │
│                     ┌────────▼────────┐                                    │
│                     │ Weather Service │                                    │
│                     │    Service      │                                    │
│                     └────────┬────────┘                                    │
│                              │                                              │
│                     ┌────────▼────────┐                                    │
│                     │ Weather Service │                                    │
│                     │      Pod        │                                    │
│                     └─────────────────┘                                    │
│                                                                              │
│  Namespace: weather-tracing                                                 │
└─────────────────────────────────────────────────────────────────────────────┘

Access via localhost:
  http://localhost/           → Frontend
  http://localhost/api/       → Gateway
  http://localhost/jaeger/    → Jaeger UI
  http://localhost/grafana/   → Grafana
  http://localhost/prometheus/→ Prometheus
```

### 5.2 Kind 配置

```yaml
# k8s/kind/kind-config-ingress.yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
name: weather-tracing
nodes:
  - role: control-plane
    kubeadmConfigPatches:
      - |
        kind: InitConfiguration
        nodeRegistration:
          kubeletExtraArgs:
            node-labels: "ingress-ready=true"
    extraPortMappings:
      - containerPort: 80
        hostPort: 80
        protocol: TCP
      - containerPort: 443
        hostPort: 443
        protocol: TCP
  - role: worker
  - role: worker
```

### 5.3 Kubernetes 資源配置

#### 5.3.1 Namespace
```yaml
# k8s/base/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: weather-tracing
  labels:
    app.kubernetes.io/name: weather-tracing
    app.kubernetes.io/component: namespace
```

#### 5.3.2 ConfigMap
```yaml
# k8s/base/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: weather-config
  namespace: weather-tracing
data:
  OTEL_EXPORTER_OTLP_ENDPOINT: "http://otel-collector:4318"
  OTEL_METRICS_EXPORTER: "otlp"
  OTEL_LOGS_EXPORTER: "otlp"
  SPRING_PROFILES_ACTIVE: "kubernetes"
```

#### 5.3.3 Gateway Deployment
```yaml
# k8s/base/gateway/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gateway
  namespace: weather-tracing
  labels:
    app: gateway
spec:
  replicas: 2
  selector:
    matchLabels:
      app: gateway
  template:
    metadata:
      labels:
        app: gateway
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      containers:
        - name: gateway
          image: weather-gateway:latest
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8080
          envFrom:
            - configMapRef:
                name: weather-config
          env:
            - name: OTEL_SERVICE_NAME
              value: "weather-gateway"
            - name: WEATHER_SERVICE_URL
              value: "http://weather-service:8081"
          resources:
            requests:
              memory: "256Mi"
              cpu: "200m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: gateway
  namespace: weather-tracing
spec:
  selector:
    app: gateway
  ports:
    - port: 8080
      targetPort: 8080
  type: ClusterIP
```

#### 5.3.4 Weather Service Deployment
```yaml
# k8s/base/weather-service/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: weather-service
  namespace: weather-tracing
  labels:
    app: weather-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: weather-service
  template:
    metadata:
      labels:
        app: weather-service
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8081"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      containers:
        - name: weather-service
          image: weather-service:latest
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8081
          envFrom:
            - configMapRef:
                name: weather-config
          env:
            - name: OTEL_SERVICE_NAME
              value: "weather-service"
          resources:
            requests:
              memory: "256Mi"
              cpu: "200m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8081
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8081
            initialDelaySeconds: 10
            periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: weather-service
  namespace: weather-tracing
spec:
  selector:
    app: weather-service
  ports:
    - port: 8081
      targetPort: 8081
  type: ClusterIP
```

#### 5.3.5 Frontend Deployment
```yaml
# k8s/base/frontend/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend
  namespace: weather-tracing
  labels:
    app: frontend
spec:
  replicas: 2
  selector:
    matchLabels:
      app: frontend
  template:
    metadata:
      labels:
        app: frontend
    spec:
      containers:
        - name: frontend
          image: weather-frontend:latest
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 80
          resources:
            requests:
              memory: "64Mi"
              cpu: "50m"
            limits:
              memory: "128Mi"
              cpu: "100m"
          livenessProbe:
            httpGet:
              path: /health
              port: 80
            initialDelaySeconds: 10
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /health
              port: 80
            initialDelaySeconds: 5
            periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: frontend
  namespace: weather-tracing
spec:
  selector:
    app: frontend
  ports:
    - port: 80
      targetPort: 80
  type: ClusterIP
```

#### 5.3.6 Observability Stack
```yaml
# k8s/base/observability/otel-collector.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: otel-collector
  namespace: weather-tracing
spec:
  replicas: 1
  selector:
    matchLabels:
      app: otel-collector
  template:
    metadata:
      labels:
        app: otel-collector
    spec:
      containers:
        - name: otel-collector
          image: otel/opentelemetry-collector-contrib:0.96.0
          args: ["--config=/etc/otel-collector-config.yaml"]
          ports:
            - containerPort: 4317  # OTLP gRPC
            - containerPort: 4318  # OTLP HTTP
            - containerPort: 8889  # Prometheus metrics
          volumeMounts:
            - name: config
              mountPath: /etc/otel-collector-config.yaml
              subPath: otel-collector-config.yaml
      volumes:
        - name: config
          configMap:
            name: otel-collector-config
---
apiVersion: v1
kind: Service
metadata:
  name: otel-collector
  namespace: weather-tracing
spec:
  selector:
    app: otel-collector
  ports:
    - name: otlp-grpc
      port: 4317
      targetPort: 4317
    - name: otlp-http
      port: 4318
      targetPort: 4318
    - name: prometheus
      port: 8889
      targetPort: 8889
  type: ClusterIP
```

```yaml
# k8s/base/observability/jaeger.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: jaeger
  namespace: weather-tracing
spec:
  replicas: 1
  selector:
    matchLabels:
      app: jaeger
  template:
    metadata:
      labels:
        app: jaeger
    spec:
      containers:
        - name: jaeger
          image: jaegertracing/all-in-one:1.54
          env:
            - name: COLLECTOR_OTLP_ENABLED
              value: "true"
          ports:
            - containerPort: 16686  # UI
            - containerPort: 4317   # OTLP gRPC
            - containerPort: 14250  # gRPC
---
apiVersion: v1
kind: Service
metadata:
  name: jaeger
  namespace: weather-tracing
spec:
  selector:
    app: jaeger
  ports:
    - name: ui
      port: 16686
      targetPort: 16686
    - name: otlp-grpc
      port: 4317
      targetPort: 4317
  type: ClusterIP
```

#### 5.3.7 Ingress 配置
```yaml
# k8s/overlays/ingress/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: weather-ingress
  namespace: weather-tracing
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /$2
    nginx.ingress.kubernetes.io/use-regex: "true"
spec:
  ingressClassName: nginx
  rules:
    - http:
        paths:
          # Frontend (根路徑)
          - path: /()(.*)
            pathType: ImplementationSpecific
            backend:
              service:
                name: frontend
                port:
                  number: 80
          
          # Gateway API
          - path: /api(/|$)(.*)
            pathType: ImplementationSpecific
            backend:
              service:
                name: gateway
                port:
                  number: 8080
          
          # Jaeger UI
          - path: /jaeger(/|$)(.*)
            pathType: ImplementationSpecific
            backend:
              service:
                name: jaeger
                port:
                  number: 16686
          
          # Grafana
          - path: /grafana(/|$)(.*)
            pathType: ImplementationSpecific
            backend:
              service:
                name: grafana
                port:
                  number: 3000
          
          # Prometheus
          - path: /prometheus(/|$)(.*)
            pathType: ImplementationSpecific
            backend:
              service:
                name: prometheus
                port:
                  number: 9090
```

### 5.4 部署腳本

```bash
#!/bin/bash
# scripts/k8s-deploy-ingress.sh

set -e

echo "🚀 Deploying Weather Tracing PoC to Kind (Ingress Mode)"
echo "========================================================"

# 顏色定義
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

CLUSTER_NAME="weather-tracing"

# 1. 檢查先決條件
echo -e "${YELLOW}Step 1: Checking prerequisites...${NC}"
command -v kind >/dev/null 2>&1 || { echo -e "${RED}kind is required but not installed.${NC}"; exit 1; }
command -v kubectl >/dev/null 2>&1 || { echo -e "${RED}kubectl is required but not installed.${NC}"; exit 1; }
command -v docker >/dev/null 2>&1 || { echo -e "${RED}docker is required but not installed.${NC}"; exit 1; }
echo -e "${GREEN}✓ All prerequisites met${NC}"

# 2. 刪除現有叢集（如果存在）
echo -e "${YELLOW}Step 2: Cleaning up existing cluster...${NC}"
kind delete cluster --name $CLUSTER_NAME 2>/dev/null || true

# 3. 建立 Kind 叢集
echo -e "${YELLOW}Step 3: Creating Kind cluster...${NC}"
kind create cluster --config k8s/kind/kind-config-ingress.yaml
echo -e "${GREEN}✓ Kind cluster created${NC}"

# 4. 建置並載入映像到 Kind
echo -e "${YELLOW}Step 4: Building and loading images...${NC}"
docker build -t weather-frontend:latest ./frontend
docker build -t weather-gateway:latest ./gateway
docker build -t weather-service:latest ./weather-service

kind load docker-image weather-frontend:latest --name $CLUSTER_NAME
kind load docker-image weather-gateway:latest --name $CLUSTER_NAME
kind load docker-image weather-service:latest --name $CLUSTER_NAME
echo -e "${GREEN}✓ Images loaded into Kind${NC}"

# 5. 安裝 NGINX Ingress Controller
echo -e "${YELLOW}Step 5: Installing NGINX Ingress Controller...${NC}"
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml

echo "Waiting for Ingress Controller to be ready..."
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=90s
echo -e "${GREEN}✓ Ingress Controller ready${NC}"

# 6. 部署應用程式
echo -e "${YELLOW}Step 6: Deploying application...${NC}"
kubectl apply -f k8s/base/namespace.yaml
kubectl apply -f k8s/base/configmap.yaml
kubectl apply -f k8s/base/observability/
kubectl apply -f k8s/base/weather-service/
kubectl apply -f k8s/base/gateway/
kubectl apply -f k8s/base/frontend/
kubectl apply -f k8s/overlays/ingress/

# 7. 等待所有 Pod 就緒
echo -e "${YELLOW}Step 7: Waiting for all pods to be ready...${NC}"
kubectl wait --namespace weather-tracing \
  --for=condition=ready pod \
  --all \
  --timeout=180s
echo -e "${GREEN}✓ All pods ready${NC}"

# 8. 顯示部署狀態
echo ""
echo -e "${YELLOW}Deployment Status:${NC}"
kubectl get pods -n weather-tracing
echo ""
kubectl get services -n weather-tracing
echo ""
kubectl get ingress -n weather-tracing

echo ""
echo -e "${GREEN}✅ Deployment complete!${NC}"
echo ""
echo "========================================================="
echo "🔗 Access URLs (via Ingress):"
echo "========================================================="
echo "  Frontend:    http://localhost/"
echo "  Gateway API: http://localhost/api/weather/TPE"
echo "  Jaeger UI:   http://localhost/jaeger/"
echo "  Grafana:     http://localhost/grafana/ (admin/admin)"
echo "  Prometheus:  http://localhost/prometheus/"
echo "========================================================="
echo ""
echo "📋 Useful commands:"
echo "  View pods:     kubectl get pods -n weather-tracing"
echo "  View logs:     kubectl logs -n weather-tracing -l app=gateway"
echo "  Delete:        kind delete cluster --name $CLUSTER_NAME"
echo "========================================================="
```

---

## 6. Mode 4: Kubernetes with LoadBalancer

### 6.1 架構圖
```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Kind Kubernetes Cluster                           │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         MetalLB (L2 Mode)                              │ │
│  │                    IP Pool: 172.18.255.200-250                         │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│    ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐           │
│    │ Frontend LB     │  │ Gateway LB      │  │ Observability   │           │
│    │ 172.18.255.200  │  │ 172.18.255.201  │  │ Services LB     │           │
│    │   :80           │  │   :8080         │  │                 │           │
│    └────────┬────────┘  └────────┬────────┘  └────────┬────────┘           │
│             │                    │                    │                     │
│    ┌────────▼────────┐  ┌────────▼────────┐  ┌───────▼─────────┐           │
│    │   Frontend      │  │    Gateway      │  │ Jaeger:16686    │           │
│    │   Deployment    │  │   Deployment    │  │ Grafana:3000    │           │
│    │   (2 replicas)  │  │   (2 replicas)  │  │ Prometheus:9090 │           │
│    └─────────────────┘  └────────┬────────┘  └─────────────────┘           │
│                                  │                                          │
│                         ┌────────▼────────┐                                │
│                         │ Weather Service │                                │
│                         │   Deployment    │                                │
│                         │  (2 replicas)   │                                │
│                         └─────────────────┘                                │
│                                                                              │
│  Namespace: weather-tracing                                                 │
└─────────────────────────────────────────────────────────────────────────────┘

Access via LoadBalancer IPs:
  http://172.18.255.200/           → Frontend
  http://172.18.255.201/api/       → Gateway
  http://172.18.255.202:16686/     → Jaeger UI
  http://172.18.255.203:3000/      → Grafana
```

### 6.2 Kind 配置 (LoadBalancer)

```yaml
# k8s/kind/kind-config-lb.yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
name: weather-tracing-lb
nodes:
  - role: control-plane
  - role: worker
  - role: worker
networking:
  # 確保可以存取 LoadBalancer IP
  disableDefaultCNI: false
```

### 6.3 MetalLB 配置

```yaml
# k8s/overlays/loadbalancer/metallb-config.yaml
apiVersion: metallb.io/v1beta1
kind: IPAddressPool
metadata:
  name: default-pool
  namespace: metallb-system
spec:
  addresses:
    - 172.18.255.200-172.18.255.250
---
apiVersion: metallb.io/v1beta1
kind: L2Advertisement
metadata:
  name: default
  namespace: metallb-system
spec:
  ipAddressPools:
    - default-pool
```

### 6.4 LoadBalancer Services

```yaml
# k8s/overlays/loadbalancer/services.yaml
apiVersion: v1
kind: Service
metadata:
  name: frontend-lb
  namespace: weather-tracing
spec:
  type: LoadBalancer
  selector:
    app: frontend
  ports:
    - port: 80
      targetPort: 80
---
apiVersion: v1
kind: Service
metadata:
  name: gateway-lb
  namespace: weather-tracing
spec:
  type: LoadBalancer
  selector:
    app: gateway
  ports:
    - port: 8080
      targetPort: 8080
---
apiVersion: v1
kind: Service
metadata:
  name: jaeger-lb
  namespace: weather-tracing
spec:
  type: LoadBalancer
  selector:
    app: jaeger
  ports:
    - port: 16686
      targetPort: 16686
---
apiVersion: v1
kind: Service
metadata:
  name: grafana-lb
  namespace: weather-tracing
spec:
  type: LoadBalancer
  selector:
    app: grafana
  ports:
    - port: 3000
      targetPort: 3000
---
apiVersion: v1
kind: Service
metadata:
  name: prometheus-lb
  namespace: weather-tracing
spec:
  type: LoadBalancer
  selector:
    app: prometheus
  ports:
    - port: 9090
      targetPort: 9090
```

### 6.5 部署腳本

```bash
#!/bin/bash
# scripts/k8s-deploy-lb.sh

set -e

echo "🚀 Deploying Weather Tracing PoC to Kind (LoadBalancer Mode)"
echo "============================================================="

# 顏色定義
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

CLUSTER_NAME="weather-tracing-lb"

# 1. 檢查先決條件
echo -e "${YELLOW}Step 1: Checking prerequisites...${NC}"
command -v kind >/dev/null 2>&1 || { echo -e "${RED}kind is required but not installed.${NC}"; exit 1; }
command -v kubectl >/dev/null 2>&1 || { echo -e "${RED}kubectl is required but not installed.${NC}"; exit 1; }
command -v docker >/dev/null 2>&1 || { echo -e "${RED}docker is required but not installed.${NC}"; exit 1; }
echo -e "${GREEN}✓ All prerequisites met${NC}"

# 2. 刪除現有叢集（如果存在）
echo -e "${YELLOW}Step 2: Cleaning up existing cluster...${NC}"
kind delete cluster --name $CLUSTER_NAME 2>/dev/null || true

# 3. 建立 Kind 叢集
echo -e "${YELLOW}Step 3: Creating Kind cluster...${NC}"
kind create cluster --config k8s/kind/kind-config-lb.yaml
echo -e "${GREEN}✓ Kind cluster created${NC}"

# 4. 取得 Docker 網路 CIDR（用於 MetalLB）
echo -e "${YELLOW}Step 4: Configuring MetalLB IP range...${NC}"
DOCKER_NETWORK=$(docker network inspect kind -f '{{(index .IPAM.Config 0).Subnet}}')
echo "Docker network CIDR: $DOCKER_NETWORK"

# 計算 MetalLB IP 範圍（使用 .255.200-.255.250）
BASE_IP=$(echo $DOCKER_NETWORK | sed 's/\.[0-9]*\/[0-9]*//')
METALLB_START="${BASE_IP}.255.200"
METALLB_END="${BASE_IP}.255.250"
echo "MetalLB IP range: $METALLB_START - $METALLB_END"

# 5. 建置並載入映像到 Kind
echo -e "${YELLOW}Step 5: Building and loading images...${NC}"
docker build -t weather-frontend:latest ./frontend
docker build -t weather-gateway:latest ./gateway
docker build -t weather-service:latest ./weather-service

kind load docker-image weather-frontend:latest --name $CLUSTER_NAME
kind load docker-image weather-gateway:latest --name $CLUSTER_NAME
kind load docker-image weather-service:latest --name $CLUSTER_NAME
echo -e "${GREEN}✓ Images loaded into Kind${NC}"

# 6. 安裝 MetalLB
echo -e "${YELLOW}Step 6: Installing MetalLB...${NC}"
kubectl apply -f https://raw.githubusercontent.com/metallb/metallb/v0.14.3/config/manifests/metallb-native.yaml

echo "Waiting for MetalLB to be ready..."
kubectl wait --namespace metallb-system \
  --for=condition=ready pod \
  --selector=app=metallb \
  --timeout=120s

# 配置 MetalLB IP Pool（動態生成配置）
cat <<EOF | kubectl apply -f -
apiVersion: metallb.io/v1beta1
kind: IPAddressPool
metadata:
  name: default-pool
  namespace: metallb-system
spec:
  addresses:
    - ${METALLB_START}-${METALLB_END}
---
apiVersion: metallb.io/v1beta1
kind: L2Advertisement
metadata:
  name: default
  namespace: metallb-system
spec:
  ipAddressPools:
    - default-pool
EOF
echo -e "${GREEN}✓ MetalLB configured${NC}"

# 7. 部署應用程式
echo -e "${YELLOW}Step 7: Deploying application...${NC}"
kubectl apply -f k8s/base/namespace.yaml
kubectl apply -f k8s/base/configmap.yaml
kubectl apply -f k8s/base/observability/
kubectl apply -f k8s/base/weather-service/
kubectl apply -f k8s/base/gateway/
kubectl apply -f k8s/base/frontend/
kubectl apply -f k8s/overlays/loadbalancer/services.yaml

# 8. 等待所有 Pod 就緒
echo -e "${YELLOW}Step 8: Waiting for all pods to be ready...${NC}"
kubectl wait --namespace weather-tracing \
  --for=condition=ready pod \
  --all \
  --timeout=180s
echo -e "${GREEN}✓ All pods ready${NC}"

# 9. 等待 LoadBalancer IP 分配
echo -e "${YELLOW}Step 9: Waiting for LoadBalancer IPs...${NC}"
sleep 10

# 取得各服務的 External IP
FRONTEND_IP=$(kubectl get svc frontend-lb -n weather-tracing -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
GATEWAY_IP=$(kubectl get svc gateway-lb -n weather-tracing -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
JAEGER_IP=$(kubectl get svc jaeger-lb -n weather-tracing -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
GRAFANA_IP=$(kubectl get svc grafana-lb -n weather-tracing -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
PROMETHEUS_IP=$(kubectl get svc prometheus-lb -n weather-tracing -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

# 10. 顯示部署狀態
echo ""
echo -e "${YELLOW}Deployment Status:${NC}"
kubectl get pods -n weather-tracing
echo ""
kubectl get services -n weather-tracing
echo ""

echo -e "${GREEN}✅ Deployment complete!${NC}"
echo ""
echo "========================================================="
echo "🔗 Access URLs (via LoadBalancer):"
echo "========================================================="
echo "  Frontend:    http://${FRONTEND_IP}/"
echo "  Gateway API: http://${GATEWAY_IP}:8080/api/weather/TPE"
echo "  Jaeger UI:   http://${JAEGER_IP}:16686/"
echo "  Grafana:     http://${GRAFANA_IP}:3000/ (admin/admin)"
echo "  Prometheus:  http://${PROMETHEUS_IP}:9090/"
echo "========================================================="
echo ""
echo "📋 Useful commands:"
echo "  View pods:     kubectl get pods -n weather-tracing"
echo "  View services: kubectl get svc -n weather-tracing"
echo "  View logs:     kubectl logs -n weather-tracing -l app=gateway"
echo "  Delete:        kind delete cluster --name $CLUSTER_NAME"
echo "========================================================="
```

---

## 7. 清理腳本

```bash
#!/bin/bash
# scripts/cleanup.sh

set -e

echo "🧹 Cleaning up Weather Tracing PoC"
echo "==================================="

# 顏色定義
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}Stopping Docker Compose...${NC}"
docker-compose down -v --remove-orphans 2>/dev/null || true
docker-compose -f docker-compose.dev.yml down -v --remove-orphans 2>/dev/null || true

echo -e "${YELLOW}Deleting Kind clusters...${NC}"
kind delete cluster --name weather-tracing 2>/dev/null || true
kind delete cluster --name weather-tracing-lb 2>/dev/null || true

echo -e "${YELLOW}Removing Docker images...${NC}"
docker rmi weather-frontend:latest 2>/dev/null || true
docker rmi weather-gateway:latest 2>/dev/null || true
docker rmi weather-service:latest 2>/dev/null || true

echo -e "${YELLOW}Pruning Docker system...${NC}"
docker system prune -f

echo ""
echo -e "${GREEN}✅ Cleanup complete!${NC}"
```

---

## 8. 端口和網路對照表

### 8.1 應用服務端口

| Service | Internal Port | Mode 1 | Mode 2 | Mode 3 (Ingress) | Mode 4 (LB) |
|---------|--------------|--------|--------|------------------|-------------|
| Frontend | 80/5173 | localhost:5173 | localhost:80 | localhost/ | LB_IP:80 |
| Gateway | 8080 | localhost:8080 | localhost:8080 | localhost/api/ | LB_IP:8080 |
| Weather Service | 8081 | localhost:8081 | (internal) | (internal) | (internal) |

### 8.2 可觀測性服務端口

| Service | Internal Port | Mode 1 | Mode 2 | Mode 3 (Ingress) | Mode 4 (LB) |
|---------|--------------|--------|--------|------------------|-------------|
| Jaeger UI | 16686 | localhost:16686 | localhost:16686 | localhost/jaeger/ | LB_IP:16686 |
| Prometheus | 9090 | localhost:9090 | localhost:9090 | localhost/prometheus/ | LB_IP:9090 |
| Grafana | 3000 | localhost:3000 | localhost:3000 | localhost/grafana/ | LB_IP:3000 |
| OTel Collector (gRPC) | 4317 | localhost:4317 | localhost:4317 | (internal) | (internal) |
| OTel Collector (HTTP) | 4318 | localhost:4318 | localhost:4318 | (internal) | (internal) |

---

## 9. 故障排除

### 9.1 常見問題

| 問題 | 原因 | 解決方案 |
|------|------|----------|
| Trace 未出現在 Jaeger | OTel Collector 連線失敗 | 檢查 OTEL_EXPORTER_OTLP_ENDPOINT 設定 |
| Metrics 未出現在 Grafana | Prometheus scrape 失敗 | 檢查 actuator endpoint 是否啟用 |
| Kind LoadBalancer pending | MetalLB 未正確配置 | 重新執行 MetalLB 配置步驟 |
| 跨服務 Trace 斷裂 | Context propagation 失敗 | 確認 W3C Trace Context headers 傳遞 |

### 9.2 診斷命令

```bash
# 檢查 OTel Collector 狀態
curl http://localhost:13133/health

# 檢查 Prometheus targets
curl http://localhost:9090/api/v1/targets

# 檢查 Jaeger 是否收到 traces
curl http://localhost:16686/api/services

# 檢查 K8s Pod 日誌
kubectl logs -n weather-tracing -l app=gateway --tail=100

# 檢查 K8s 事件
kubectl get events -n weather-tracing --sort-by='.lastTimestamp'
```

---

## 10. 版本歷史

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-01-20 | Architect | Initial version |
