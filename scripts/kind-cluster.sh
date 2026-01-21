#!/bin/bash
# Kind 叢集管理腳本
# 用於建立、刪除和管理本地 Kubernetes 叢集

set -e

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置
CLUSTER_NAME="weather-tracing"
KIND_CONFIG_FILE="kind-config.yaml"

# 輸出函式
info() { echo -e "${BLUE}[INFO]${NC} $1"; }
success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# 檢查前置工具
check_prerequisites() {
    info "檢查前置工具..."

    # 檢查 Kind
    if ! command -v kind &> /dev/null; then
        error "Kind 未安裝。請執行: brew install kind (macOS) 或參考 https://kind.sigs.k8s.io/docs/user/quick-start/#installation"
    fi
    success "Kind 版本: $(kind version)"

    # 檢查 kubectl
    if ! command -v kubectl &> /dev/null; then
        error "kubectl 未安裝。請執行: brew install kubectl (macOS)"
    fi
    success "kubectl 版本: $(kubectl version --client --short 2>/dev/null || kubectl version --client | head -1)"

    # 檢查 Docker
    if ! command -v docker &> /dev/null; then
        error "Docker 未安裝"
    fi
    if ! docker info &> /dev/null; then
        error "Docker daemon 未啟動"
    fi
    success "Docker 運行中"
}

# 建立 Kind 配置檔
create_kind_config() {
    local script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    local config_path="${script_dir}/${KIND_CONFIG_FILE}"

    if [ ! -f "$config_path" ]; then
        info "建立 Kind 配置檔: ${config_path}"
        cat > "$config_path" << 'EOF'
# Kind 叢集配置
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
name: weather-tracing
nodes:
  # Control plane node
  - role: control-plane
    kubeadmConfigPatches:
      - |
        kind: InitConfiguration
        nodeRegistration:
          kubeletExtraArgs:
            node-labels: "ingress-ready=true"
    extraPortMappings:
      # HTTP (使用 8000 避免與其他服務衝突)
      - containerPort: 80
        hostPort: 8000
        protocol: TCP
      # HTTPS
      - containerPort: 443
        hostPort: 8443
        protocol: TCP
      # NodePort 範圍（用於直接存取服務）
      - containerPort: 30080
        hostPort: 30080
        protocol: TCP
      - containerPort: 30081
        hostPort: 30081
        protocol: TCP
      - containerPort: 30090
        hostPort: 30090
        protocol: TCP
      - containerPort: 30686
        hostPort: 30686
        protocol: TCP
      - containerPort: 30300
        hostPort: 30300
        protocol: TCP
EOF
        success "Kind 配置檔已建立"
    else
        info "Kind 配置檔已存在: ${config_path}"
    fi
}

# 建立叢集
create_cluster() {
    local script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    local config_path="${script_dir}/${KIND_CONFIG_FILE}"

    # 檢查叢集是否已存在
    if kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
        warn "叢集 '${CLUSTER_NAME}' 已存在"
        read -p "是否刪除並重新建立？[y/N] " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            delete_cluster
        else
            info "保留現有叢集"
            return 0
        fi
    fi

    # 建立配置檔
    create_kind_config

    info "建立 Kind 叢集: ${CLUSTER_NAME}"
    kind create cluster --config="${config_path}"

    # 等待叢集就緒
    info "等待叢集就緒..."
    kubectl wait --for=condition=Ready nodes --all --timeout=120s

    success "叢集建立完成！"

    # 顯示叢集資訊
    show_cluster_info
}

# 刪除叢集
delete_cluster() {
    if ! kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
        warn "叢集 '${CLUSTER_NAME}' 不存在"
        return 0
    fi

    info "刪除 Kind 叢集: ${CLUSTER_NAME}"
    kind delete cluster --name="${CLUSTER_NAME}"
    success "叢集已刪除"
}

# 顯示叢集資訊
show_cluster_info() {
    info "叢集資訊："
    echo ""
    echo "叢集名稱: ${CLUSTER_NAME}"
    echo "Kubectl context: kind-${CLUSTER_NAME}"
    echo ""
    echo "節點狀態："
    kubectl get nodes -o wide
    echo ""
    echo "系統 Pod 狀態："
    kubectl get pods -n kube-system
}

# 安裝 NGINX Ingress Controller
install_ingress() {
    info "安裝 NGINX Ingress Controller..."

    # 使用官方的 Kind 專用配置
    kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml

    info "等待 Ingress Controller 就緒..."
    kubectl wait --namespace ingress-nginx \
        --for=condition=ready pod \
        --selector=app.kubernetes.io/component=controller \
        --timeout=180s

    success "NGINX Ingress Controller 安裝完成"
}

# 檢查叢集狀態
check_status() {
    if ! kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
        warn "叢集 '${CLUSTER_NAME}' 不存在"
        return 1
    fi

    info "叢集狀態："
    echo ""

    echo "=== 節點 ==="
    kubectl get nodes

    echo ""
    echo "=== 命名空間 ==="
    kubectl get namespaces

    echo ""
    echo "=== 所有 Pod ==="
    kubectl get pods --all-namespaces

    echo ""
    echo "=== Ingress ==="
    kubectl get ingress --all-namespaces 2>/dev/null || echo "無 Ingress 資源"
}

# 載入 Docker 映像檔到叢集
load_images() {
    info "載入 Docker 映像檔到 Kind 叢集..."

    local images=("weather-service:latest" "gateway:latest" "frontend:latest")

    for image in "${images[@]}"; do
        if docker image inspect "$image" &> /dev/null; then
            info "載入映像檔: $image"
            kind load docker-image "$image" --name "${CLUSTER_NAME}"
            success "已載入: $image"
        else
            warn "映像檔不存在: $image (請先執行 docker build)"
        fi
    done
}

# 顯示使用說明
show_usage() {
    cat << EOF
Kind 叢集管理腳本

使用方式:
    $0 <命令>

命令:
    create          建立 Kind 叢集
    delete          刪除 Kind 叢集
    status          檢查叢集狀態
    ingress         安裝 NGINX Ingress Controller
    load-images     載入 Docker 映像檔到叢集
    info            顯示叢集資訊
    help            顯示此說明

範例:
    # 建立叢集
    $0 create

    # 安裝 Ingress Controller
    $0 ingress

    # 載入應用程式映像檔
    $0 load-images

    # 檢查狀態
    $0 status

    # 刪除叢集
    $0 delete

環境變數:
    CLUSTER_NAME    叢集名稱 (預設: weather-tracing)

EOF
}

# 主函式
main() {
    local command="${1:-help}"

    case "$command" in
        create)
            check_prerequisites
            create_cluster
            ;;
        delete)
            delete_cluster
            ;;
        status)
            check_status
            ;;
        ingress)
            install_ingress
            ;;
        load-images)
            load_images
            ;;
        info)
            show_cluster_info
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
