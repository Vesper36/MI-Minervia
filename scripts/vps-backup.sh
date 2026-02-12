#!/usr/bin/env bash
#
# Minervia Institute - VPS Backup Script
# Backs up MySQL database from remote VPS
#
# Usage:
#   ./vps-backup.sh [--target TARGET] [--setup] [--now] [--help]
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VPS_SSH="${HOME}/.agents/skills/vps-ssh-ops/scripts/vps-ssh.sh"
TARGET="${VPS_SSH_TARGET:-HZUS}"
REMOTE_BACKUP_DIR="/opt/minervia/backups"
BARK_KEY="Hbyb4zFPsxcQNNdnZSe7Sn"

log_info() { echo -e "\033[0;32m[INFO]\033[0m $*"; }
log_error() { echo -e "\033[0;31m[ERROR]\033[0m $*"; }

show_help() {
    cat <<EOF
Minervia Institute - VPS Backup Script

Usage: $0 [--target TARGET] [--setup] [--now] [--help]

Options:
  --target TARGET      VPS target name (default: HZUS)
  --setup              Setup cron job for automatic backups
  --now                Run backup immediately
  --help               Show this help message
EOF
}

create_backup() {
    local timestamp=$(date +"%Y%m%d_%H%M%S")
    local backup_file="minervia_${timestamp}.sql.gz"

    log_info "Creating backup: $backup_file"

    bash "$VPS_SSH" -q --target "$TARGET" -- "
        set -e
        mkdir -p ${REMOTE_BACKUP_DIR}
        cd /opt/minervia
        DB_PASSWORD=\$(grep '^DB_PASSWORD=' .env.prod | cut -d'=' -f2)
        MYSQL_ROOT_PASSWORD=\$(grep '^MYSQL_ROOT_PASSWORD=' .env.prod | cut -d'=' -f2)
        docker exec minervia-mysql mysqldump -u root -p\"\${MYSQL_ROOT_PASSWORD}\" --single-transaction minervia | gzip > ${REMOTE_BACKUP_DIR}/${backup_file}
        gzip -t ${REMOTE_BACKUP_DIR}/${backup_file}
        echo \"Backup created: ${backup_file}\"
    "

    echo "$backup_file"
}

setup_cron() {
    log_info "Setting up cron job..."
    bash "$VPS_SSH" -q --target "$TARGET" -- "
        cat > /usr/local/bin/minervia-backup.sh <<'EOF'
#!/bin/bash
set -e
TIMESTAMP=\$(date +\"%Y%m%d_%H%M%S\")
BACKUP_FILE=\"minervia_\${TIMESTAMP}.sql.gz\"
mkdir -p ${REMOTE_BACKUP_DIR}
cd /opt/minervia
MYSQL_ROOT_PASSWORD=\$(grep '^MYSQL_ROOT_PASSWORD=' .env.prod | cut -d'=' -f2)
docker exec minervia-mysql mysqldump -u root -p\"\${MYSQL_ROOT_PASSWORD}\" --single-transaction minervia | gzip > ${REMOTE_BACKUP_DIR}/\${BACKUP_FILE}
find ${REMOTE_BACKUP_DIR} -name 'minervia_*.sql.gz' -mtime +7 -delete
EOF
        chmod +x /usr/local/bin/minervia-backup.sh
        (crontab -l 2>/dev/null | grep -v 'minervia-backup.sh'; echo '0 2 * * * /usr/local/bin/minervia-backup.sh') | crontab -
    "
    log_info "Cron job setup completed (daily at 2:00 AM)"
}

main() {
    local SETUP=0 RUN_NOW=0
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --target) TARGET="$2"; shift 2 ;;
            --setup) SETUP=1; shift ;;
            --now) RUN_NOW=1; shift ;;
            --help|-h) show_help; exit 0 ;;
            *) log_error "Unknown option: $1"; exit 1 ;;
        esac
    done

    [[ "$SETUP" -eq 1 ]] && { setup_cron; exit 0; }
    [[ "$RUN_NOW" -eq 1 ]] && { create_backup; exit 0; }

    show_help
    exit 1
}

main "$@"
