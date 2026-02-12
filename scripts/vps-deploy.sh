#!/usr/bin/env bash
#
# Minervia Institute - VPS Deployment Script
# Deploys the application to remote VPS server
#
# Usage:
#   ./vps-deploy.sh [--target TARGET] [--version VERSION] [--skip-build] [--help]
#
# Examples:
#   ./vps-deploy.sh --target HZUS
#   ./vps-deploy.sh --target HZUS --version v1.0.0
#   ./vps-deploy.sh --target HZUS --skip-build
#

set -euo pipefail

# =============================================================================
# Configuration
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# VPS tools
VPS_SSH="${HOME}/.agents/skills/vps-ssh-ops/scripts/vps-ssh.sh"
VPS_RSYNC="${HOME}/.agents/skills/vps-ssh-ops/scripts/vps-rsync.sh"

# Default values
TARGET="${VPS_SSH_TARGET:-HZUS}"
REMOTE_DIR="/opt/minervia"
VERSION=""
SKIP_BUILD=0
BARK_KEY="Hbyb4zFPsxcQNNdnZSe7Sn"

# =============================================================================
# Utility Functions
# =============================================================================

log_info() {
    echo -e "${GREEN}[INFO]${NC} $*"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $*"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $*"
}

log_step() {
    echo -e "${BLUE}[STEP]${NC} $*"
}

bark_notify() {
    local title="$1"
    local message="$2"
    local icon="${3:-https://cloud.vesper366.com/claude-ai-icon.png}"
    curl -s "https://api.day.app/${BARK_KEY}/${title}/${message}?icon=${icon}" >/dev/null 2>&1 || true
}

show_help() {
    cat <<EOF
Minervia Institute - VPS Deployment Script

Usage: $0 [options]

Options:
  --target TARGET      VPS target name (default: HZUS)
  --version VERSION    Deployment version tag (default: git commit hash)
  --skip-build         Skip Docker image build
  --help               Show this help message

Examples:
  $0 --target HZUS
  $0 --target HZUS --version v1.0.0
  $0 --target HZUS --skip-build

Deployment Steps:
  1. Pre-check: Verify environment and dependencies
  2. Version: Generate or use provided version tag
  3. Sync: Upload code to remote server
  4. Build: Build Docker images on remote server
  5. Deploy: Start services with docker compose
  6. Health Check: Verify all services are healthy

EOF
}

get_version() {
    if [[ -n "$VERSION" ]]; then
        echo "$VERSION"
        return
    fi

    # Try to get git commit hash
    if git rev-parse --git-dir > /dev/null 2>&1; then
        local commit_hash
        commit_hash=$(git rev-parse --short HEAD 2>/dev/null || echo "")
        if [[ -n "$commit_hash" ]]; then
            echo "$commit_hash"
            return
        fi
    fi

    # Fallback to timestamp
    date +"%Y%m%d-%H%M%S"
}

save_version() {
    local version="$1"
    local prev_version

    # Get previous version
    prev_version=$(bash "$VPS_SSH" -q --target "$TARGET" -- "cat ${REMOTE_DIR}/.deploy/current_version 2>/dev/null || echo ''")

    # Save versions
    bash "$VPS_SSH" -q --target "$TARGET" -- "
        mkdir -p ${REMOTE_DIR}/.deploy
        echo '${prev_version}' > ${REMOTE_DIR}/.deploy/previous_version
        echo '${version}' > ${REMOTE_DIR}/.deploy/current_version
        echo '$(date -u +"%Y-%m-%dT%H:%M:%SZ")' > ${REMOTE_DIR}/.deploy/last_deploy_time
    "

    log_info "Version saved: ${version} (previous: ${prev_version:-none})"
}

# =============================================================================
# Deployment Steps
# =============================================================================

step_precheck() {
    log_step "Step 1/6: Pre-check"

    # Check local dependencies
    if [[ ! -f "$VPS_SSH" ]]; then
        log_error "VPS SSH script not found: $VPS_SSH"
        exit 1
    fi

    if [[ ! -f "$VPS_RSYNC" ]]; then
        log_error "VPS RSYNC script not found: $VPS_RSYNC"
        exit 1
    fi

    # Check .env.prod exists
    if [[ ! -f "${PROJECT_DIR}/.env.prod" ]]; then
        log_error ".env.prod not found. Please create it from .env.prod.template"
        exit 1
    fi

    # Run environment check
    log_info "Checking remote environment..."
    if ! bash "${SCRIPT_DIR}/vps-check-env.sh" --target "$TARGET" >/dev/null 2>&1; then
        log_error "Environment check failed. Run: ./vps-check-env.sh --target $TARGET"
        exit 1
    fi

    log_info "Pre-check passed"
}

step_version() {
    log_step "Step 2/6: Version"

    VERSION=$(get_version)
    log_info "Deployment version: $VERSION"

    # Export version for docker compose
    export VERSION
}

step_sync() {
    log_step "Step 3/6: Sync code to remote"

    log_info "Syncing code to ${TARGET}:${REMOTE_DIR}..."

    # Sync code excluding unnecessary files
    bash "$VPS_RSYNC" -q --target "$TARGET" \
        --src "$PROJECT_DIR" \
        --dir "$REMOTE_DIR" \
        --exclude "node_modules" \
        --exclude ".git" \
        --exclude "logs" \
        --exclude "*.log" \
        --exclude ".next" \
        --exclude "build" \
        --exclude "dist" \
        --exclude "coverage" \
        --exclude ".env.example"

    log_info "Code synced successfully"
}

step_build() {
    log_step "Step 4/6: Build Docker images"

    if [[ "$SKIP_BUILD" -eq 1 ]]; then
        log_warn "Skipping build (--skip-build flag)"
        return
    fi

    log_info "Building Docker images on remote server..."

    bash "$VPS_SSH" -q --target "$TARGET" -- "
        cd ${REMOTE_DIR}
        export VERSION=${VERSION}
        docker compose -f docker-compose.prod.yml build --no-cache
    "

    log_info "Docker images built successfully"
}

step_deploy() {
    log_step "Step 5/6: Deploy services"

    log_info "Starting services with docker compose..."

    # Save version before deployment
    save_version "$VERSION"

    # Start services
    bash "$VPS_SSH" -q --target "$TARGET" -- "
        cd ${REMOTE_DIR}
        export VERSION=${VERSION}
        docker compose -f docker-compose.prod.yml --profile with-nginx up -d
    "

    log_info "Services started"
}

step_health_check() {
    log_step "Step 6/6: Health check"

    log_info "Waiting for services to be healthy..."

    local max_wait=120
    local waited=0

    while [[ $waited -lt $max_wait ]]; do
        local unhealthy
        unhealthy=$(bash "$VPS_SSH" -q --target "$TARGET" -- "
            cd ${REMOTE_DIR}
            docker compose -f docker-compose.prod.yml ps --format json | jq -r 'select(.Health == \"unhealthy\" or .Health == \"starting\") | .Name' | wc -l
        " 2>/dev/null || echo "999")

        if [[ "$unhealthy" == "0" ]]; then
            log_info "All services are healthy"
            return 0
        fi

        sleep 5
        ((waited+=5))
        echo -n "."
    done

    echo ""
    log_warn "Some services may not be fully healthy yet"

    # Show service status
    bash "$VPS_SSH" -q --target "$TARGET" -- "
        cd ${REMOTE_DIR}
        docker compose -f docker-compose.prod.yml ps
    "

    return 1
}

# =============================================================================
# Rollback on Failure
# =============================================================================

rollback_on_failure() {
    log_error "Deployment failed! Rolling back..."
    bark_notify "部署失败" "正在回滚到上一版本" "https://img.icons8.com/?size=100&id=HZhl4S0tHUuX&format=png&color=000000"

    if [[ -f "${SCRIPT_DIR}/vps-rollback.sh" ]]; then
        bash "${SCRIPT_DIR}/vps-rollback.sh" --target "$TARGET" --auto-confirm
    else
        log_error "Rollback script not found. Please rollback manually."
    fi

    exit 1
}

# =============================================================================
# Main
# =============================================================================

main() {
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --target)
                TARGET="$2"
                shift 2
                ;;
            --version)
                VERSION="$2"
                shift 2
                ;;
            --skip-build)
                SKIP_BUILD=1
                shift
                ;;
            --help|-h)
                show_help
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                show_help
                exit 1
                ;;
        esac
    done

    # Set trap for errors
    trap rollback_on_failure ERR

    echo ""
    echo "=========================================="
    echo "  Minervia Institute - VPS Deployment"
    echo "=========================================="
    echo ""
    echo "Target: $TARGET"
    echo "Remote Directory: $REMOTE_DIR"
    echo ""

    bark_notify "开始部署" "目标: ${TARGET}" "https://cloud.vesper366.com/claude-ai-icon.png"

    # Execute deployment steps
    step_precheck
    step_version
    step_sync
    step_build
    step_deploy

    if step_health_check; then
        echo ""
        log_info "Deployment completed successfully!"
        echo ""
        echo "Version: $VERSION"
        echo "Access URLs:"
        echo "  Frontend: https://yourdomain.com"
        echo "  Backend API: https://yourdomain.com/api"
        echo ""
        echo "Management:"
        echo "  View logs: ./vps-logs.sh --target $TARGET"
        echo "  Check status: ssh $TARGET 'cd $REMOTE_DIR && docker compose -f docker-compose.prod.yml ps'"
        echo "  Rollback: ./vps-rollback.sh --target $TARGET"
        echo ""

        bark_notify "部署成功" "版本: ${VERSION}" "https://img.icons8.com/?size=100&id=70yRC8npwT3d&format=png&color=000000"
    else
        log_warn "Deployment completed with warnings. Please check service status."
        bark_notify "部署完成" "存在警告-请检查" "https://img.icons8.com/?size=100&id=HZhl4S0tHUuX&format=png&color=000000"
    fi
}

main "$@"
