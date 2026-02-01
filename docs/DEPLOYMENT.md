# Minervia Institute - Deployment Guide

Complete guide for deploying the Minervia Institute Education Platform.

## Table of Contents

- [Quick Start](#quick-start)
- [System Requirements](#system-requirements)
- [Dependencies](#dependencies)
- [Port Configuration](#port-configuration)
- [Deployment Methods](#deployment-methods)
- [Environment Variables](#environment-variables)
- [Pre-deployment Checklist](#pre-deployment-checklist)
- [Post-deployment Verification](#post-deployment-verification)
- [Troubleshooting](#troubleshooting)
- [Backup and Recovery](#backup-and-recovery)
- [Security Checklist](#security-checklist)

---

## Quick Start

For a quick deployment using Docker:

```bash
# 1. Clone the repository
git clone https://github.com/your-org/minervia-platform.git
cd minervia-platform

# 2. Run the deployment script
./scripts/deploy.sh docker

# 3. Access the application
# Frontend: http://localhost:3000
# Backend:  http://localhost:8080
```

For detailed instructions, see [QUICKSTART.md](./QUICKSTART.md).

---

## System Requirements

### Minimum Requirements

| Resource | Minimum | Recommended | Notes |
|----------|---------|-------------|-------|
| CPU | 2 cores | 4+ cores | More cores improve concurrent request handling |
| RAM | 4 GB | 8+ GB | Backend JVM requires ~1GB, MySQL ~1GB |
| Disk | 20 GB | 50+ GB SSD | SSD recommended for database performance |
| Network | 10 Mbps | 100+ Mbps | Higher bandwidth for file uploads |

### Supported Operating Systems

| OS | Version | Support Level |
|----|---------|---------------|
| Debian | 11, 12 | Full (recommended) |
| Ubuntu | 22.04, 24.04 | Full |
| CentOS/RHEL | 8, 9 | Partial |
| macOS | 13+ | Development only |
| Windows | WSL2 | Development only |

---

## Dependencies

### Docker Deployment

| Dependency | Version | Required | Installation |
|------------|---------|----------|--------------|
| Docker | 24.0+ | Yes | `curl -fsSL https://get.docker.com \| sh` |
| Docker Compose | 2.20+ | Yes | Included with Docker Desktop |
| Git | 2.30+ | Yes | `apt install git` |

### Manual Deployment

| Dependency | Version | Required | Installation |
|------------|---------|----------|--------------|
| Java (OpenJDK) | 21+ | Yes | `apt install openjdk-21-jdk` |
| Node.js | 20+ | Yes | Via NodeSource or nvm |
| MySQL | 8.0+ | Yes | `apt install mysql-server` |
| Redis | 7+ | Yes | `apt install redis-server` |
| Kafka | 3.5+ | Optional | For async processing |
| Nginx | 1.24+ | Optional | For reverse proxy/SSL |

### Potential Dependency Conflicts

| Conflict | Symptom | Resolution |
|----------|---------|------------|
| Java version | `UnsupportedClassVersionError` | Ensure Java 21+ is installed and `JAVA_HOME` is set |
| Node.js version | Build failures | Use nvm to install Node.js 20 |
| MySQL auth | `Access denied` | Use `mysql_native_password` plugin |
| Port conflicts | `Address already in use` | Change ports in `.env` or stop conflicting services |

---

## Port Configuration

### Default Ports

| Port | Service | Protocol | Exposure | Description |
|------|---------|----------|----------|-------------|
| 3000 | Frontend | HTTP | Public | Next.js web application |
| 8080 | Backend | HTTP | Public | Spring Boot REST API |
| 3306 | MySQL | TCP | Internal | Database server |
| 6379 | Redis | TCP | Internal | Cache and rate limiting |
| 9092 | Kafka | TCP | Internal | Message queue (optional) |
| 9200 | Elasticsearch | TCP | Internal | Search engine (optional) |

### Firewall Configuration

For production servers, open only necessary ports:

```bash
# UFW (Ubuntu/Debian)
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw enable

# firewalld (CentOS/RHEL)
sudo firewall-cmd --permanent --add-service=ssh
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload
```

### Changing Ports

Edit `.env` file or use the management script:

```bash
# View current ports
./scripts/minervia.sh ports

# Change a port
./scripts/minervia.sh ports set frontend 3001

# Restart to apply
./scripts/minervia.sh restart
```

---

## Deployment Methods

### Method 1: Docker Deployment (Recommended)

Best for: Production servers, quick setup, isolated environments.

```bash
# Interactive deployment
./scripts/deploy.sh docker

# Or manually:
cp .env.example .env
# Edit .env with your configuration
docker compose -f docker-compose.prod.yml up -d
```

### Method 2: Manual Installation

Best for: Custom configurations, existing infrastructure, learning.

```bash
# Run the manual installation
./scripts/deploy.sh manual

# This will:
# 1. Install Java 21, Node.js 20, MySQL 8, Redis 7
# 2. Configure the database
# 3. Build backend and frontend
# 4. Create systemd services
```

### Method 3: Development Setup

Best for: Local development, testing.

```bash
# Start infrastructure only
docker compose up -d mysql redis

# Run backend (in backend/ directory)
./gradlew bootRun

# Run frontend (in frontend/ directory)
npm run dev
```

---

## Environment Variables

### Required Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `DB_PASSWORD` | MySQL password for minervia user | `SecureP@ssw0rd123` |
| `JWT_SECRET` | JWT signing key (min 32 chars) | `openssl rand -base64 32` |
| `MAIL_HOST` | SMTP server hostname | `smtp.sendgrid.net` |
| `MAIL_USERNAME` | SMTP username | `apikey` |
| `MAIL_PASSWORD` | SMTP password/API key | `SG.xxxxx` |
| `NEXT_PUBLIC_API_URL` | Backend API URL | `https://api.minervia.edu` |
| `NEXT_PUBLIC_WS_URL` | WebSocket URL | `wss://api.minervia.edu` |

### Optional Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `REDIS_PASSWORD` | Redis password | (none) |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers | `localhost:9092` |
| `JWT_ACCESS_EXPIRATION` | Access token TTL (ms) | `1800000` (30 min) |
| `JWT_REFRESH_EXPIRATION` | Refresh token TTL (ms) | `1209600000` (14 days) |
| `OPENAI_API_KEY` | OpenAI API key for LLM | (none) |
| `OPENAI_MODEL` | OpenAI model to use | `gpt-4o-mini` |

### Generating Secure Values

```bash
# Generate JWT secret (256 bits)
openssl rand -base64 32

# Generate database password
openssl rand -base64 16

# Generate webhook signing key
openssl rand -hex 32
```

---

## Pre-deployment Checklist

### Server Preparation

- [ ] Server meets minimum requirements (2 CPU, 4GB RAM, 20GB disk)
- [ ] Operating system is updated (`apt update && apt upgrade`)
- [ ] Firewall is configured (ports 80, 443 open)
- [ ] SSH access is secured (key-based auth, no root login)

### Domain and SSL

- [ ] Domain name is registered and DNS configured
- [ ] A record points to server IP
- [ ] SSL certificate is ready (or use Let's Encrypt)

### External Services

- [ ] Email service account created (SendGrid, Mailgun, etc.)
- [ ] SMTP credentials obtained
- [ ] Webhook URLs configured (for email bounce handling)

### Configuration

- [ ] `.env` file created from `.env.example`
- [ ] All required variables are set
- [ ] JWT_SECRET is at least 32 characters
- [ ] Database password is strong and unique

### Backup Plan

- [ ] Backup storage location identified
- [ ] Backup schedule planned
- [ ] Recovery procedure documented

---

## Post-deployment Verification

### Health Checks

```bash
# Check service status
./scripts/minervia.sh status

# Check backend health
curl http://localhost:8080/actuator/health

# Check frontend
curl http://localhost:3000
```

### Functional Tests

1. **Frontend loads**: Visit `http://your-domain.com`
2. **API responds**: Visit `http://your-domain.com:8080/swagger-ui.html`
3. **Database connected**: Check backend logs for connection success
4. **Email works**: Test registration flow

### Log Verification

```bash
# View all logs
./scripts/minervia.sh logs

# View specific service
./scripts/minervia.sh logs backend -f

# Check for errors
./scripts/minervia.sh logs | grep -i error
```

---

## Troubleshooting

### Common Issues

#### Backend won't start

**Symptom**: Backend container exits immediately or shows errors.

**Solutions**:
1. Check database connection:
   ```bash
   docker exec minervia-mysql mysql -u minervia -p -e "SELECT 1"
   ```
2. Verify environment variables in `.env`
3. Check logs: `./scripts/minervia.sh logs backend`

#### Frontend shows "Cannot connect to API"

**Symptom**: Frontend loads but API calls fail.

**Solutions**:
1. Verify `NEXT_PUBLIC_API_URL` is correct
2. Check CORS configuration in backend
3. Ensure backend is running: `curl http://localhost:8080/actuator/health`

#### Database connection refused

**Symptom**: `Connection refused` errors in backend logs.

**Solutions**:
1. Ensure MySQL is running: `docker ps | grep mysql`
2. Check MySQL logs: `./scripts/minervia.sh logs mysql`
3. Verify credentials in `.env` match MySQL configuration

#### Port already in use

**Symptom**: `Address already in use` error on startup.

**Solutions**:
1. Find conflicting process: `lsof -i :3000`
2. Change port: `./scripts/minervia.sh ports set frontend 3001`
3. Or stop conflicting service

#### Out of memory

**Symptom**: Services crash or become unresponsive.

**Solutions**:
1. Check memory usage: `free -h`
2. Increase server RAM
3. Reduce container memory limits in `docker-compose.prod.yml`

### Getting Help

1. Check logs: `./scripts/minervia.sh logs`
2. Review this documentation
3. Search existing issues on GitHub
4. Open a new issue with:
   - Error messages
   - Steps to reproduce
   - Environment details (OS, Docker version, etc.)

---

## Backup and Recovery

### Automated Backup

```bash
# Create full backup
./scripts/minervia.sh backup

# Create database-only backup
./scripts/minervia.sh backup --db-only

# List available backups
./scripts/minervia.sh backup list
```

### Manual Backup

```bash
# Database backup
docker exec minervia-mysql mysqldump -u minervia -p minervia > backup.sql

# Or for non-Docker:
mysqldump -u minervia -p minervia > backup.sql
```

### Restore from Backup

```bash
# Using management script
./scripts/minervia.sh restore backups/minervia_backup_20260131.tar.gz

# Manual restore
docker exec -i minervia-mysql mysql -u minervia -p minervia < backup.sql
```

### Backup Schedule Recommendation

| Data | Frequency | Retention |
|------|-----------|-----------|
| Database | Daily | 30 days |
| Full backup | Weekly | 12 weeks |
| Configuration | On change | Indefinite |

---

## Security Checklist

### Authentication & Authorization

- [ ] JWT_SECRET is randomly generated and at least 256 bits
- [ ] JWT_SECRET is not committed to version control
- [ ] Access tokens expire in 30 minutes or less
- [ ] Refresh tokens expire in 14 days or less
- [ ] TOTP (2FA) is available for admin accounts

### Network Security

- [ ] HTTPS is enforced in production
- [ ] SSL certificate is valid and not expired
- [ ] Unnecessary ports are closed
- [ ] Internal services (MySQL, Redis) are not exposed publicly
- [ ] CORS is configured to allow only trusted origins

### Data Security

- [ ] Database passwords are strong and unique
- [ ] Sensitive data is encrypted at rest
- [ ] Audit logging is enabled
- [ ] Backups are encrypted and stored securely

### Application Security

- [ ] Rate limiting is enabled
- [ ] Input validation is in place
- [ ] SQL injection protection (parameterized queries)
- [ ] XSS protection (output encoding)
- [ ] CSRF protection for state-changing operations

### Operational Security

- [ ] SSH uses key-based authentication
- [ ] Root login is disabled
- [ ] System packages are updated regularly
- [ ] Logs are monitored for suspicious activity
- [ ] Incident response plan is documented

---

## Additional Resources

- [Quick Start Guide](./QUICKSTART.md)
- [API Documentation](http://localhost:8080/swagger-ui.html)
- [Development Guide](../DEV.md)
