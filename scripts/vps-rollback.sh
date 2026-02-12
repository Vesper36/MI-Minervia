#!/usr/bin/env bash
#
# Minervia Institute - VPS Rollback Script
# Rolls back to previous deployment version
#
# Usage:
#   ./vps-rollback.sh [--target TARGET] [--auto-confirm] [--help]
#
# Examples:
#   ./vps-rollback.sh --target HZUS
#   ./vps-rollback.sh --target HZUS --auto-confirm
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

# Default values
TARGET="${VPS_SSH_TARGET:-HZUS}"
REMOTE_DIR="/opt/minervia"
AUTO_CONFIRM=0
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

bark_notify() {
    local title="$1"
    local message="$2"
    local icon="${3:-https://cloud.vesper366.com/claude-ai-icon.png}"
    curl -s "https://api.day.app/${BARK_KEY}/${title}/${message}?icon=${icon}" >/dev/null 2>&1 || true
}

show_help() {
    cat <<EOF
Minervia Institute - VPS Rollback Script

Usage: $0 [options]

Options:
  --target TARGET      VPS target name (default: HZUS)
  --auto-confirm       Skip confirmation prompt
  --help               Show this help message

Examples:
  $0 --target HZUS
  $0 --target HZUS --auto-confirm

Rollback Process:
  1. Read previous version from deployment metadata
  2. Confirm rollback with user (unless --auto-confirm)
  3. Restart services with previous version tag
  4. Verify services are healthy

EOF
}

get_versions() {
    local current previous

    current=$(bash "$VPS_SSH" -q --target "$TARGET" -- "cat ${REMOTE_DIR}/.deploy/current_version 2>/dev/null || echo ''")
    previous=$(bash "$VPS_SSH" -q --target "$TARGET" -- "cat ${REMOTE_DIR}/.deploy/previous_version 2>/dev/null || echo ''")

    echo "${current}:${previous}"
}

confirm_rollback() {
    local current="$1"
    local previous="$2"

    if [[ "$AUTO_CONFIRM" -eq 1 ]]; then
        return 0
    fi

    echo ""
    echo "=========================================="
    echo "  Rollback Confirmation"
    echo "=========================================="
    echo ""
    echo "Current Version:  $current"
    echo "Rollback To:      $previous"
    echo ""
    read -rp "Are you sure you want to rollback? [y/N]: " response

    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        log_info "Rollback cancelled"
        exit 0
    fi
}

perform_rollback() {
    local previous_version="$1"

    log_info "Rolling back to version: $previous_version"

    # Restart services with previous version
    bash "$VPS_SSH" -q --target "$TARGET" -- "
        cd ${REMOTE_DIR}
        export VERSION=${previous_version}
        docker compose -f docker-compose.prod.yml --profile with-nginx up -d --force-recreate
    "

    # Update version metadata
    bash "$VPS_SSH" -q --target "$TARGET" -- "
        echo '${previous_version}' > ${REMOTE_DIR}/.deploy/current_version
        echo '' > ${REMOTE_DIR}/.deploy/previous_version
        echo '$(date -u +"%Y-%m-%dT%H:%M:%SZ")' > ${REMOTE_DIR}/.deploy/last_rollback_time
    "

    log_info "Rollback completed"
}

check_health() {
    log_info "Checking service health..."

    local max_wait=60
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
    log_warn "Some services may not be fully healthy"
    return 1
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
            --auto-confirm)
                AUTO_CONFIRM=1
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

    # Check VPS SSH script
    if [[ ! -f "$VPS_SSH" ]]; then
        log_error "VPS SSH script not found: $VPS_SSH"
        exit 1
    fi

    echo ""
    echo "=========================================="
    echo "  Minervia Institute - VPS Rollback"
    echo "=========================================="
    echo ""

    # Get versions
    local current_version previous_version
    IFS=':' read -r current_version previous_version <<< "$(get_versions)"

    if [[ -z "$previous_version" ]]; then
        log_error "No previous version found. Cannot rollback."
        exit 1
    fi

    # Confirm rollback
    confirm_rollback "$current_version" "$previous_version"

    bark_notify "开始回滚" "从 ${current_version} 回滚到 ${previous_version}" "https://img.icons8.com/?size=100&id=HZhl4S0tHUuX&format=png&color=000000"

    # Perform rollback
    perform_rollback "$previous_version"

    # Check health
    if check_health; then
        echo ""
        log_info "Rollback successful!"
        echo ""
        echo "Rolled back from: $current_version"
        echo "Current version:  $previous_version"
        echo ""

        bark_notify "回滚成功" "当前版本: ${previous_version}" "https://img.icons8.com/?size=100&id=70yRC8npwT3d&format=png&color=000000"
    else
        log_warn "Rollback completed with warnings. Please check service status."
        bark_notify "回滚完成" "存在警告-请检查" "https://img.icons8.com/?size=100&id=HZhl4S0tHUuX&format=png&color=000000"
    fi
}

main "$@"
