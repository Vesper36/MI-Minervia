#!/usr/bin/env bash
#
# Minervia Institute - VPS Restore Script
# Restores MySQL database from backup
#
# Usage:
#   ./vps-restore.sh [--target TARGET] [--file BACKUP_FILE] [--help]
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VPS_SSH="${HOME}/.agents/skills/vps-ssh-ops/scripts/vps-ssh.sh"
TARGET="${VPS_SSH_TARGET:-HZUS}"
REMOTE_BACKUP_DIR="/opt/minervia/backups"
BACKUP_FILE=""

log_info() { echo -e "\033[0;32m[INFO]\033[0m $*"; }
log_warn() { echo -e "\033[1;33m[WARN]\033[0m $*"; }
log_error() { echo -e "\033[0;31m[ERROR]\033[0m $*"; }

show_help() {
    cat <<EOF
Minervia Institute - VPS Restore Script

Usage: $0 --file BACKUP_FILE [--target TARGET] [--help]

Options:
  --target TARGET      VPS target name (default: HZUS)
  --file FILE          Backup file name (e.g., minervia_20260212_020000.sql.gz)
  --help               Show this help message

Examples:
  $0 --file minervia_20260212_020000.sql.gz
  $0 --target HZUS --file minervia_20260212_020000.sql.gz

WARNING: This will replace the current database. Make sure to backup first!
EOF
}

list_backups() {
    log_info "Available backups on remote server:"
    bash "$VPS_SSH" -q --target "$TARGET" -- "ls -lh ${REMOTE_BACKUP_DIR}/minervia_*.sql.gz 2>/dev/null || echo 'No backups found'"
}

restore_backup() {
    local backup_file="$1"

    log_warn "This will REPLACE the current database!"
    read -rp "Are you sure you want to continue? [y/N]: " response

    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        log_info "Restore cancelled"
        exit 0
    fi

    log_info "Restoring from: $backup_file"

    bash "$VPS_SSH" -q --target "$TARGET" -- "
        set -e
        cd /opt/minervia
        MYSQL_ROOT_PASSWORD=\$(grep '^MYSQL_ROOT_PASSWORD=' .env.prod | cut -d'=' -f2)

        # Verify backup file exists
        if [[ ! -f ${REMOTE_BACKUP_DIR}/${backup_file} ]]; then
            echo 'Backup file not found'
            exit 1
        fi

        # Restore database
        gunzip -c ${REMOTE_BACKUP_DIR}/${backup_file} | docker exec -i minervia-mysql mysql -u root -p\"\${MYSQL_ROOT_PASSWORD}\" minervia

        echo 'Database restored successfully'
    "

    log_info "Restore completed successfully!"
}

main() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --target) TARGET="$2"; shift 2 ;;
            --file) BACKUP_FILE="$2"; shift 2 ;;
            --help|-h) show_help; exit 0 ;;
            *) log_error "Unknown option: $1"; exit 1 ;;
        esac
    done

    if [[ -z "$BACKUP_FILE" ]]; then
        list_backups
        echo ""
        log_error "Please specify a backup file with --file"
        exit 1
    fi

    restore_backup "$BACKUP_FILE"
}

main "$@"
