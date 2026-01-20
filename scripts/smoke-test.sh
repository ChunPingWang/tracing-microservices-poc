#!/bin/bash
#
# Weather Tracing PoC - Smoke Test Script
# 煙霧測試腳本：快速驗證基本功能
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
WEATHER_SERVICE_URL="${WEATHER_SERVICE_URL:-http://localhost:8081}"

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_pass() { echo -e "${GREEN}[PASS]${NC} $1"; }
log_fail() { echo -e "${RED}[FAIL]${NC} $1"; }

echo ""
echo "========================================"
echo "  Smoke Test / 煙霧測試"
echo "========================================"
echo ""

tests_passed=0
tests_failed=0

# Test 1: Weather Service Health
log_info "Testing Weather Service health..."
if curl -sf "$WEATHER_SERVICE_URL/actuator/health" | grep -q "UP"; then
    log_pass "Weather Service is healthy"
    ((tests_passed++))
else
    log_fail "Weather Service is not healthy"
    ((tests_failed++))
fi

# Test 2: Gateway Health
log_info "Testing Gateway health..."
if curl -sf "$GATEWAY_URL/actuator/health" | grep -q "UP"; then
    log_pass "Gateway is healthy"
    ((tests_passed++))
else
    log_fail "Gateway is not healthy"
    ((tests_failed++))
fi

# Test 3: Weather API through Gateway
log_info "Testing Weather API (TPE)..."
response=$(curl -sf "$GATEWAY_URL/api/weather/TPE" 2>/dev/null || echo "")
if echo "$response" | grep -q '"success":true'; then
    log_pass "Weather API returned successful response"
    ((tests_passed++))
else
    log_fail "Weather API failed"
    ((tests_failed++))
fi

# Test 4: Trace ID in response
log_info "Testing Trace ID presence..."
if echo "$response" | grep -q '"traceId"'; then
    log_pass "Trace ID is present in response"
    ((tests_passed++))
else
    log_fail "Trace ID is missing from response"
    ((tests_failed++))
fi

# Test 5: Weather data validation
log_info "Testing weather data fields..."
if echo "$response" | grep -q '"temperature"' && echo "$response" | grep -q '"rainfall"'; then
    log_pass "Weather data contains required fields"
    ((tests_passed++))
else
    log_fail "Weather data is missing required fields"
    ((tests_failed++))
fi

echo ""
echo "========================================"
echo "  Results / 結果"
echo "========================================"
echo ""
echo "  Passed: $tests_passed"
echo "  Failed: $tests_failed"
echo ""

if [ $tests_failed -eq 0 ]; then
    log_pass "All smoke tests passed!"
    exit 0
else
    log_fail "Some smoke tests failed"
    exit 1
fi
