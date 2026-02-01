# Minervia Institute - Quick Start Guide

Get the Minervia Institute platform running in 5 minutes.

## Prerequisites

- Docker 24.0+ and Docker Compose 2.20+
- Git
- 4GB RAM minimum

## Step 1: Clone and Configure

```bash
# Clone the repository
git clone https://github.com/your-org/minervia-platform.git
cd minervia-platform

# Create environment file
cp .env.example .env
```

## Step 2: Configure Required Settings

Edit `.env` and set these required values:

```bash
# Generate and set JWT secret (REQUIRED)
JWT_SECRET=$(openssl rand -base64 32)

# Set database password (REQUIRED)
DB_PASSWORD=your_secure_password_here

# Email configuration (REQUIRED for registration)
MAIL_HOST=smtp.sendgrid.net
MAIL_USERNAME=apikey
MAIL_PASSWORD=your_sendgrid_api_key
```

## Step 3: Deploy

```bash
# Run the deployment script
./scripts/deploy.sh docker

# Or manually with Docker Compose
docker compose -f docker-compose.prod.yml up -d
```

## Step 4: Verify

```bash
# Check service status
./scripts/minervia.sh status

# Or check manually
docker compose -f docker-compose.prod.yml ps
```

## Step 5: Access

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **API Docs**: http://localhost:8080/swagger-ui.html

## Common Commands

```bash
# View logs
./scripts/minervia.sh logs

# Restart services
./scripts/minervia.sh restart

# Stop services
./scripts/minervia.sh stop

# Create backup
./scripts/minervia.sh backup
```

## Troubleshooting

### Services won't start

```bash
# Check logs for errors
./scripts/minervia.sh logs backend

# Verify environment variables
cat .env | grep -v "^#" | grep -v "^$"
```

### Port conflicts

```bash
# Check which ports are in use
./scripts/minervia.sh ports check

# Change a port
./scripts/minervia.sh ports set frontend 3001
```

### Database connection issues

```bash
# Test database connection
docker exec minervia-mysql mysql -u minervia -p -e "SELECT 1"
```

## Next Steps

1. Configure your domain and SSL certificate
2. Set up email service (SendGrid/Mailgun)
3. Configure OpenAI API key for LLM features
4. Set up automated backups
5. Review the [full deployment guide](./DEPLOYMENT.md)

## Need Help?

- Full documentation: [DEPLOYMENT.md](./DEPLOYMENT.md)
- API documentation: http://localhost:8080/swagger-ui.html
- Report issues: GitHub Issues
