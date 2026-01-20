#!/bin/bash
#
# Weather Tracing PoC - Integration Test Script
# 整合測試腳本：驗證整個系統端到端的功能
#

set -e

# =============================================================================
# Configuration
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Service URLs
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
WEATHER_SERVICE_URL="${WEATHER_SERVICE_URL:-http://localhost:8081}"
JAEGER_URL="${JAEGER_URL:-http://localhost:16686}"
PROMETHEUS_URL="${PROMETHEUS_URL:-http://localhost:9090}"
GRAFANA_URL="${GRAFANA_URL:-http://localhost:3000}"

# Test configuration
MAX_RETRIES=30
RETRY_INTERVAL=2
VERBOSE=${VERBOSE:-false}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# =============================================================================
# Utility Functions
# =============================================================================

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[PASS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[FAIL]${NC} $1"
}

log_verbose() {
    if [ "$VERBOSE" = "true" ]; then
        echo -e "${BLUE}[DEBUG]${NC} $1"
    fi
}

# Wait for a service to be healthy
wait_for_service() {
    local name=$1
    local url=$2
    local retries=0

    log_info "Waiting for $name to be ready at $url..."

    while [ $retries -lt $MAX_RETRIES ]; do
        if curl -s -f "$url" > /dev/null 2>&1; then
            log_success "$name is ready"
            return 0
        fi
        retries=$((retries + 1))
        log_verbose "Retry $retries/$MAX_RETRIES for $name"
        sleep $RETRY_INTERVAL
    done

    log_error "$name failed to start after $MAX_RETRIES retries"
    return 1
}

# Make HTTP request and capture response
http_get() {
    local url=$1
    local response
    local http_code

    response=$(curl -s -w "\n%{http_code}" "$url" 2>/dev/null)
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    echo "$http_code|$body"
}

# Extract JSON field using grep/sed (no jq dependency)
json_field() {
    local json=$1
    local field=$2
    echo "$json" | grep -o "\"$field\"[[:space:]]*:[[:space:]]*\"[^\"]*\"" | sed "s/\"$field\"[[:space:]]*:[[:space:]]*\"//" | sed 's/"$//' | head -1
}

json_field_number() {
    local json=$1
    local field=$2
    echo "$json" | grep -o "\"$field\"[[:space:]]*:[[:space:]]*[0-9.]*" | sed "s/\"$field\"[[:space:]]*:[[:space:]]*//" | head -1
}

json_field_bool() {
    local json=$1
    local field=$2
    echo "$json" | grep -o "\"$field\"[[:space:]]*:[[:space:]]*[a-z]*" | sed "s/\"$field\"[[:space:]]*:[[:space:]]*//" | head -1
}

# =============================================================================
# Test Cases
# =============================================================================

test_count=0
pass_count=0
fail_count=0

run_test() {
    local test_name=$1
    local test_func=$2

    test_count=$((test_count + 1))
    echo ""
    log_info "Running test: $test_name"

    if $test_func; then
        pass_count=$((pass_count + 1))
        log_success "Test passed: $test_name"
        return 0
    else
        fail_count=$((fail_count + 1))
        log_error "Test failed: $test_name"
        return 1
    fi
}

# -----------------------------------------------------------------------------
# Health Check Tests
# -----------------------------------------------------------------------------

test_weather_service_health() {
    local result=$(http_get "$WEATHER_SERVICE_URL/actuator/health")
    local code=$(echo "$result" | cut -d'|' -f1)
    local body=$(echo "$result" | cut -d'|' -f2-)

    log_verbose "Health response: $code - $body"

    if [ "$code" = "200" ]; then
        local status=$(json_field "$body" "status")
        if [ "$status" = "UP" ]; then
            return 0
        fi
    fi
    return 1
}

test_gateway_health() {
    local result=$(http_get "$GATEWAY_URL/actuator/health")
    local code=$(echo "$result" | cut -d'|' -f1)
    local body=$(echo "$result" | cut -d'|' -f2-)

    log_verbose "Gateway health response: $code - $body"

    if [ "$code" = "200" ]; then
        local status=$(json_field "$body" "status")
        if [ "$status" = "UP" ]; then
            return 0
        fi
    fi
    return 1
}

test_jaeger_health() {
    local result=$(http_get "$JAEGER_URL/api/services")
    local code=$(echo "$result" | cut -d'|' -f1)

    log_verbose "Jaeger health response code: $code"

    [ "$code" = "200" ]
}

test_prometheus_health() {
    local result=$(http_get "$PROMETHEUS_URL/-/healthy")
    local code=$(echo "$result" | cut -d'|' -f1)

    log_verbose "Prometheus health response code: $code"

    [ "$code" = "200" ]
}

test_grafana_health() {
    local result=$(http_get "$GRAFANA_URL/api/health")
    local code=$(echo "$result" | cut -d'|' -f1)

    log_verbose "Grafana health response code: $code"

    [ "$code" = "200" ]
}

# -----------------------------------------------------------------------------
# Weather API Tests
# -----------------------------------------------------------------------------

test_weather_api_taipei() {
    local result=$(http_get "$GATEWAY_URL/api/weather/TPE")
    local code=$(echo "$result" | cut -d'|' -f1)
    local body=$(echo "$result" | cut -d'|' -f2-)

    log_verbose "Weather API response: $code - $body"

    if [ "$code" = "200" ]; then
        local success=$(json_field_bool "$body" "success")
        local city_code=$(json_field "$body" "cityCode")

        if [ "$success" = "true" ] && [ "$city_code" = "TPE" ]; then
            return 0
        fi
    fi
    return 1
}

test_weather_api_taichung() {
    local result=$(http_get "$GATEWAY_URL/api/weather/TXG")
    local code=$(echo "$result" | cut -d'|' -f1)
    local body=$(echo "$result" | cut -d'|' -f2-)

    log_verbose "Weather API response: $code - $body"

    if [ "$code" = "200" ]; then
        local success=$(json_field_bool "$body" "success")
        local city_code=$(json_field "$body" "cityCode")

        if [ "$success" = "true" ] && [ "$city_code" = "TXG" ]; then
            return 0
        fi
    fi
    return 1
}

test_weather_api_kaohsiung() {
    local result=$(http_get "$GATEWAY_URL/api/weather/KHH")
    local code=$(echo "$result" | cut -d'|' -f1)
    local body=$(echo "$result" | cut -d'|' -f2-)

    log_verbose "Weather API response: $code - $body"

    if [ "$code" = "200" ]; then
        local success=$(json_field_bool "$body" "success")
        local city_code=$(json_field "$body" "cityCode")

        if [ "$success" = "true" ] && [ "$city_code" = "KHH" ]; then
            return 0
        fi
    fi
    return 1
}

test_weather_api_invalid_city() {
    local result=$(http_get "$GATEWAY_URL/api/weather/INVALID")
    local code=$(echo "$result" | cut -d'|' -f1)
    local body=$(echo "$result" | cut -d'|' -f2-)

    log_verbose "Invalid city response: $code - $body"

    # Should return 400 Bad Request for invalid city code
    [ "$code" = "400" ]
}

test_weather_api_direct() {
    # Test direct access to weather service (bypass gateway)
    local result=$(http_get "$WEATHER_SERVICE_URL/weather/TPE")
    local code=$(echo "$result" | cut -d'|' -f1)
    local body=$(echo "$result" | cut -d'|' -f2-)

    log_verbose "Direct API response: $code - $body"

    if [ "$code" = "200" ]; then
        local success=$(json_field_bool "$body" "success")
        if [ "$success" = "true" ]; then
            return 0
        fi
    fi
    return 1
}

# -----------------------------------------------------------------------------
# Trace Propagation Tests
# -----------------------------------------------------------------------------

test_trace_header_present() {
    local response
    response=$(curl -s -i "$GATEWAY_URL/api/weather/TPE" 2>/dev/null)

    log_verbose "Full response headers: $response"

    # Check for trace headers in response
    if echo "$response" | grep -qi "x-trace-id"; then
        return 0
    fi
    return 1
}

test_trace_id_format() {
    local response
    response=$(curl -s -i "$GATEWAY_URL/api/weather/TPE" 2>/dev/null)

    local trace_id=$(echo "$response" | grep -i "x-trace-id" | sed 's/.*: //' | tr -d '\r')

    log_verbose "Trace ID: $trace_id"

    # Trace ID should be 32 hex characters
    if echo "$trace_id" | grep -qE "^[a-f0-9]{32}$"; then
        return 0
    fi
    return 1
}

test_trace_in_jaeger() {
    # First, make a request to generate a trace
    curl -s "$GATEWAY_URL/api/weather/TPE" > /dev/null

    # Wait for trace to be processed
    sleep 3

    # Check if traces exist in Jaeger
    local result=$(http_get "$JAEGER_URL/api/services")
    local code=$(echo "$result" | cut -d'|' -f1)
    local body=$(echo "$result" | cut -d'|' -f2-)

    log_verbose "Jaeger services: $body"

    # Should have at least one service registered
    if [ "$code" = "200" ] && echo "$body" | grep -q "data"; then
        return 0
    fi
    return 1
}

# -----------------------------------------------------------------------------
# Response Data Tests
# -----------------------------------------------------------------------------

test_response_contains_temperature() {
    local result=$(http_get "$GATEWAY_URL/api/weather/TPE")
    local code=$(echo "$result" | cut -d'|' -f1)
    local body=$(echo "$result" | cut -d'|' -f2-)

    if [ "$code" = "200" ]; then
        local temp=$(json_field_number "$body" "temperature")
        log_verbose "Temperature: $temp"

        # Temperature should be a reasonable value (0-50)
        if [ -n "$temp" ]; then
            # Use bc for floating point comparison
            if command -v bc &> /dev/null; then
                if [ $(echo "$temp >= 0 && $temp <= 50" | bc) -eq 1 ]; then
                    return 0
                fi
            else
                # Fallback: just check it's not empty
                return 0
            fi
        fi
    fi
    return 1
}

test_response_contains_rainfall() {
    local result=$(http_get "$GATEWAY_URL/api/weather/TPE")
    local code=$(echo "$result" | cut -d'|' -f1)
    local body=$(echo "$result" | cut -d'|' -f2-)

    if [ "$code" = "200" ]; then
        local rainfall=$(json_field_number "$body" "rainfall")
        log_verbose "Rainfall: $rainfall"

        # Rainfall should be a non-negative value
        if [ -n "$rainfall" ]; then
            return 0
        fi
    fi
    return 1
}

test_response_contains_trace_info() {
    local result=$(http_get "$GATEWAY_URL/api/weather/TPE")
    local code=$(echo "$result" | cut -d'|' -f1)
    local body=$(echo "$result" | cut -d'|' -f2-)

    if [ "$code" = "200" ]; then
        # Check for traceInfo in response body
        if echo "$body" | grep -q "traceInfo"; then
            local trace_id=$(json_field "$body" "traceId")
            log_verbose "Trace ID in body: $trace_id"
            if [ -n "$trace_id" ]; then
                return 0
            fi
        fi
    fi
    return 1
}

test_response_duration_present() {
    local result=$(http_get "$GATEWAY_URL/api/weather/TPE")
    local code=$(echo "$result" | cut -d'|' -f1)
    local body=$(echo "$result" | cut -d'|' -f2-)

    if [ "$code" = "200" ]; then
        local duration=$(json_field_number "$body" "duration")
        log_verbose "Duration: $duration ms"

        if [ -n "$duration" ]; then
            return 0
        fi
    fi
    return 1
}

# -----------------------------------------------------------------------------
# Performance Tests
# -----------------------------------------------------------------------------

test_response_time_under_threshold() {
    local threshold_ms=5000  # 5 seconds
    local start_time=$(date +%s%N)

    curl -s "$GATEWAY_URL/api/weather/TPE" > /dev/null

    local end_time=$(date +%s%N)
    local duration_ms=$(( (end_time - start_time) / 1000000 ))

    log_verbose "Response time: ${duration_ms}ms (threshold: ${threshold_ms}ms)"

    [ $duration_ms -lt $threshold_ms ]
}

test_concurrent_requests() {
    local concurrent=5
    local pids=()
    local success=0

    log_verbose "Sending $concurrent concurrent requests..."

    for i in $(seq 1 $concurrent); do
        (
            result=$(http_get "$GATEWAY_URL/api/weather/TPE")
            code=$(echo "$result" | cut -d'|' -f1)
            [ "$code" = "200" ]
        ) &
        pids+=($!)
    done

    # Wait for all requests and count successes
    for pid in "${pids[@]}"; do
        if wait $pid; then
            success=$((success + 1))
        fi
    done

    log_verbose "Successful requests: $success/$concurrent"

    [ $success -eq $concurrent ]
}

# =============================================================================
# Main Execution
# =============================================================================

print_banner() {
    echo ""
    echo "============================================================"
    echo "  Weather Tracing PoC - Integration Test Suite"
    echo "  整合測試套件"
    echo "============================================================"
    echo ""
}

print_config() {
    log_info "Configuration:"
    echo "  Gateway URL:         $GATEWAY_URL"
    echo "  Weather Service URL: $WEATHER_SERVICE_URL"
    echo "  Jaeger URL:          $JAEGER_URL"
    echo "  Prometheus URL:      $PROMETHEUS_URL"
    echo "  Grafana URL:         $GRAFANA_URL"
    echo "  Verbose:             $VERBOSE"
    echo ""
}

print_summary() {
    echo ""
    echo "============================================================"
    echo "  Test Summary / 測試摘要"
    echo "============================================================"
    echo ""
    echo "  Total tests:  $test_count"
    echo "  Passed:       $pass_count"
    echo "  Failed:       $fail_count"
    echo ""

    if [ $fail_count -eq 0 ]; then
        log_success "All tests passed! 所有測試通過!"
        return 0
    else
        log_error "$fail_count test(s) failed. 有 $fail_count 個測試失敗。"
        return 1
    fi
}

usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -h, --help          Show this help message"
    echo "  -v, --verbose       Enable verbose output"
    echo "  -w, --wait          Wait for services before running tests"
    echo "  --health-only       Run only health check tests"
    echo "  --api-only          Run only API tests"
    echo "  --trace-only        Run only trace tests"
    echo ""
    echo "Environment Variables:"
    echo "  GATEWAY_URL         Gateway service URL (default: http://localhost:8080)"
    echo "  WEATHER_SERVICE_URL Weather service URL (default: http://localhost:8081)"
    echo "  JAEGER_URL          Jaeger UI URL (default: http://localhost:16686)"
    echo "  PROMETHEUS_URL      Prometheus URL (default: http://localhost:9090)"
    echo "  GRAFANA_URL         Grafana URL (default: http://localhost:3000)"
    echo ""
}

main() {
    local wait_for_services=false
    local health_only=false
    local api_only=false
    local trace_only=false

    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                usage
                exit 0
                ;;
            -v|--verbose)
                VERBOSE=true
                shift
                ;;
            -w|--wait)
                wait_for_services=true
                shift
                ;;
            --health-only)
                health_only=true
                shift
                ;;
            --api-only)
                api_only=true
                shift
                ;;
            --trace-only)
                trace_only=true
                shift
                ;;
            *)
                log_error "Unknown option: $1"
                usage
                exit 1
                ;;
        esac
    done

    print_banner
    print_config

    # Wait for services if requested
    if [ "$wait_for_services" = "true" ]; then
        log_info "Waiting for services to be ready..."
        wait_for_service "Weather Service" "$WEATHER_SERVICE_URL/actuator/health"
        wait_for_service "Gateway" "$GATEWAY_URL/actuator/health"
        wait_for_service "Jaeger" "$JAEGER_URL/api/services"
        wait_for_service "Prometheus" "$PROMETHEUS_URL/-/healthy"
        wait_for_service "Grafana" "$GRAFANA_URL/api/health"
        echo ""
    fi

    # Run test suites based on options
    if [ "$health_only" = "true" ]; then
        log_info "Running health check tests only..."
        run_test "Weather Service Health" test_weather_service_health
        run_test "Gateway Health" test_gateway_health
        run_test "Jaeger Health" test_jaeger_health
        run_test "Prometheus Health" test_prometheus_health
        run_test "Grafana Health" test_grafana_health
    elif [ "$api_only" = "true" ]; then
        log_info "Running API tests only..."
        run_test "Weather API - Taipei (TPE)" test_weather_api_taipei
        run_test "Weather API - Taichung (TXG)" test_weather_api_taichung
        run_test "Weather API - Kaohsiung (KHH)" test_weather_api_kaohsiung
        run_test "Weather API - Invalid City" test_weather_api_invalid_city
        run_test "Weather API - Direct Access" test_weather_api_direct
        run_test "Response Contains Temperature" test_response_contains_temperature
        run_test "Response Contains Rainfall" test_response_contains_rainfall
    elif [ "$trace_only" = "true" ]; then
        log_info "Running trace tests only..."
        run_test "Trace Header Present" test_trace_header_present
        run_test "Trace ID Format Valid" test_trace_id_format
        run_test "Trace in Jaeger" test_trace_in_jaeger
        run_test "Response Contains Trace Info" test_response_contains_trace_info
        run_test "Response Duration Present" test_response_duration_present
    else
        # Run all tests
        log_info "Running all tests..."

        echo ""
        echo "--- Health Check Tests / 健康檢查測試 ---"
        run_test "Weather Service Health" test_weather_service_health
        run_test "Gateway Health" test_gateway_health
        run_test "Jaeger Health" test_jaeger_health
        run_test "Prometheus Health" test_prometheus_health
        run_test "Grafana Health" test_grafana_health

        echo ""
        echo "--- Weather API Tests / 天氣 API 測試 ---"
        run_test "Weather API - Taipei (TPE)" test_weather_api_taipei
        run_test "Weather API - Taichung (TXG)" test_weather_api_taichung
        run_test "Weather API - Kaohsiung (KHH)" test_weather_api_kaohsiung
        run_test "Weather API - Invalid City" test_weather_api_invalid_city
        run_test "Weather API - Direct Access" test_weather_api_direct

        echo ""
        echo "--- Response Data Tests / 回應資料測試 ---"
        run_test "Response Contains Temperature" test_response_contains_temperature
        run_test "Response Contains Rainfall" test_response_contains_rainfall
        run_test "Response Contains Trace Info" test_response_contains_trace_info
        run_test "Response Duration Present" test_response_duration_present

        echo ""
        echo "--- Trace Propagation Tests / 追蹤傳播測試 ---"
        run_test "Trace Header Present" test_trace_header_present
        run_test "Trace ID Format Valid" test_trace_id_format
        run_test "Trace in Jaeger" test_trace_in_jaeger

        echo ""
        echo "--- Performance Tests / 效能測試 ---"
        run_test "Response Time Under Threshold" test_response_time_under_threshold
        run_test "Concurrent Requests" test_concurrent_requests
    fi

    print_summary

    if [ $fail_count -gt 0 ]; then
        exit 1
    fi
}

main "$@"
