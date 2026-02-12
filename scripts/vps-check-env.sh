#!/usr/bin/env bash
#
# Minervia Institute - VPS Environment Check Script
# Checks remote VPS environment for deployment readiness
#
# Usage:
#   ./vps-check-env.sh [--target TARGET] [--json] [--help]
#
# Examples:
#   ./vps-check-env.sh --target HZUS
#   ./vps-check-env.sh --target HZUS --json > report.json
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
NC='\033[0m' # No Color

# VPS SSH wrapper
VPS_SSH="${HOME}/.agents/skills/vps-ssh-ops/scripts/vps-ssh.sh"

# Default values
TARGET="${VPS_SSH_TARGET:-HZUS}"
JSON_OUTPUT=0

# =============================================================================
# Utility Functions
# =============================================================================

log_info() {
    [[ "$JSON_OUTPUT" -eq 1 ]] && return
    echo -e "${GREEN}[INFO]${NC} $*" >&2
}

log_warn() {
    [[ "$JSON_OUTPUT" -eq 1 ]] && return
    echo -e "${YELLOW}[WARN]${NC} $*" >&2
}

log_error() {
    [[ "$JSON_OUTPUT" -eq 1 ]] && return
    echo -e "${RED}[ERROR]${NC} $*" >&2
}

show_help() {
    cat <<EOF
Minervia Institute - VPS Environment Check Script

Usage: $0 [options]

Options:
  --target TARGET    VPS target name (default: HZUS)
  --json             Output results in JSON format
  --help             Show this help message

Examples:
  $0 --target HZUS
  $0 --target HZUS --json > report.json

Environment Variables:
  VPS_SSH_TARGET     Default VPS target name

EOF
}

# =============================================================================
# Check Functions
# =============================================================================

check_docker() {
    local result
    result=$(bash "$VPS_SSH" -q --target "$TARGET" -- 'docker version --format "{{.Server.Version}}" 2>/dev/null || echo "NOT_INSTALLED"')

    if [[ "$result" == "NOT_INSTALLED" ]]; then
        echo "NOT_INSTALLED"
        return 1
    fi

    echo "$result"
    return 0
}

check_docker_compose() {
    local result
    result=$(bash "$VPS_SSH" -q --target "$TARGET" -- 'docker compose version --short 2>/dev/null || echo "NOT_INSTALLED"')

    if [[ "$result" == "NOT_INSTALLED" ]]; then
        echo "NOT_INSTALLED"
        return 1
    fi

    echo "$result"
    return 0
}

check_system_resources() {
    local cpu_cores mem_gb disk_gb

    # CPU cores
    cpu_cores=$(bash "$VPS_SSH" -q --target "$TARGET" -- 'nproc 2>/dev/null || echo "0"')

    # Memory in GB
    mem_gb=$(bash "$VPS_SSH" -q --target "$TARGET" -- 'free -g | awk "/^Mem:/ {print \$2}"')

    # Disk space in GB
    disk_gb=$(bash "$VPS_SSH" -q --target "$TARGET" -- 'df -BG /opt | tail -1 | awk "{print \$4}" | tr -d "G"')

    echo "${cpu_cores}:${mem_gb}:${disk_gb}"
}

check_ports() {
    local ports="80 443"
    local occupied=""

    for port in $ports; do
        local result
        result=$(bash "$VPS_SSH" -q --target "$TARGET" -- "ss -tuln 2>/dev/null | grep -q \":${port} \" && echo \"OCCUPIED\" || echo \"FREE\"")

        if [[ "$result" == "OCCUPIED" ]]; then
            occupied="${occupied}${port},"
        fi
    done

    echo "${occupied%,}"
}

check_required_software() {
    local software="git curl openssl"
    local missing=""

    for cmd in $software; do
        local result
        result=$(bash "$VPS_SSH" -q --target "$TARGET" -- "command -v $cmd >/dev/null 2>&1 && echo \"INSTALLED\" || echo \"MISSING\"")

        if [[ "$result" == "MISSING" ]]; then
            missing="${missing}${cmd},"
        fi
    done

    echo "${missing%,}"
}

check_deployment_directory() {
    local result
    result=$(bash "$VPS_SSH" -q --target "$TARGET" -- 'test -d /opt/minervia && echo "EXISTS" || echo "NOT_EXISTS"')
    echo "$result"
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
            --json)
                JSON_OUTPUT=1
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

    # Check if VPS SSH script exists
    if [[ ! -f "$VPS_SSH" ]]; then
        log_error "VPS SSH script not found: $VPS_SSH"
        exit 1
    fi

    log_info "Checking VPS environment: $TARGET"

    # Perform checks
    local docker_version docker_compose_version
    local cpu_cores mem_gb disk_gb
    local occupied_ports missing_software deployment_dir

    docker_version=$(check_docker || echo "NOT_INSTALLED")
    docker_compose_version=$(check_docker_compose || echo "NOT_INSTALLED")

    IFS=':' read -r cpu_cores mem_gb disk_gb <<< "$(check_system_resources)"

    occupied_ports=$(check_ports)
    missing_software=$(check_required_software)
    deployment_dir=$(check_deployment_directory)

    # Calculate status
    local status="READY"
    local warnings=0
    local errors=0

    if [[ "$docker_version" == "NOT_INSTALLED" ]]; then
        ((errors++))
        status="NOT_READY"
    fi

    if [[ "$docker_compose_version" == "NOT_INSTALLED" ]]; then
        ((errors++))
        status="NOT_READY"
    fi

    if [[ "$cpu_cores" -lt 2 ]]; then
        ((warnings++))
    fi

    if [[ "$mem_gb" -lt 4 ]]; then
        ((warnings++))
    fi

    if [[ "$disk_gb" -lt 20 ]]; then
        ((warnings++))
    fi

    if [[ -n "$missing_software" ]]; then
        ((warnings++))
    fi

    # Output results
    if [[ "$JSON_OUTPUT" -eq 1 ]]; then
        cat <<EOF
{
  "target": "$TARGET",
  "timestamp": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "status": "$status",
  "errors": $errors,
  "warnings": $warnings,
  "docker": {
    "version": "$docker_version",
    "installed": $([ "$docker_version" != "NOT_INSTALLED" ] && echo "true" || echo "false")
  },
  "docker_compose": {
    "version": "$docker_compose_version",
    "installed": $([ "$docker_compose_version" != "NOT_INSTALLED" ] && echo "true" || echo "false")
  },
  "system": {
    "cpu_cores": $cpu_cores,
    "memory_gb": $mem_gb,
    "disk_available_gb": $disk_gb
  },
  "ports": {
    "occupied": [$(echo "$occupied_ports" | sed 's/,/", "/g' | sed 's/^/"/;s/$/"/' | sed 's/""//g')]
  },
  "software": {
    "missing": [$(echo "$missing_software" | sed 's/,/", "/g' | sed 's/^/"/;s/$/"/' | sed 's/""//g')]
  },
  "deployment": {
    "directory_exists": $([ "$deployment_dir" == "EXISTS" ] && echo "true" || echo "false")
  }
}
EOF
    else
        echo ""
        echo "=========================================="
        echo "  VPS Environment Check Report"
        echo "=========================================="
        echo ""
        echo "Target: $TARGET"
        echo "Status: $status"
        echo ""
        echo "Docker:"
        echo "  Version: $docker_version"
        echo ""
        echo "Docker Compose:"
        echo "  Version: $docker_compose_version"
        echo ""
        echo "System Resources:"
        echo "  CPU Cores: $cpu_cores $([ "$cpu_cores" -lt 2 ] && echo "(⚠️  recommended: >= 2)" || echo "(✓)")"
        echo "  Memory: ${mem_gb}GB $([ "$mem_gb" -lt 4 ] && echo "(⚠️  recommended: >= 4GB)" || echo "(✓)")"
        echo "  Disk Available: ${disk_gb}GB $([ "$disk_gb" -lt 20 ] && echo "(⚠️  recommended: >= 20GB)" || echo "(✓)")"
        echo ""
        echo "Ports:"
        if [[ -n "$occupied_ports" ]]; then
            echo "  Occupied: $occupied_ports (⚠️  may conflict)"
        else
            echo "  All required ports available (✓)"
        fi
        echo ""
        echo "Required Software:"
        if [[ -n "$missing_software" ]]; then
            echo "  Missing: $missing_software (⚠️)"
        else
            echo "  All required software installed (✓)"
        fi
        echo ""
        echo "Deployment Directory:"
        echo "  /opt/minervia: $([ "$deployment_dir" == "EXISTS" ] && echo "exists (✓)" || echo "not exists (will be created)")"
        echo ""
        echo "Summary:"
        echo "  Errors: $errors"
        echo "  Warnings: $warnings"
        echo ""

        if [[ "$status" == "READY" ]]; then
            log_info "Environment is ready for deployment!"
        else
            log_error "Environment is NOT ready for deployment. Please fix the errors above."
            exit 1
        fi
    fi
}

main "$@"
