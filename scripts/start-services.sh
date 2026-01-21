#!/bin/bash
#
# Weather Tracing PoC - 本地開發服務啟動腳本
# 一鍵啟動所有本地開發服務
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# PID 檔案位置
PID_DIR="/tmp/weather-tracing"

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step() { echo -e "${CYAN}[STEP]${NC} $1"; }

# 初始化 PID 目錄
init_pid_dir() {
    mkdir -p "$PID_DIR"
}

# 檢查 Docker 是否運行
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        log_error "Docker 未運行，請先啟動 Docker"
        exit 1
    fi
    log_success "Docker 運行中"
}

# 檢查 Java
check_java() {
    if ! command -v java &> /dev/null; then
        log_error "Java 未安裝"
        exit 1
    fi
    log_success "Java 版本: $(java -version 2>&1 | head -1)"
}

# 檢查 Node.js
check_node() {
    if ! command -v node &> /dev/null; then
        log_error "Node.js 未安裝"
        exit 1
    fi
    log_success "Node.js 版本: $(node --version)"
}

# 建置專案
build_project() {
    log_step "建置 Java 專案..."
    cd "$PROJECT_ROOT"
    ./gradlew build -x test --quiet
    log_success "Java 專案建置完成"
}

# 啟動可觀測性堆疊
start_observability() {
    log_step "啟動可觀測性堆疊..."
    cd "$PROJECT_ROOT"

    if [ -f "docker-compose.dev.yml" ]; then
        docker compose -f docker-compose.dev.yml up -d
    else
        # 只啟動可觀測性相關服務
        docker compose up -d otel-collector jaeger prometheus grafana 2>/dev/null || \
        log_warning "docker-compose.dev.yml 不存在，跳過可觀測性堆疊"
    fi

    log_success "可觀測性堆疊已啟動"
}

# 啟動天氣服務
start_weather_service() {
    log_step "啟動天氣服務 (port 8081)..."
    cd "$PROJECT_ROOT/weather-service"

    # 使用 bootRun 背景執行
    nohup ../gradlew bootRun --quiet > "$PID_DIR/weather-service.log" 2>&1 &
    echo $! > "$PID_DIR/weather-service.pid"

    # 等待服務啟動
    log_info "等待天氣服務啟動..."
    for i in {1..60}; do
        if curl -s http://localhost:8081/actuator/health > /dev/null 2>&1; then
            log_success "天氣服務已啟動: http://localhost:8081"
            return 0
        fi
        sleep 2
    done

    log_warning "天氣服務啟動逾時，請檢查日誌: $PID_DIR/weather-service.log"
}

# 啟動閘道器
start_gateway() {
    log_step "啟動閘道器 (port 8080)..."
    cd "$PROJECT_ROOT/gateway"

    nohup ../gradlew bootRun --quiet > "$PID_DIR/gateway.log" 2>&1 &
    echo $! > "$PID_DIR/gateway.pid"

    log_info "等待閘道器啟動..."
    for i in {1..60}; do
        if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
            log_success "閘道器已啟動: http://localhost:8080"
            return 0
        fi
        sleep 2
    done

    log_warning "閘道器啟動逾時，請檢查日誌: $PID_DIR/gateway.log"
}

# 啟動前端
start_frontend() {
    log_step "啟動前端 (port 5173)..."
    cd "$PROJECT_ROOT/frontend"

    # 安裝依賴
    if [ ! -d "node_modules" ]; then
        log_info "安裝前端依賴..."
        npm install --silent
    fi

    nohup npm run dev > "$PID_DIR/frontend.log" 2>&1 &
    echo $! > "$PID_DIR/frontend.pid"

    log_info "等待前端啟動..."
    for i in {1..30}; do
        if curl -s http://localhost:5173 > /dev/null 2>&1; then
            log_success "前端已啟動: http://localhost:5173"
            return 0
        fi
        sleep 1
    done

    log_warning "前端啟動逾時，請檢查日誌: $PID_DIR/frontend.log"
}

# 停止所有服務
stop_services() {
    log_step "停止所有服務..."

    # 停止 Java 服務
    for service in weather-service gateway frontend; do
        if [ -f "$PID_DIR/$service.pid" ]; then
            local pid=$(cat "$PID_DIR/$service.pid")
            if kill -0 "$pid" 2>/dev/null; then
                log_info "停止 $service (PID: $pid)..."
                kill "$pid" 2>/dev/null || true
                # 等待程序結束
                for i in {1..10}; do
                    if ! kill -0 "$pid" 2>/dev/null; then
                        break
                    fi
                    sleep 1
                done
                # 強制終止
                kill -9 "$pid" 2>/dev/null || true
            fi
            rm -f "$PID_DIR/$service.pid"
        fi
    done

    # 停止 Gradle daemon 相關的 Java 程序
    pkill -f "weather-service" 2>/dev/null || true
    pkill -f "gateway.*8080" 2>/dev/null || true

    # 停止 Docker 可觀測性堆疊
    cd "$PROJECT_ROOT"
    if [ -f "docker-compose.dev.yml" ]; then
        docker compose -f docker-compose.dev.yml down 2>/dev/null || true
    fi

    log_success "所有服務已停止"
}

# 顯示服務狀態
show_status() {
    echo ""
    echo "======================================"
    echo "  Weather Tracing PoC - 服務狀態"
    echo "======================================"
    echo ""

    # 檢查各服務
    local services=(
        "天氣服務|http://localhost:8081/actuator/health"
        "閘道器|http://localhost:8080/actuator/health"
        "前端|http://localhost:5173"
        "Jaeger|http://localhost:16686"
        "Prometheus|http://localhost:9090/-/healthy"
        "Grafana|http://localhost:3000/api/health"
    )

    printf "%-15s %-10s %-30s\n" "服務" "狀態" "網址"
    printf "%-15s %-10s %-30s\n" "------" "------" "-----"

    for item in "${services[@]}"; do
        IFS='|' read -r name url <<< "$item"
        if curl -s --max-time 2 "$url" > /dev/null 2>&1; then
            printf "%-15s ${GREEN}%-10s${NC} %-30s\n" "$name" "運行中" "$url"
        else
            printf "%-15s ${RED}%-10s${NC} %-30s\n" "$name" "未運行" "$url"
        fi
    done
    echo ""
}

# 顯示存取資訊
show_access_info() {
    echo ""
    echo "======================================"
    echo "  服務存取資訊"
    echo "======================================"
    echo ""
    echo "  應用程式:"
    echo "    前端介面      http://localhost:5173"
    echo "    閘道器 API    http://localhost:8080/api"
    echo "    天氣服務      http://localhost:8081"
    echo "    Swagger UI    http://localhost:8081/swagger-ui.html"
    echo "    H2 Console    http://localhost:8081/h2-console"
    echo ""
    echo "  可觀測性:"
    echo "    Jaeger UI     http://localhost:16686"
    echo "    Prometheus    http://localhost:9090"
    echo "    Grafana       http://localhost:3000 (admin/admin)"
    echo ""
    echo "  測試 API:"
    echo "    curl http://localhost:8080/api/weather/TPE | jq"
    echo ""
}

# 查看日誌
show_logs() {
    local service="${1:-all}"

    case "$service" in
        weather-service|weather)
            tail -f "$PID_DIR/weather-service.log"
            ;;
        gateway)
            tail -f "$PID_DIR/gateway.log"
            ;;
        frontend)
            tail -f "$PID_DIR/frontend.log"
            ;;
        all)
            tail -f "$PID_DIR"/*.log
            ;;
        *)
            log_error "未知服務: $service"
            echo "可用服務: weather-service, gateway, frontend, all"
            ;;
    esac
}

# 顯示使用說明
usage() {
    cat << EOF
Weather Tracing PoC - 本地開發服務管理腳本

使用方式:
    $0 <命令>

命令:
    start           啟動所有服務（預設）
    stop            停止所有服務
    restart         重新啟動所有服務
    status          顯示服務狀態
    logs [service]  查看服務日誌
    help            顯示此說明

日誌選項:
    logs                查看所有日誌
    logs weather        查看天氣服務日誌
    logs gateway        查看閘道器日誌
    logs frontend       查看前端日誌

範例:
    # 啟動所有服務
    $0 start

    # 檢查狀態
    $0 status

    # 查看閘道器日誌
    $0 logs gateway

    # 停止所有服務
    $0 stop

EOF
}

# 完整啟動流程
start_all() {
    init_pid_dir
    check_docker
    check_java
    check_node

    echo ""
    echo "======================================"
    echo "  Weather Tracing PoC - 本地開發啟動"
    echo "======================================"
    echo ""

    build_project
    start_observability

    # 等待可觀測性堆疊就緒
    sleep 5

    start_weather_service
    start_gateway
    start_frontend

    show_access_info

    log_success "所有服務啟動完成！"
}

# 主函式
main() {
    local command="${1:-start}"
    shift || true

    case "$command" in
        start)
            start_all
            ;;
        stop)
            stop_services
            ;;
        restart)
            stop_services
            sleep 3
            start_all
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs "$@"
            ;;
        help|--help|-h)
            usage
            ;;
        *)
            log_error "未知命令: $command"
            usage
            exit 1
            ;;
    esac
}

main "$@"
