# Minervia Platform Development Environment

## Services
- MySQL 8.0 (port 3306)
- Redis 7 (port 6379)
- Kafka (port 9092)
- Elasticsearch 8.11 (port 9200)

## Quick Start

### Start infrastructure only (recommended for development)
```bash
docker compose up -d mysql redis
```

### Start all services including Kafka and Elasticsearch
```bash
docker compose up -d
```

### Start full stack (includes backend and frontend)
```bash
docker compose --profile full up -d
```

## Database Access
- Host: localhost
- Port: 3306
- Database: minervia
- User: minervia
- Password: minervia_dev

## Initial Admin Setup
Create the first admin account using the CLI tool after database is ready:
```bash
cd backend
./gradlew bootRun --args='--create-admin'
```
Or via SQL (development only):
```sql
INSERT INTO admins (username, email, password_hash, role) VALUES
('admin', 'admin@localhost', '<bcrypt-hash>', 'SUPER_ADMIN');
```

## Security Notes
- JWT_SECRET environment variable MUST be set in production
- Database SSL should be enabled in production
- Never commit credentials to version control

## Frontend Development
```bash
cd frontend
npm install
npm run dev
```

## Backend Development
Requires Java 21+
```bash
cd backend
./gradlew bootRun
```
