# Quickstart Guide: Weather Tracing PoC

**Branch**: `001-weather-tracing-poc` | **Date**: 2026-01-21
**Status**: Complete

## Overview

This guide provides step-by-step instructions to run the Weather Tracing PoC system in all four deployment modes.

---

## Prerequisites

### Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| Java | 21 (LTS) | Backend services |
| Node.js | 20.x (LTS) | Frontend build |
| Docker | 24.x+ | Container runtime |
| Docker Compose | 2.x+ | Multi-container orchestration |
| Kind | 0.20+ | Local Kubernetes (Modes 3-4) |
| kubectl | 1.28+ | Kubernetes CLI |

### Verify Installation

```bash
java --version        # Should show 21.x
node --version        # Should show 20.x
docker --version      # Should show 24.x+
docker compose version
kind --version        # For Modes 3-4
kubectl version --client
```

---

## Mode 1: Local Development

Best for: Day-to-day development with IDE debugging support.

### Architecture
```
┌─────────────┐     ┌─────────────┐     ┌─────────────────┐
│   Frontend  │────▶│   Gateway   │────▶│ Weather Service │
│  localhost  │     │  localhost  │     │   localhost     │
│    :5173    │     │    :8080    │     │     :8081       │
└─────────────┘     └─────────────┘     └─────────────────┘
                                               │
                    ┌──────────────────────────┘
                    ▼
         ┌────────────────────────────────────────────┐
         │         Observability Stack (Docker)        │
         │  Jaeger:16686  Prometheus:9090  Grafana:3000│
         └────────────────────────────────────────────┘
```

### Steps

1. **Start observability stack**
   ```bash
   docker compose -f docker-compose.dev.yml up -d
   ```

2. **Start Weather Service**
   ```bash
   cd weather-service
   ./gradlew bootRun
   ```

3. **Start Gateway**
   ```bash
   cd gateway
   ./gradlew bootRun
   ```

4. **Start Frontend**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

### Access Points

| Service | URL |
|---------|-----|
| Frontend | http://localhost:5173 |
| Gateway API | http://localhost:8080/api |
| Weather Service | http://localhost:8081 |
| Jaeger UI | http://localhost:16686 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 |

### Verify

```bash
# Query weather via Gateway
curl http://localhost:8080/api/weather/TPE

# Expected response
{
  "success": true,
  "data": {
    "cityCode": "TPE",
    "cityName": "台北",
    "temperature": 25.5,
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

---

## Mode 2: Docker Compose Full Stack

Best for: Quick demos, integration testing, sharing with team.

### Architecture
```
┌─────────────────────────────────────────────────────┐
│                  Docker Network                      │
│                                                      │
│  ┌──────────┐   ┌─────────┐   ┌─────────────────┐  │
│  │ Frontend │──▶│ Gateway │──▶│ Weather Service │  │
│  │  :5173   │   │  :8080  │   │     :8081       │  │
│  └──────────┘   └─────────┘   └─────────────────┘  │
│                                       │             │
│  ┌────────────────────────────────────┘             │
│  ▼                                                  │
│  ┌──────────────────────────────────────────────┐  │
│  │       OTel Collector → Jaeger + Prometheus    │  │
│  │                    ↓                          │  │
│  │                 Grafana                       │  │
│  └──────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

### Steps

1. **Start all services**
   ```bash
   docker compose up -d --build
   ```

2. **Check service health**
   ```bash
   docker compose ps
   docker compose logs -f weather-service
   ```

### Access Points

| Service | URL |
|---------|-----|
| Frontend | http://localhost:5173 |
| Gateway API | http://localhost:8080/api |
| Jaeger UI | http://localhost:16686 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 |

### Verify

```bash
# Health check
curl http://localhost:8080/actuator/health

# Query weather
curl http://localhost:8080/api/weather/TXG
```

### Cleanup

```bash
docker compose down -v
```

---

## Mode 3: Kubernetes with Ingress

Best for: Demonstrating Kubernetes ingress routing patterns.

### Architecture
```
┌─────────────────────────────────────────────────────────┐
│                    Kind Cluster                          │
│                                                          │
│  ┌────────────────────────────────────────────────────┐ │
│  │              NGINX Ingress Controller               │ │
│  │                  localhost:80                       │ │
│  └────────────────────────────────────────────────────┘ │
│           │              │              │                │
│           ▼              ▼              ▼                │
│     /             /api/*          /jaeger/*              │
│  ┌────────┐    ┌─────────┐    ┌──────────────┐         │
│  │Frontend│    │ Gateway │    │    Jaeger    │         │
│  └────────┘    └─────────┘    └──────────────┘         │
│                     │                                    │
│                     ▼                                    │
│              ┌─────────────────┐                        │
│              │ Weather Service │                        │
│              └─────────────────┘                        │
└─────────────────────────────────────────────────────────┘
```

### Steps

1. **Create Kind cluster with ingress**
   ```bash
   kind create cluster --config k8s/kind/kind-config-ingress.yaml
   ```

2. **Install NGINX Ingress Controller**
   ```bash
   kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
   kubectl wait --namespace ingress-nginx \
     --for=condition=ready pod \
     --selector=app.kubernetes.io/component=controller \
     --timeout=90s
   ```

3. **Deploy application**
   ```bash
   kubectl apply -k k8s/overlays/ingress
   ```

4. **Wait for pods**
   ```bash
   kubectl -n weather-poc get pods -w
   ```

### Access Points

| Service | URL |
|---------|-----|
| Frontend | http://localhost/ |
| Gateway API | http://localhost/api |
| Jaeger UI | http://localhost/jaeger |
| Grafana | http://localhost/grafana |

### Verify

```bash
# Query weather via Ingress
curl http://localhost/api/weather/KHH
```

### Cleanup

```bash
kind delete cluster
```

---

## Mode 4: Kubernetes with LoadBalancer

Best for: Demonstrating production-like LoadBalancer services.

### Architecture
```
┌─────────────────────────────────────────────────────────┐
│                    Kind Cluster                          │
│                                                          │
│     External IPs (MetalLB)                              │
│     ┌─────────────────────────────────────────────┐    │
│     │ 172.18.255.1  172.18.255.2  172.18.255.3   │    │
│     └─────────────────────────────────────────────┘    │
│           │              │              │               │
│           ▼              ▼              ▼               │
│     ┌──────────┐  ┌─────────┐  ┌──────────────┐       │
│     │ Frontend │  │ Gateway │  │ Observability│       │
│     │   LB     │  │   LB    │  │     LB       │       │
│     └──────────┘  └─────────┘  └──────────────┘       │
│                        │                               │
│                        ▼                               │
│                 ┌─────────────────┐                   │
│                 │ Weather Service │                   │
│                 │   (ClusterIP)   │                   │
│                 └─────────────────┘                   │
└─────────────────────────────────────────────────────────┘
```

### Steps

1. **Create Kind cluster with LoadBalancer support**
   ```bash
   kind create cluster --config k8s/kind/kind-config-lb.yaml
   ```

2. **Install MetalLB**
   ```bash
   kubectl apply -f https://raw.githubusercontent.com/metallb/metallb/v0.13.12/config/manifests/metallb-native.yaml
   kubectl wait --namespace metallb-system \
     --for=condition=ready pod \
     --selector=app=metallb \
     --timeout=90s
   ```

3. **Configure MetalLB IP pool**
   ```bash
   kubectl apply -f k8s/kind/metallb-config.yaml
   ```

4. **Deploy application**
   ```bash
   kubectl apply -k k8s/overlays/loadbalancer
   ```

5. **Get external IPs**
   ```bash
   kubectl -n weather-poc get svc
   ```

### Access Points

After deployment, get IPs with `kubectl get svc -n weather-poc`:

| Service | URL |
|---------|-----|
| Frontend | http://<FRONTEND_EXTERNAL_IP>/ |
| Gateway API | http://<GATEWAY_EXTERNAL_IP>/api |
| Jaeger UI | http://<JAEGER_EXTERNAL_IP>:16686 |
| Grafana | http://<GRAFANA_EXTERNAL_IP>:3000 |

### Verify

```bash
# Get Gateway external IP
GATEWAY_IP=$(kubectl -n weather-poc get svc gateway -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

# Query weather
curl http://${GATEWAY_IP}/api/weather/TPE
```

### Cleanup

```bash
kind delete cluster
```

---

## Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| Port already in use | `lsof -i :PORT` and kill the process |
| Docker containers not starting | Check logs with `docker compose logs SERVICE` |
| Kubernetes pods in CrashLoopBackOff | `kubectl logs POD -n weather-poc` |
| Traces not appearing in Jaeger | Verify OTel Collector is running and receiving data |
| Metrics missing in Grafana | Check Prometheus targets at :9090/targets |

### Health Checks

```bash
# Gateway health
curl http://localhost:8080/actuator/health

# Weather Service health
curl http://localhost:8081/actuator/health

# Prometheus targets
curl http://localhost:9090/api/v1/targets
```

### Logs

```bash
# Docker Compose
docker compose logs -f weather-service

# Kubernetes
kubectl -n weather-poc logs -f deployment/weather-service
```

---

## Demo Script (SC-007: Complete within 10 minutes)

**Total estimated time**: 8-10 minutes

This timed demonstration script validates all three user stories and success criteria.

### Pre-Demo Checklist (1 minute)

- [ ] System running (Mode 1 or Mode 2)
- [ ] Browser open with 3 tabs ready:
  - Tab 1: Frontend (http://localhost:5173)
  - Tab 2: Jaeger UI (http://localhost:16686)
  - Tab 3: Grafana (http://localhost:3000)

---

### Part 1: Weather Query (US1) - 2 minutes

| Step | Action | Expected Result | Time |
|------|--------|-----------------|------|
| 1.1 | Open Frontend tab | See 3 city buttons: 台北, 台中, 高雄 | 10s |
| 1.2 | Click "台北" button | Loading indicator appears briefly | 5s |
| 1.3 | View weather result | Temperature (15-35°C) and rainfall (0-50mm) displayed | 5s |
| 1.4 | Note the temperature value | Record value (e.g., 26.5°C) | 5s |
| 1.5 | Click "台北" again | Different temperature/rainfall values appear | 10s |
| 1.6 | Click "台中" button | Taichung weather displayed (different baseline) | 10s |
| 1.7 | Click "高雄" button | Kaohsiung weather displayed (warmest baseline) | 10s |

**Validation**:
- [ ] All 3 cities return weather data
- [ ] Repeated queries show different values (FR-010)
- [ ] Response time < 200ms (check browser DevTools Network tab)

---

### Part 2: Trace Information (US2) - 3 minutes

| Step | Action | Expected Result | Time |
|------|--------|-----------------|------|
| 2.1 | View Trace ID on page | 32-character hex string displayed below weather | 10s |
| 2.2 | Note request duration | Duration in milliseconds shown (e.g., "45ms") | 5s |
| 2.3 | Click the Trace ID link | New tab opens to Jaeger UI with trace details | 15s |
| 2.4 | Examine trace timeline | See 4 spans in waterfall view | 30s |
| 2.5 | Verify span chain | Frontend → Gateway → Weather Service → H2 (JDBC) | 30s |
| 2.6 | Click on Weather Service span | See tags: cityCode, service.name, status | 20s |
| 2.7 | Return to Frontend, query again | New Trace ID generated | 10s |
| 2.8 | Click new Trace ID | Different trace with different timing | 20s |

**Validation**:
- [ ] Trace ID is clickable and links to Jaeger (FR-012)
- [ ] 4-hop trace chain visible (FR-016)
- [ ] Each span has operation name, timing, status (FR-017)

---

### Part 3: Metrics Dashboard (US3) - 3 minutes

| Step | Action | Expected Result | Time |
|------|--------|-----------------|------|
| 3.1 | Open Grafana tab | Dashboard loads (may need to navigate to Weather Service dashboard) | 20s |
| 3.2 | View QPS panel | Request count shows recent queries | 15s |
| 3.3 | View latency panel | P50, P95, P99 percentiles displayed | 15s |
| 3.4 | View error rate panel | Error rate at 0% (or shows recent errors) | 10s |
| 3.5 | Return to Frontend | Make 5 rapid queries (click cities quickly) | 30s |
| 3.6 | Return to Grafana | Wait up to 15 seconds for metrics update | 20s |
| 3.7 | Verify QPS increased | Request count reflects new queries | 10s |
| 3.8 | Use service filter dropdown | Filter to show only "weather-service" metrics | 20s |
| 3.9 | Verify filtered view | Only weather-service data shown | 10s |

**Validation**:
- [ ] QPS, latency percentiles, error rate visible (FR-018, FR-019, FR-020)
- [ ] Metrics update within 15 seconds (SC-004)
- [ ] Service filter works (FR-022)

---

### Part 4: Error Handling (Optional) - 1 minute

| Step | Action | Expected Result | Time |
|------|--------|-----------------|------|
| 4.1 | Open browser DevTools Network tab | Ready to see API calls | 10s |
| 4.2 | Manually call invalid city | `curl http://localhost:8080/api/weather/XXX` | 15s |
| 4.3 | View error response | 404 with "找不到指定的城市" message and Trace ID | 15s |
| 4.4 | Click error Trace ID in Jaeger | Trace shows error span with status code | 20s |

**Validation**:
- [ ] Error includes Chinese message (FR-004, FR-005)
- [ ] Error response still includes Trace ID (FR-011)

---

### Demo Complete Checklist

| Success Criterion | Validated |
|-------------------|-----------|
| SC-001: P95 < 200ms | [ ] Checked in DevTools |
| SC-002: 100% Trace ID display | [ ] Every query showed Trace ID |
| SC-003: 4-hop trace in Jaeger | [ ] Verified in Part 2 |
| SC-004: Metrics update < 15s | [ ] Verified in Part 3 |
| SC-009: Different values per query | [ ] Verified in Part 1 |

**Total time**: _______ minutes (target: < 10 minutes)
