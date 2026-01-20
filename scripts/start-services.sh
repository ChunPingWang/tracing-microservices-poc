#!/bin/bash
#
# Weather Tracing PoC - Start Services Script
# 服務啟動腳本
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[OK]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# Check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        echo "Error: Docker is not running. Please start Docker first."
        exit 1
    fi
    log_success "Docker is running"
}

# Build Java services
build_services() {
    log_info "Building Java services..."
    cd "$PROJECT_ROOT"

    if [ -f "gradlew" ]; then
        ./gradlew build -x test
    else
        log_warning "Gradle wrapper not found. Skipping Java build."
    fi
}

# Start observability stack
start_observability() {
    log_info "Starting observability stack (Jaeger, Prometheus, Grafana, OTEL Collector)..."
    cd "$PROJECT_ROOT"
    docker-compose -f docker-compose.dev.yml up -d otel-collector jaeger prometheus grafana
}

# Start backend services
start_backend() {
    log_info "Starting backend services..."
    cd "$PROJECT_ROOT"

    # Start weather-service
    if [ -f "weather-service/build/libs/weather-service-0.0.1-SNAPSHOT.jar" ]; then
        log_info "Starting Weather Service..."
        java -jar weather-service/build/libs/weather-service-0.0.1-SNAPSHOT.jar &
        echo $! > /tmp/weather-service.pid
    else
        log_warning "Weather Service JAR not found. Please build first."
    fi

    # Start gateway
    if [ -f "gateway/build/libs/gateway-0.0.1-SNAPSHOT.jar" ]; then
        log_info "Starting Gateway..."
        java -jar gateway/build/libs/gateway-0.0.1-SNAPSHOT.jar &
        echo $! > /tmp/gateway.pid
    else
        log_warning "Gateway JAR not found. Please build first."
    fi
}

# Start with Docker Compose
start_with_docker() {
    log_info "Starting all services with Docker Compose..."
    cd "$PROJECT_ROOT"
    docker-compose -f docker-compose.dev.yml up -d
}

# Stop services
stop_services() {
    log_info "Stopping services..."

    # Stop Java processes
    if [ -f /tmp/weather-service.pid ]; then
        kill $(cat /tmp/weather-service.pid) 2>/dev/null || true
        rm /tmp/weather-service.pid
    fi

    if [ -f /tmp/gateway.pid ]; then
        kill $(cat /tmp/gateway.pid) 2>/dev/null || true
        rm /tmp/gateway.pid
    fi

    # Stop Docker services
    cd "$PROJECT_ROOT"
    docker-compose -f docker-compose.dev.yml down

    log_success "Services stopped"
}

usage() {
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  start       Start all services (default)"
    echo "  stop        Stop all services"
    echo "  restart     Restart all services"
    echo "  docker      Start all services with Docker Compose"
    echo "  obs         Start only observability stack"
    echo "  build       Build Java services"
    echo ""
}

main() {
    local command=${1:-start}

    case $command in
        start)
            check_docker
            start_observability
            sleep 5
            build_services
            start_backend
            log_success "All services started"
            ;;
        stop)
            stop_services
            ;;
        restart)
            stop_services
            sleep 2
            check_docker
            start_observability
            sleep 5
            start_backend
            log_success "All services restarted"
            ;;
        docker)
            check_docker
            start_with_docker
            log_success "All services started with Docker Compose"
            ;;
        obs)
            check_docker
            start_observability
            log_success "Observability stack started"
            ;;
        build)
            build_services
            log_success "Build completed"
            ;;
        -h|--help)
            usage
            ;;
        *)
            echo "Unknown command: $command"
            usage
            exit 1
            ;;
    esac
}

main "$@"
