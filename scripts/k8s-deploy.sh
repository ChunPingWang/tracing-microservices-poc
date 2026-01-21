#!/bin/bash
# Kubernetes 部署腳本
# 用於部署和管理 Weather Tracing PoC 應用程式

set -e

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 配置
NAMESPACE="weather-tracing"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
K8S_BASE_DIR="${PROJECT_ROOT}/k8s/base"

# 輸出函式
info() { echo -e "${BLUE}[INFO]${NC} $1"; }
success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }
step() { echo -e "${CYAN}[STEP]${NC} $1"; }

# 檢查前置需求
check_prerequisites() {
    info "檢查前置需求..."

    # 檢查 kubectl
    if ! command -v kubectl &> /dev/null; then
        error "kubectl 未安裝"
    fi

    # 檢查叢集連線
    if ! kubectl cluster-info &> /dev/null; then
        error "無法連線到 Kubernetes 叢集。請確認叢集已啟動並設定正確的 context"
    fi

    # 檢查 kustomize
    if ! command -v kustomize &> /dev/null; then
        # 使用 kubectl 內建的 kustomize
        info "使用 kubectl 內建的 kustomize"
    fi

    success "前置需求檢查通過"
}

# 建置 Docker 映像檔
build_images() {
    step "建置 Docker 映像檔..."

    cd "${PROJECT_ROOT}"

    # 建置 weather-service
    info "建置 weather-service..."
    docker build -t weather-service:latest ./weather-service
    success "weather-service 建置完成"

    # 建置 gateway
    info "建置 gateway..."
    docker build -t gateway:latest ./gateway
    success "gateway 建置完成"

    # 建置 frontend
    info "建置 frontend..."
    docker build -t frontend:latest ./frontend
    success "frontend 建置完成"
}

# 載入映像檔到 Kind
load_images_to_kind() {
    step "載入映像檔到 Kind 叢集..."

    local cluster_name="${1:-weather-tracing}"

    # 檢查是否為 Kind 叢集
    if ! kind get clusters 2>/dev/null | grep -q "^${cluster_name}$"; then
        warn "Kind 叢集 '${cluster_name}' 不存在，跳過映像檔載入"
        return 0
    fi

    local images=("weather-service:latest" "gateway:latest" "frontend:latest")

    for image in "${images[@]}"; do
        if docker image inspect "$image" &> /dev/null; then
            info "載入: $image"
            kind load docker-image "$image" --name "${cluster_name}"
        else
            warn "映像檔不存在: $image"
        fi
    done

    success "映像檔載入完成"
}

# 部署應用程式
deploy() {
    step "部署應用程式到 Kubernetes..."

    # 檢查 K8s 配置目錄
    if [ ! -d "${K8S_BASE_DIR}" ]; then
        error "K8s 配置目錄不存在: ${K8S_BASE_DIR}"
    fi

    # 使用 kustomize 部署
    info "套用 Kustomize 配置..."
    kubectl apply -k "${K8S_BASE_DIR}"

    success "Kubernetes 資源已建立"

    # 等待部署完成
    wait_for_deployments
}

# 等待所有部署就緒
wait_for_deployments() {
    step "等待部署就緒..."

    local deployments=("weather-service" "gateway" "frontend" "otel-collector" "jaeger" "prometheus" "grafana")
    local timeout=300

    for deployment in "${deployments[@]}"; do
        info "等待 ${deployment}..."
        if kubectl wait --for=condition=available deployment/"${deployment}" \
            -n "${NAMESPACE}" --timeout="${timeout}s" 2>/dev/null; then
            success "${deployment} 已就緒"
        else
            warn "${deployment} 未能在 ${timeout} 秒內就緒"
        fi
    done
}

# 檢查部署狀態
check_status() {
    step "檢查部署狀態..."

    echo ""
    echo "=== 命名空間 ==="
    kubectl get namespace "${NAMESPACE}" 2>/dev/null || warn "命名空間 ${NAMESPACE} 不存在"

    echo ""
    echo "=== Deployments ==="
    kubectl get deployments -n "${NAMESPACE}" 2>/dev/null || echo "無部署"

    echo ""
    echo "=== Pods ==="
    kubectl get pods -n "${NAMESPACE}" -o wide 2>/dev/null || echo "無 Pod"

    echo ""
    echo "=== Services ==="
    kubectl get services -n "${NAMESPACE}" 2>/dev/null || echo "無服務"

    echo ""
    echo "=== Ingress ==="
    kubectl get ingress -n "${NAMESPACE}" 2>/dev/null || echo "無 Ingress"
}

# 顯示存取資訊
show_access_info() {
    step "存取資訊"

    echo ""
    echo "======================================"
    echo "  Weather Tracing PoC - 存取資訊"
    echo "======================================"
    echo ""

    # 檢查 Ingress 是否可用
    if kubectl get ingress -n "${NAMESPACE}" &>/dev/null; then
        echo "透過 Ingress 存取 (需設定 /etc/hosts):"
        echo ""
        echo "  請將以下內容加入 /etc/hosts:"
        echo "  127.0.0.1 weather.local jaeger.local grafana.local prometheus.local"
        echo ""
        echo "  服務網址:"
        echo "  ┌────────────────────────────────────────────────┐"
        echo "  │ 前端          http://weather.local             │"
        echo "  │ API           http://weather.local/api         │"
        echo "  │ Jaeger        http://jaeger.local              │"
        echo "  │ Prometheus    http://prometheus.local          │"
        echo "  │ Grafana       http://grafana.local (admin/admin)│"
        echo "  └────────────────────────────────────────────────┘"
    fi

    echo ""
    echo "透過 Port Forward 存取:"
    echo ""
    echo "  # 前端"
    echo "  kubectl port-forward -n ${NAMESPACE} svc/frontend 8000:80"
    echo ""
    echo "  # 閘道器 API"
    echo "  kubectl port-forward -n ${NAMESPACE} svc/gateway 8080:8080"
    echo ""
    echo "  # 天氣服務"
    echo "  kubectl port-forward -n ${NAMESPACE} svc/weather-service 8081:8081"
    echo ""
    echo "  # Jaeger UI"
    echo "  kubectl port-forward -n ${NAMESPACE} svc/jaeger 16686:16686"
    echo ""
    echo "  # Prometheus"
    echo "  kubectl port-forward -n ${NAMESPACE} svc/prometheus 9090:9090"
    echo ""
    echo "  # Grafana"
    echo "  kubectl port-forward -n ${NAMESPACE} svc/grafana 3000:3000"
    echo ""
}

# 快速測試
quick_test() {
    step "執行快速測試..."

    # 使用 port-forward 測試
    info "建立 port-forward 連線..."

    # 啟動 port-forward (背景執行)
    kubectl port-forward -n "${NAMESPACE}" svc/gateway 18080:8080 &>/dev/null &
    local pf_pid=$!

    # 等待 port-forward 就緒
    sleep 3

    # 測試 API
    info "測試天氣 API..."
    if curl -s "http://localhost:18080/api/weather/TPE" | grep -q "cityCode"; then
        success "API 測試通過"
    else
        warn "API 測試失敗"
    fi

    # 清理 port-forward
    kill $pf_pid 2>/dev/null || true
}

# 刪除部署
undeploy() {
    step "刪除 Kubernetes 部署..."

    if [ ! -d "${K8S_BASE_DIR}" ]; then
        error "K8s 配置目錄不存在: ${K8S_BASE_DIR}"
    fi

    info "刪除所有資源..."
    kubectl delete -k "${K8S_BASE_DIR}" --ignore-not-found=true

    success "部署已刪除"
}

# 查看日誌
show_logs() {
    local deployment="${1:-gateway}"

    info "顯示 ${deployment} 日誌..."
    kubectl logs -f "deployment/${deployment}" -n "${NAMESPACE}" --tail=100
}

# 重新部署
redeploy() {
    step "重新部署應用程式..."

    # 重新建置映像檔
    build_images

    # 載入到 Kind
    load_images_to_kind

    # 重啟部署
    info "重啟部署..."
    kubectl rollout restart deployment/weather-service -n "${NAMESPACE}" 2>/dev/null || true
    kubectl rollout restart deployment/gateway -n "${NAMESPACE}" 2>/dev/null || true
    kubectl rollout restart deployment/frontend -n "${NAMESPACE}" 2>/dev/null || true

    # 等待就緒
    wait_for_deployments

    success "重新部署完成"
}

# 顯示使用說明
show_usage() {
    cat << EOF
Kubernetes 部署管理腳本

使用方式:
    $0 <命令> [選項]

命令:
    build           建置 Docker 映像檔
    load            載入映像檔到 Kind 叢集
    deploy          部署應用程式到 K8s
    undeploy        刪除 K8s 部署
    redeploy        重新建置並部署
    status          檢查部署狀態
    access          顯示存取資訊
    test            執行快速測試
    logs [name]     查看部署日誌 (預設: gateway)
    all             完整部署流程 (build -> load -> deploy)
    help            顯示此說明

範例:
    # 完整部署流程
    $0 all

    # 只部署（映像檔已存在）
    $0 deploy

    # 檢查狀態
    $0 status

    # 查看閘道器日誌
    $0 logs gateway

    # 刪除部署
    $0 undeploy

EOF
}

# 完整部署流程
full_deploy() {
    check_prerequisites
    build_images
    load_images_to_kind
    deploy
    show_access_info
}

# 主函式
main() {
    local command="${1:-help}"
    shift || true

    case "$command" in
        build)
            build_images
            ;;
        load)
            load_images_to_kind "$@"
            ;;
        deploy)
            check_prerequisites
            deploy
            show_access_info
            ;;
        undeploy)
            undeploy
            ;;
        redeploy)
            check_prerequisites
            redeploy
            ;;
        status)
            check_status
            ;;
        access)
            show_access_info
            ;;
        test)
            quick_test
            ;;
        logs)
            show_logs "$@"
            ;;
        all)
            full_deploy
            ;;
        help|--help|-h)
            show_usage
            ;;
        *)
            error "未知命令: $command\n執行 '$0 help' 查看使用說明"
            ;;
    esac
}

main "$@"
