#!/usr/bin/env bash
#
# Minervia Institute - Management Script
# Provides status monitoring, port management, logs, and service control
#
# Usage:
#   minervia status              # Show service status
#   minervia ports               # Show port configuration
#   minervia logs [service]      # View logs
#   minervia start|stop|restart  # Service control
#   minervia backup|restore      # Data management
#

set -euo pipefail

# =============================================================================
# Configuration
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
BACKUP_DIR="${PROJECT_DIR}/backups"
ENV_FILE="${PROJECT_DIR}/.env"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# Service names
SERVICES=(mysql redis kafka backend frontend)

# =============================================================================
# Utility Functions
# =============================================================================

print_header() {
    echo ""
    echo -e "${BOLD}${CYAN}========================================${NC}"
    echo -e "${BOLD}${CYAN}  Minervia Institute - $1${NC}"
    echo -e "${BOLD}${CYAN}========================================${NC}"
    echo ""
}

print_row() {
    local label="$1"
    local value="$2"
    local status="${3:-}"

    printf "  %-20s " "$label"

    case "$status" in
        ok|running|healthy)
            echo -e "${GREEN}${value}${NC}"
            ;;
        warn|warning)
            echo -e "${YELLOW}${value}${NC}"
            ;;
        error|stopped|unhealthy)
            echo -e "${RED}${value}${NC}"
            ;;
        *)
            echo -e "$value"
            ;;
    esac
}

load_env() {
    if [[ -f "$ENV_FILE" ]]; then
        set -a
        source "$ENV_FILE"
        set +a
    fi
}

detect_deployment_mode() {
    if docker compose ps &>/dev/null 2>&1; then
        echo "docker"
    elif systemctl is-active minervia-backend &>/dev/null; then
        echo "systemd"
    else
        echo "unknown"
    fi
}

get_compose_file() {
    if [[ -f "${PROJECT_DIR}/docker-compose.prod.yml" ]]; then
        echo "${PROJECT_DIR}/docker-compose.prod.yml"
    else
        echo "${PROJECT_DIR}/docker-compose.yml"
    fi
}

# =============================================================================
# Status Command
# =============================================================================

cmd_status() {
    print_header "Service Status"

    local mode
    mode=$(detect_deployment_mode)

    echo -e "  ${BOLD}Deployment Mode:${NC} $mode"
    echo ""

    if [[ "$mode" == "docker" ]]; then
        status_docker
    elif [[ "$mode" == "systemd" ]]; then
        status_systemd
    else
        echo -e "  ${YELLOW}No deployment detected${NC}"
        echo "  Run ./scripts/deploy.sh to deploy the application"
        return 1
    fi

    echo ""
    echo -e "  ${BOLD}Resource Usage:${NC}"
    status_resources

    echo ""
    echo -e "  ${BOLD}Recent Errors:${NC}"
    status_errors
}

status_docker() {
    local compose_file
    compose_file=$(get_compose_file)

    echo -e "  ${BOLD}Services:${NC}"

    cd "$PROJECT_DIR"

    docker compose -f "$compose_file" ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null | while read -r line; do
        if [[ "$line" == *"running"* ]] || [[ "$line" == *"Up"* ]]; then
            echo -e "  ${GREEN}$line${NC}"
        elif [[ "$line" == *"unhealthy"* ]] || [[ "$line" == *"Exit"* ]]; then
            echo -e "  ${RED}$line${NC}"
        else
            echo "  $line"
        fi
    done
}

status_systemd() {
    echo -e "  ${BOLD}Services:${NC}"

    for service in minervia-backend minervia-frontend mysql redis; do
        local status
        if systemctl is-active "$service" &>/dev/null; then
            status="running"
            print_row "$service" "Running" "ok"
        else
            status="stopped"
            print_row "$service" "Stopped" "error"
        fi
    done
}

status_resources() {
    local mode
    mode=$(detect_deployment_mode)

    if [[ "$mode" == "docker" ]]; then
        docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}" 2>/dev/null | head -10 | while read -r line; do
            echo "  $line"
        done
    else
        # System resources
        local cpu_usage mem_usage disk_usage
        cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1 2>/dev/null || echo "N/A")
        mem_usage=$(free -m | awk 'NR==2{printf "%.1f%%", $3*100/$2}' 2>/dev/null || echo "N/A")
        disk_usage=$(df -h "$PROJECT_DIR" | awk 'NR==2{print $5}' 2>/dev/null || echo "N/A")

        print_row "CPU Usage" "$cpu_usage"
        print_row "Memory Usage" "$mem_usage"
        print_row "Disk Usage" "$disk_usage"
    fi
}

status_errors() {
    local mode
    mode=$(detect_deployment_mode)
    local errors_found=0

    if [[ "$mode" == "docker" ]]; then
        local compose_file
        compose_file=$(get_compose_file)

        cd "$PROJECT_DIR"

        # Check for recent errors in logs
        local error_count
        error_count=$(docker compose -f "$compose_file" logs --tail=100 2>/dev/null | grep -ci "error\|exception\|fatal" || echo "0")

        if [[ $error_count -gt 0 ]]; then
            echo -e "  ${YELLOW}Found ${error_count} error(s) in recent logs${NC}"
            echo "  Run 'minervia logs' to view details"
            errors_found=1
        fi
    fi

    if [[ $errors_found -eq 0 ]]; then
        echo -e "  ${GREEN}No recent errors${NC}"
    fi
}

# =============================================================================
# Ports Command
# =============================================================================

cmd_ports() {
    local subcommand="${1:-list}"
    shift || true

    case "$subcommand" in
        list|"")
            ports_list
            ;;
        set)
            ports_set "$@"
            ;;
        check)
            ports_check
            ;;
        *)
            echo "Usage: minervia ports [list|set|check]"
            echo ""
            echo "Commands:"
            echo "  list              Show current port configuration"
            echo "  set <svc> <port>  Change port for a service"
            echo "  check             Check for port conflicts"
            exit 1
            ;;
    esac
}

ports_list() {
    print_header "Port Configuration"

    load_env

    echo -e "  ${BOLD}Configured Ports:${NC}"
    echo ""
    printf "  %-15s %-10s %-10s %s\n" "SERVICE" "PORT" "STATUS" "DESCRIPTION"
    printf "  %-15s %-10s %-10s %s\n" "-------" "----" "------" "-----------"

    local ports=(
        "Frontend:${FRONTEND_PORT:-3000}:Next.js web application"
        "Backend:${BACKEND_PORT:-8080}:Spring Boot API server"
        "MySQL:${MYSQL_PORT:-3306}:Database (internal)"
        "Redis:${REDIS_PORT:-6379}:Cache (internal)"
        "Kafka:${KAFKA_PORT:-9092}:Message queue (internal)"
    )

    for entry in "${ports[@]}"; do
        IFS=':' read -r service port desc <<< "$entry"

        local status="free"
        local status_color="ok"

        if ss -tuln 2>/dev/null | grep -q ":${port} " || \
           netstat -tuln 2>/dev/null | grep -q ":${port} " || \
           lsof -i ":${port}" &>/dev/null; then
            status="in use"
            status_color="warn"
        fi

        printf "  %-15s " "$service"
        printf "%-10s " "$port"

        if [[ "$status" == "in use" ]]; then
            printf "${GREEN}%-10s${NC} " "$status"
        else
            printf "%-10s " "$status"
        fi

        echo "$desc"
    done

    echo ""
    echo -e "  ${BOLD}To change a port:${NC}"
    echo "  Edit .env file or run: minervia ports set <service> <port>"
}

ports_set() {
    local service="$1"
    local new_port="$2"

    if [[ -z "$service" || -z "$new_port" ]]; then
        echo "Usage: minervia ports set <service> <port>"
        echo "Example: minervia ports set frontend 3001"
        exit 1
    fi

    # Validate port number
    if ! [[ "$new_port" =~ ^[0-9]+$ ]] || [[ $new_port -lt 1 || $new_port -gt 65535 ]]; then
        echo -e "${RED}Error: Invalid port number${NC}"
        exit 1
    fi

    # Check if port is available
    if ss -tuln 2>/dev/null | grep -q ":${new_port} " || \
       lsof -i ":${new_port}" &>/dev/null; then
        echo -e "${YELLOW}Warning: Port ${new_port} is already in use${NC}"
        read -rp "Continue anyway? [y/N]: " confirm
        [[ "$confirm" =~ ^[Yy]$ ]] || exit 1
    fi

    # Map service to env variable
    local env_var
    case "${service,,}" in
        frontend) env_var="FRONTEND_PORT" ;;
        backend)  env_var="BACKEND_PORT" ;;
        mysql)    env_var="MYSQL_PORT" ;;
        redis)    env_var="REDIS_PORT" ;;
        kafka)    env_var="KAFKA_PORT" ;;
        *)
            echo -e "${RED}Error: Unknown service '${service}'${NC}"
            echo "Valid services: frontend, backend, mysql, redis, kafka"
            exit 1
            ;;
    esac

    # Update .env file
    if grep -q "^${env_var}=" "$ENV_FILE" 2>/dev/null; then
        sed -i.bak "s/^${env_var}=.*/${env_var}=${new_port}/" "$ENV_FILE"
    else
        echo "${env_var}=${new_port}" >> "$ENV_FILE"
    fi

    rm -f "${ENV_FILE}.bak"

    echo -e "${GREEN}Updated ${service} port to ${new_port}${NC}"
    echo ""
    echo "Restart services to apply changes:"
    echo "  minervia restart"
}

ports_check() {
    print_header "Port Conflict Check"

    load_env

    local conflicts=0
    local ports=(
        "${FRONTEND_PORT:-3000}:Frontend"
        "${BACKEND_PORT:-8080}:Backend"
        "${MYSQL_PORT:-3306}:MySQL"
        "${REDIS_PORT:-6379}:Redis"
    )

    for entry in "${ports[@]}"; do
        IFS=':' read -r port service <<< "$entry"

        echo -n "  Checking port ${port} (${service})... "

        if ss -tuln 2>/dev/null | grep -q ":${port} " || \
           lsof -i ":${port}" &>/dev/null; then

            # Check if it's our own service
            local process
            process=$(lsof -i ":${port}" 2>/dev/null | tail -1 | awk '{print $1}' || echo "unknown")

            if [[ "$process" == "docker" || "$process" == "java" || "$process" == "node" ]]; then
                echo -e "${GREEN}OK (${process})${NC}"
            else
                echo -e "${RED}CONFLICT (${process})${NC}"
                ((conflicts++))
            fi
        else
            echo -e "${GREEN}Available${NC}"
        fi
    done

    echo ""
    if [[ $conflicts -gt 0 ]]; then
        echo -e "  ${RED}Found ${conflicts} port conflict(s)${NC}"
        echo "  Use 'minervia ports set' to change conflicting ports"
        return 1
    else
        echo -e "  ${GREEN}No conflicts detected${NC}"
    fi
}

# =============================================================================
# Logs Command
# =============================================================================

cmd_logs() {
    local service="${1:-}"
    shift || true

    local follow=false
    local since=""
    local tail="100"

    while [[ $# -gt 0 ]]; do
        case "$1" in
            -f|--follow) follow=true ;;
            --since) since="$2"; shift ;;
            --tail) tail="$2"; shift ;;
            *) ;;
        esac
        shift
    done

    local mode
    mode=$(detect_deployment_mode)

    if [[ "$mode" == "docker" ]]; then
        logs_docker "$service" "$follow" "$since" "$tail"
    elif [[ "$mode" == "systemd" ]]; then
        logs_systemd "$service" "$follow" "$since"
    else
        echo -e "${RED}No deployment detected${NC}"
        exit 1
    fi
}

logs_docker() {
    local service="$1"
    local follow="$2"
    local since="$3"
    local tail="$4"

    local compose_file
    compose_file=$(get_compose_file)

    cd "$PROJECT_DIR"

    local cmd="docker compose -f $compose_file logs"

    [[ "$follow" == "true" ]] && cmd="$cmd -f"
    [[ -n "$since" ]] && cmd="$cmd --since $since"
    [[ -n "$tail" ]] && cmd="$cmd --tail $tail"
    [[ -n "$service" ]] && cmd="$cmd $service"

    eval "$cmd"
}

logs_systemd() {
    local service="$1"
    local follow="$2"
    local since="$3"

    local cmd="journalctl"

    if [[ -n "$service" ]]; then
        cmd="$cmd -u minervia-${service}"
    else
        cmd="$cmd -u 'minervia-*'"
    fi

    [[ "$follow" == "true" ]] && cmd="$cmd -f"
    [[ -n "$since" ]] && cmd="$cmd --since '$since'"

    eval "$cmd"
}

# =============================================================================
# Service Control Commands
# =============================================================================

cmd_start() {
    local service="${1:-}"

    print_header "Starting Services"

    local mode
    mode=$(detect_deployment_mode)

    if [[ "$mode" == "docker" ]]; then
        local compose_file
        compose_file=$(get_compose_file)
        cd "$PROJECT_DIR"

        if [[ -n "$service" ]]; then
            docker compose -f "$compose_file" start "$service"
        else
            docker compose -f "$compose_file" up -d
        fi
    elif [[ "$mode" == "systemd" ]]; then
        if [[ -n "$service" ]]; then
            sudo systemctl start "minervia-${service}"
        else
            sudo systemctl start minervia-backend minervia-frontend
        fi
    else
        echo -e "${RED}No deployment detected${NC}"
        exit 1
    fi

    echo -e "${GREEN}Services started${NC}"
}

cmd_stop() {
    local service="${1:-}"

    print_header "Stopping Services"

    local mode
    mode=$(detect_deployment_mode)

    if [[ "$mode" == "docker" ]]; then
        local compose_file
        compose_file=$(get_compose_file)
        cd "$PROJECT_DIR"

        if [[ -n "$service" ]]; then
            docker compose -f "$compose_file" stop "$service"
        else
            docker compose -f "$compose_file" down
        fi
    elif [[ "$mode" == "systemd" ]]; then
        if [[ -n "$service" ]]; then
            sudo systemctl stop "minervia-${service}"
        else
            sudo systemctl stop minervia-frontend minervia-backend
        fi
    else
        echo -e "${RED}No deployment detected${NC}"
        exit 1
    fi

    echo -e "${GREEN}Services stopped${NC}"
}

cmd_restart() {
    local service="${1:-}"

    print_header "Restarting Services"

    local mode
    mode=$(detect_deployment_mode)

    if [[ "$mode" == "docker" ]]; then
        local compose_file
        compose_file=$(get_compose_file)
        cd "$PROJECT_DIR"

        if [[ -n "$service" ]]; then
            docker compose -f "$compose_file" restart "$service"
        else
            docker compose -f "$compose_file" restart
        fi
    elif [[ "$mode" == "systemd" ]]; then
        if [[ -n "$service" ]]; then
            sudo systemctl restart "minervia-${service}"
        else
            sudo systemctl restart minervia-backend minervia-frontend
        fi
    else
        echo -e "${RED}No deployment detected${NC}"
        exit 1
    fi

    echo -e "${GREEN}Services restarted${NC}"
}

cmd_update() {
    print_header "Updating Application"

    local mode
    mode=$(detect_deployment_mode)

    if [[ "$mode" == "docker" ]]; then
        local compose_file
        compose_file=$(get_compose_file)
        cd "$PROJECT_DIR"

        echo "Pulling latest changes..."
        git pull

        echo "Rebuilding images..."
        docker compose -f "$compose_file" build --no-cache

        echo "Restarting services..."
        docker compose -f "$compose_file" up -d

        echo -e "${GREEN}Update complete${NC}"
    else
        echo "Manual update required for non-Docker deployments"
        echo "Steps:"
        echo "  1. git pull"
        echo "  2. ./gradlew bootJar (backend)"
        echo "  3. npm run build (frontend)"
        echo "  4. minervia restart"
    fi
}

# =============================================================================
# Backup/Restore Commands
# =============================================================================

cmd_backup() {
    local db_only=false

    while [[ $# -gt 0 ]]; do
        case "$1" in
            --db-only) db_only=true ;;
            list) backup_list; return ;;
            *) ;;
        esac
        shift
    done

    print_header "Creating Backup"

    mkdir -p "$BACKUP_DIR"

    local timestamp
    timestamp=$(date +%Y%m%d_%H%M%S)
    local backup_name="minervia_backup_${timestamp}"
    local backup_path="${BACKUP_DIR}/${backup_name}"

    mkdir -p "$backup_path"

    load_env

    # Database backup
    echo "Backing up database..."

    local mode
    mode=$(detect_deployment_mode)

    if [[ "$mode" == "docker" ]]; then
        docker exec minervia-mysql mysqldump -u minervia -p"${DB_PASSWORD}" minervia > "${backup_path}/database.sql"
    else
        mysqldump -u minervia -p"${DB_PASSWORD}" minervia > "${backup_path}/database.sql"
    fi

    echo -e "${GREEN}Database backup complete${NC}"

    if [[ "$db_only" == "false" ]]; then
        # Config backup
        echo "Backing up configuration..."
        cp "$ENV_FILE" "${backup_path}/.env" 2>/dev/null || true

        # Compress
        echo "Compressing backup..."
        cd "$BACKUP_DIR"
        tar -czf "${backup_name}.tar.gz" "$backup_name"
        rm -rf "$backup_name"

        echo -e "${GREEN}Backup saved to: ${BACKUP_DIR}/${backup_name}.tar.gz${NC}"
    else
        echo -e "${GREEN}Database backup saved to: ${backup_path}/database.sql${NC}"
    fi
}

cmd_restore() {
    local backup_file="$1"

    if [[ -z "$backup_file" ]]; then
        echo "Usage: minervia restore <backup_file>"
        echo ""
        echo "Available backups:"
        backup_list
        exit 1
    fi

    if [[ ! -f "$backup_file" && -f "${BACKUP_DIR}/${backup_file}" ]]; then
        backup_file="${BACKUP_DIR}/${backup_file}"
    fi

    if [[ ! -f "$backup_file" ]]; then
        echo -e "${RED}Backup file not found: ${backup_file}${NC}"
        exit 1
    fi

    print_header "Restoring from Backup"

    echo -e "${YELLOW}WARNING: This will overwrite current data!${NC}"
    read -rp "Continue? [y/N]: " confirm
    [[ "$confirm" =~ ^[Yy]$ ]] || exit 1

    local temp_dir
    temp_dir=$(mktemp -d)

    echo "Extracting backup..."
    tar -xzf "$backup_file" -C "$temp_dir"

    local backup_dir
    backup_dir=$(ls "$temp_dir")

    load_env

    # Restore database
    echo "Restoring database..."

    local mode
    mode=$(detect_deployment_mode)

    if [[ "$mode" == "docker" ]]; then
        docker exec -i minervia-mysql mysql -u minervia -p"${DB_PASSWORD}" minervia < "${temp_dir}/${backup_dir}/database.sql"
    else
        mysql -u minervia -p"${DB_PASSWORD}" minervia < "${temp_dir}/${backup_dir}/database.sql"
    fi

    # Cleanup
    rm -rf "$temp_dir"

    echo -e "${GREEN}Restore complete${NC}"
    echo "You may need to restart services: minervia restart"
}

backup_list() {
    echo ""
    echo "Available backups in ${BACKUP_DIR}:"
    echo ""

    if [[ -d "$BACKUP_DIR" ]]; then
        ls -lh "$BACKUP_DIR"/*.tar.gz 2>/dev/null || echo "  No backups found"
    else
        echo "  Backup directory does not exist"
    fi
}

# =============================================================================
# Help
# =============================================================================

show_help() {
    cat <<EOF
Minervia Institute - Management Script

Usage: minervia <command> [options]

Commands:
  status              Show service status and health
  ports [subcommand]  Port management
    list              Show current port configuration
    set <svc> <port>  Change port for a service
    check             Check for port conflicts
  logs [service]      View service logs
    -f, --follow      Follow log output
    --since <time>    Show logs since timestamp
    --tail <n>        Number of lines to show
  start [service]     Start services
  stop [service]      Stop services
  restart [service]   Restart services
  update              Update to latest version
  backup              Create backup
    --db-only         Only backup database
    list              List available backups
  restore <file>      Restore from backup
  help                Show this help message

Examples:
  minervia status
  minervia logs backend -f
  minervia ports set frontend 3001
  minervia restart backend
  minervia backup --db-only

EOF
}

# =============================================================================
# Main
# =============================================================================

main() {
    local command="${1:-help}"
    shift || true

    case "$command" in
        status)   cmd_status "$@" ;;
        ports)    cmd_ports "$@" ;;
        logs)     cmd_logs "$@" ;;
        start)    cmd_start "$@" ;;
        stop)     cmd_stop "$@" ;;
        restart)  cmd_restart "$@" ;;
        update)   cmd_update "$@" ;;
        backup)   cmd_backup "$@" ;;
        restore)  cmd_restore "$@" ;;
        help|--help|-h) show_help ;;
        *)
            echo -e "${RED}Unknown command: ${command}${NC}"
            echo "Run 'minervia help' for usage"
            exit 1
            ;;
    esac
}

main "$@"
