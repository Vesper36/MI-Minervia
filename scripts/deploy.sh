#!/usr/bin/env bash
#
# Minervia Institute - Deployment Script
# Supports: Docker deployment and manual installation on Debian/Ubuntu
#
# Usage:
#   ./deploy.sh                    # Interactive mode
#   ./deploy.sh docker             # Docker deployment
#   ./deploy.sh manual             # Manual installation
#   ./deploy.sh --help             # Show help
#

set -euo pipefail

# =============================================================================
# Configuration
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
LOG_FILE="${PROJECT_DIR}/deploy.log"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Required versions
DOCKER_MIN_VERSION="24.0"
DOCKER_COMPOSE_MIN_VERSION="2.20"
JAVA_MIN_VERSION="21"
NODE_MIN_VERSION="20"
MYSQL_MIN_VERSION="8.0"

# Default ports
DEFAULT_FRONTEND_PORT=3000
DEFAULT_BACKEND_PORT=8080
DEFAULT_MYSQL_PORT=3306
DEFAULT_REDIS_PORT=6379
DEFAULT_KAFKA_PORT=9092

# =============================================================================
# Utility Functions
# =============================================================================

log() {
    local level="$1"
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo -e "${timestamp} [${level}] ${message}" >> "$LOG_FILE"

    case "$level" in
        INFO)  echo -e "${GREEN}[INFO]${NC} ${message}" ;;
        WARN)  echo -e "${YELLOW}[WARN]${NC} ${message}" ;;
        ERROR) echo -e "${RED}[ERROR]${NC} ${message}" ;;
        DEBUG) [[ "${DEBUG:-false}" == "true" ]] && echo -e "${BLUE}[DEBUG]${NC} ${message}" ;;
    esac
}

die() {
    log ERROR "$1"
    exit 1
}

confirm() {
    local prompt="$1"
    local default="${2:-n}"

    if [[ "$default" == "y" ]]; then
        prompt="${prompt} [Y/n]: "
    else
        prompt="${prompt} [y/N]: "
    fi

    read -rp "$prompt" response
    response="${response:-$default}"
    [[ "$response" =~ ^[Yy]$ ]]
}

version_gte() {
    # Returns 0 if $1 >= $2
    printf '%s\n%s\n' "$2" "$1" | sort -V -C
}

check_command() {
    command -v "$1" &> /dev/null
}

# =============================================================================
# System Detection
# =============================================================================

detect_os() {
    if [[ -f /etc/os-release ]]; then
        . /etc/os-release
        OS_ID="${ID}"
        OS_VERSION="${VERSION_ID}"
        OS_NAME="${PRETTY_NAME}"
    elif [[ "$(uname)" == "Darwin" ]]; then
        OS_ID="macos"
        OS_VERSION="$(sw_vers -productVersion)"
        OS_NAME="macOS ${OS_VERSION}"
    else
        OS_ID="unknown"
        OS_VERSION="unknown"
        OS_NAME="Unknown OS"
    fi

    log INFO "Detected OS: ${OS_NAME}"
}

check_system_requirements() {
    log INFO "Checking system requirements..."

    local warnings=0

    # CPU cores
    local cpu_cores
    if [[ "$OS_ID" == "macos" ]]; then
        cpu_cores=$(sysctl -n hw.ncpu)
    else
        cpu_cores=$(nproc)
    fi

    if [[ $cpu_cores -lt 2 ]]; then
        log WARN "CPU cores: ${cpu_cores} (recommended: >= 2)"
        ((warnings++))
    else
        log INFO "CPU cores: ${cpu_cores}"
    fi

    # Memory
    local mem_gb
    if [[ "$OS_ID" == "macos" ]]; then
        mem_gb=$(( $(sysctl -n hw.memsize) / 1024 / 1024 / 1024 ))
    else
        mem_gb=$(( $(grep MemTotal /proc/meminfo | awk '{print $2}') / 1024 / 1024 ))
    fi

    if [[ $mem_gb -lt 4 ]]; then
        log WARN "Memory: ${mem_gb}GB (recommended: >= 4GB)"
        ((warnings++))
    else
        log INFO "Memory: ${mem_gb}GB"
    fi

    # Disk space
    local disk_gb
    disk_gb=$(df -BG "$PROJECT_DIR" | tail -1 | awk '{print $4}' | tr -d 'G')

    if [[ $disk_gb -lt 20 ]]; then
        log WARN "Available disk space: ${disk_gb}GB (recommended: >= 20GB)"
        ((warnings++))
    else
        log INFO "Available disk space: ${disk_gb}GB"
    fi

    if [[ $warnings -gt 0 ]]; then
        log WARN "System does not meet all recommended requirements"
        if ! confirm "Continue anyway?"; then
            exit 1
        fi
    fi
}

check_port_available() {
    local port="$1"
    local service="$2"

    if ss -tuln 2>/dev/null | grep -q ":${port} " || \
       netstat -tuln 2>/dev/null | grep -q ":${port} " || \
       lsof -i ":${port}" &>/dev/null; then
        log WARN "Port ${port} (${service}) is already in use"
        return 1
    fi
    return 0
}

check_all_ports() {
    log INFO "Checking port availability..."

    local conflicts=0

    check_port_available "$DEFAULT_FRONTEND_PORT" "Frontend" || ((conflicts++))
    check_port_available "$DEFAULT_BACKEND_PORT" "Backend" || ((conflicts++))
    check_port_available "$DEFAULT_MYSQL_PORT" "MySQL" || ((conflicts++))
    check_port_available "$DEFAULT_REDIS_PORT" "Redis" || ((conflicts++))

    if [[ $conflicts -gt 0 ]]; then
        log WARN "${conflicts} port conflict(s) detected"
        log INFO "You can customize ports in .env file"
        if ! confirm "Continue anyway?"; then
            exit 1
        fi
    else
        log INFO "All default ports are available"
    fi
}

# =============================================================================
# Docker Deployment
# =============================================================================

check_docker() {
    log INFO "Checking Docker installation..."

    if ! check_command docker; then
        log ERROR "Docker is not installed"
        log INFO "Install Docker: https://docs.docker.com/engine/install/"
        case "$OS_ID" in
            debian|ubuntu)
                log INFO "Quick install: curl -fsSL https://get.docker.com | sh"
                ;;
        esac
        return 1
    fi

    local docker_version
    docker_version=$(docker version --format '{{.Server.Version}}' 2>/dev/null || echo "0.0")

    if ! version_gte "$docker_version" "$DOCKER_MIN_VERSION"; then
        log ERROR "Docker version ${docker_version} is too old (required: >= ${DOCKER_MIN_VERSION})"
        return 1
    fi

    log INFO "Docker version: ${docker_version}"

    # Check Docker Compose
    local compose_version
    if docker compose version &>/dev/null; then
        compose_version=$(docker compose version --short 2>/dev/null || echo "0.0")
    elif check_command docker-compose; then
        compose_version=$(docker-compose version --short 2>/dev/null || echo "0.0")
    else
        log ERROR "Docker Compose is not installed"
        return 1
    fi

    if ! version_gte "$compose_version" "$DOCKER_COMPOSE_MIN_VERSION"; then
        log ERROR "Docker Compose version ${compose_version} is too old (required: >= ${DOCKER_COMPOSE_MIN_VERSION})"
        return 1
    fi

    log INFO "Docker Compose version: ${compose_version}"

    # Check if Docker daemon is running
    if ! docker info &>/dev/null; then
        log ERROR "Docker daemon is not running"
        log INFO "Start Docker: sudo systemctl start docker"
        return 1
    fi

    return 0
}

setup_env_file() {
    log INFO "Setting up environment configuration..."

    local env_file="${PROJECT_DIR}/.env"
    local env_example="${PROJECT_DIR}/.env.example"

    if [[ -f "$env_file" ]]; then
        log INFO "Found existing .env file"
        if confirm "Use existing .env file?"; then
            return 0
        fi
        cp "$env_file" "${env_file}.backup.$(date +%Y%m%d%H%M%S)"
        log INFO "Backed up existing .env file"
    fi

    if [[ ! -f "$env_example" ]]; then
        die ".env.example not found"
    fi

    cp "$env_example" "$env_file"

    # Generate JWT secret
    local jwt_secret
    jwt_secret=$(openssl rand -base64 32 2>/dev/null || head -c 32 /dev/urandom | base64)
    sed -i.bak "s/your_256_bit_secret_key_here_minimum_32_characters/${jwt_secret}/" "$env_file"

    # Generate database password
    local db_password
    db_password=$(openssl rand -base64 16 2>/dev/null || head -c 16 /dev/urandom | base64)
    sed -i.bak "s/your_secure_database_password_here/${db_password}/" "$env_file"

    # Generate MySQL root password
    local mysql_root_password
    mysql_root_password=$(openssl rand -base64 16 2>/dev/null || head -c 16 /dev/urandom | base64)
    sed -i.bak "s/root_password_change_in_production/${mysql_root_password}/" "$env_file"

    rm -f "${env_file}.bak"

    log INFO "Generated secure passwords in .env file"
    log WARN "Please configure email settings in .env file before starting"

    if confirm "Edit .env file now?"; then
        ${EDITOR:-nano} "$env_file"
    fi
}

deploy_docker() {
    log INFO "Starting Docker deployment..."

    check_docker || die "Docker requirements not met"
    check_all_ports
    setup_env_file

    cd "$PROJECT_DIR"

    # Choose compose file
    local compose_file
    if confirm "Use production configuration?" "y"; then
        compose_file="docker-compose.prod.yml"
    else
        compose_file="docker-compose.yml"
    fi

    log INFO "Using ${compose_file}"

    # Build images
    log INFO "Building Docker images..."
    docker compose -f "$compose_file" build --no-cache

    # Start services
    log INFO "Starting services..."
    docker compose -f "$compose_file" up -d

    # Wait for health checks
    log INFO "Waiting for services to be healthy..."
    local max_wait=120
    local waited=0

    while [[ $waited -lt $max_wait ]]; do
        if docker compose -f "$compose_file" ps | grep -q "unhealthy\|starting"; then
            sleep 5
            ((waited+=5))
            echo -n "."
        else
            echo ""
            break
        fi
    done

    if [[ $waited -ge $max_wait ]]; then
        log WARN "Some services may not be fully healthy yet"
        docker compose -f "$compose_file" ps
    fi

    # Show status
    log INFO "Deployment complete!"
    echo ""
    echo "=========================================="
    echo "  Minervia Institute - Deployment Status"
    echo "=========================================="
    docker compose -f "$compose_file" ps
    echo ""
    echo "Access URLs:"
    echo "  Frontend: http://localhost:${FRONTEND_PORT:-3000}"
    echo "  Backend:  http://localhost:${BACKEND_PORT:-8080}"
    echo "  API Docs: http://localhost:${BACKEND_PORT:-8080}/swagger-ui.html"
    echo ""
    echo "Management:"
    echo "  View logs:    docker compose -f ${compose_file} logs -f"
    echo "  Stop:         docker compose -f ${compose_file} down"
    echo "  Restart:      docker compose -f ${compose_file} restart"
    echo ""
}

# =============================================================================
# Manual Installation
# =============================================================================

install_java() {
    log INFO "Installing Java ${JAVA_MIN_VERSION}..."

    case "$OS_ID" in
        debian|ubuntu)
            sudo apt-get update
            sudo apt-get install -y openjdk-21-jdk
            ;;
        *)
            die "Unsupported OS for automatic Java installation"
            ;;
    esac
}

install_nodejs() {
    log INFO "Installing Node.js ${NODE_MIN_VERSION}..."

    case "$OS_ID" in
        debian|ubuntu)
            curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
            sudo apt-get install -y nodejs
            ;;
        *)
            die "Unsupported OS for automatic Node.js installation"
            ;;
    esac
}

install_mysql() {
    log INFO "Installing MySQL ${MYSQL_MIN_VERSION}..."

    case "$OS_ID" in
        debian|ubuntu)
            sudo apt-get update
            sudo apt-get install -y mysql-server
            sudo systemctl enable mysql
            sudo systemctl start mysql
            ;;
        *)
            die "Unsupported OS for automatic MySQL installation"
            ;;
    esac
}

install_redis() {
    log INFO "Installing Redis..."

    case "$OS_ID" in
        debian|ubuntu)
            sudo apt-get update
            sudo apt-get install -y redis-server
            sudo systemctl enable redis-server
            sudo systemctl start redis-server
            ;;
        *)
            die "Unsupported OS for automatic Redis installation"
            ;;
    esac
}

setup_database() {
    log INFO "Setting up database..."

    local db_password
    read -rsp "Enter database password for 'minervia' user: " db_password
    echo ""

    sudo mysql -e "CREATE DATABASE IF NOT EXISTS minervia CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    sudo mysql -e "CREATE USER IF NOT EXISTS 'minervia'@'localhost' IDENTIFIED BY '${db_password}';"
    sudo mysql -e "GRANT ALL PRIVILEGES ON minervia.* TO 'minervia'@'localhost';"
    sudo mysql -e "FLUSH PRIVILEGES;"

    log INFO "Database setup complete"
}

build_backend() {
    log INFO "Building backend..."

    cd "${PROJECT_DIR}/backend"

    if [[ ! -f gradlew ]]; then
        die "gradlew not found in backend directory"
    fi

    chmod +x gradlew
    ./gradlew bootJar --no-daemon

    log INFO "Backend build complete"
}

build_frontend() {
    log INFO "Building frontend..."

    cd "${PROJECT_DIR}/frontend"

    npm ci
    npm run build

    log INFO "Frontend build complete"
}

create_systemd_services() {
    log INFO "Creating systemd services..."

    # Backend service
    sudo tee /etc/systemd/system/minervia-backend.service > /dev/null <<EOF
[Unit]
Description=Minervia Backend Service
After=network.target mysql.service redis.service

[Service]
Type=simple
User=www-data
WorkingDirectory=${PROJECT_DIR}/backend
ExecStart=/usr/bin/java -jar ${PROJECT_DIR}/backend/build/libs/*.jar
Restart=always
RestartSec=10
Environment=SPRING_PROFILES_ACTIVE=prod

[Install]
WantedBy=multi-user.target
EOF

    # Frontend service
    sudo tee /etc/systemd/system/minervia-frontend.service > /dev/null <<EOF
[Unit]
Description=Minervia Frontend Service
After=network.target minervia-backend.service

[Service]
Type=simple
User=www-data
WorkingDirectory=${PROJECT_DIR}/frontend
ExecStart=/usr/bin/npm start
Restart=always
RestartSec=10
Environment=NODE_ENV=production

[Install]
WantedBy=multi-user.target
EOF

    sudo systemctl daemon-reload
    sudo systemctl enable minervia-backend minervia-frontend

    log INFO "Systemd services created"
}

deploy_manual() {
    log INFO "Starting manual installation..."

    if [[ "$OS_ID" != "debian" && "$OS_ID" != "ubuntu" ]]; then
        die "Manual installation only supports Debian/Ubuntu"
    fi

    check_system_requirements

    # Install dependencies
    log INFO "Installing system dependencies..."
    sudo apt-get update
    sudo apt-get install -y curl wget git build-essential

    # Check and install Java
    if ! check_command java || ! java -version 2>&1 | grep -q "version \"21"; then
        install_java
    else
        log INFO "Java already installed"
    fi

    # Check and install Node.js
    if ! check_command node || ! version_gte "$(node -v | tr -d 'v')" "$NODE_MIN_VERSION"; then
        install_nodejs
    else
        log INFO "Node.js already installed"
    fi

    # Check and install MySQL
    if ! check_command mysql; then
        install_mysql
    else
        log INFO "MySQL already installed"
    fi

    # Check and install Redis
    if ! check_command redis-cli; then
        install_redis
    else
        log INFO "Redis already installed"
    fi

    # Setup database
    setup_database

    # Setup environment
    setup_env_file

    # Build applications
    build_backend
    build_frontend

    # Create systemd services
    create_systemd_services

    # Start services
    log INFO "Starting services..."
    sudo systemctl start minervia-backend
    sleep 10
    sudo systemctl start minervia-frontend

    log INFO "Manual installation complete!"
    echo ""
    echo "=========================================="
    echo "  Minervia Institute - Installation Complete"
    echo "=========================================="
    echo ""
    echo "Services:"
    echo "  Backend:  sudo systemctl status minervia-backend"
    echo "  Frontend: sudo systemctl status minervia-frontend"
    echo ""
    echo "Access URLs:"
    echo "  Frontend: http://localhost:3000"
    echo "  Backend:  http://localhost:8080"
    echo ""
}

# =============================================================================
# Main
# =============================================================================

show_help() {
    cat <<EOF
Minervia Institute - Deployment Script

Usage: $0 [command] [options]

Commands:
  docker      Deploy using Docker (recommended)
  manual      Manual installation on Debian/Ubuntu
  check       Check system requirements only
  help        Show this help message

Options:
  --debug     Enable debug output
  --yes       Auto-confirm all prompts

Examples:
  $0                  # Interactive mode
  $0 docker           # Docker deployment
  $0 manual           # Manual installation
  $0 check            # Check requirements

For more information, see docs/DEPLOYMENT.md
EOF
}

main() {
    # Initialize log file
    echo "=== Deployment started at $(date) ===" >> "$LOG_FILE"

    # Parse arguments
    local command="${1:-}"
    shift || true

    while [[ $# -gt 0 ]]; do
        case "$1" in
            --debug) DEBUG=true ;;
            --yes) YES=true ;;
            *) log WARN "Unknown option: $1" ;;
        esac
        shift
    done

    # Detect OS
    detect_os

    # Execute command
    case "$command" in
        docker)
            deploy_docker
            ;;
        manual)
            deploy_manual
            ;;
        check)
            check_system_requirements
            check_all_ports
            check_docker && log INFO "Docker is ready"
            ;;
        help|--help|-h)
            show_help
            ;;
        "")
            # Interactive mode
            echo ""
            echo "=========================================="
            echo "  Minervia Institute - Deployment Script"
            echo "=========================================="
            echo ""
            echo "Select deployment method:"
            echo "  1) Docker (recommended)"
            echo "  2) Manual installation"
            echo "  3) Check requirements only"
            echo "  4) Exit"
            echo ""
            read -rp "Enter choice [1-4]: " choice

            case "$choice" in
                1) deploy_docker ;;
                2) deploy_manual ;;
                3)
                    check_system_requirements
                    check_all_ports
                    ;;
                4) exit 0 ;;
                *) die "Invalid choice" ;;
            esac
            ;;
        *)
            die "Unknown command: $command. Use --help for usage."
            ;;
    esac
}

main "$@"
