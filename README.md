# Minervia Institute - Educational Platform

A comprehensive educational management system built with modern technologies, featuring student registration, admin management, and a multilingual marketing website.

## Overview

Minervia Institute is a full-stack educational platform that provides:

- **Student Registration System**: Multi-step registration with email verification and real-time status tracking
- **Admin Portal**: Comprehensive management interface for applications, students, and registration codes
- **Student Portal**: Personalized dashboard for enrolled students
- **Marketing Website**: Multilingual public-facing website (English, Polish, Chinese)
- **Real-time Communication**: WebSocket-based live updates for registration status

## Tech Stack

### Backend
- **Kotlin** with Spring Boot 3.x
- **PostgreSQL** for data persistence
- **Redis** for caching and session management
- **Flyway** for database migrations
- **WebSocket** for real-time updates

### Frontend
- **Next.js 14** with App Router
- **TypeScript** for type safety
- **Tailwind CSS** for styling
- **next-intl** for internationalization
- **Radix UI** for accessible components

## Quick Start

For detailed deployment instructions, see [docs/QUICKSTART.md](docs/QUICKSTART.md).

### Prerequisites

- Docker & Docker Compose (recommended)
- OR: JDK 17+, Node.js 18+, PostgreSQL 15+, Redis 7+

### Docker Deployment (Recommended)

```bash
# 1. Copy environment template
cp .env.example .env

# 2. Configure environment variables
nano .env

# 3. Deploy with Docker
./scripts/deploy.sh

# 4. Check status
./scripts/minervia.sh status
```

### Manual Deployment

See [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) for detailed manual installation instructions.

## Project Structure

```
.
├── backend/              # Kotlin Spring Boot application
│   ├── src/main/kotlin/ # Application source code
│   └── src/main/resources/ # Configuration and migrations
├── frontend/            # Next.js application
│   ├── src/app/        # App Router pages
│   ├── src/components/ # React components
│   └── messages/       # i18n translations
├── docs/               # Documentation
├── scripts/            # Deployment and management scripts
└── openspec/           # OpenSpec specifications
```

## Features

### Student Registration Flow
1. Enter registration code provided by admin
2. Fill in basic information (identity type, country)
3. Verify email address
4. Select major and class preferences
5. Real-time status tracking via WebSocket
6. Automated identity generation and photo creation

### Admin Features
- Dashboard with key metrics
- Application review and approval
- Student management
- Registration code generation
- Audit log tracking

### Marketing Website
- Multilingual support (en, pl, zh-CN)
- Responsive design
- SEO optimized
- Pages: Home, About, Programs, Admissions, Campus Life, News, FAQ, History

## Configuration

Key environment variables (see `.env.example` for full list):

```env
# Database
DATABASE_URL=postgresql://localhost:5432/minervia
DATABASE_USERNAME=minervia
DATABASE_PASSWORD=your_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Email (SendGrid)
SENDGRID_API_KEY=your_sendgrid_key
SENDGRID_FROM_EMAIL=noreply@minervia.edu

# Frontend
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_WS_URL=ws://localhost:8080
```

## Management

Use the `minervia.sh` script for common operations:

```bash
# Check service status
./scripts/minervia.sh status

# View logs
./scripts/minervia.sh logs [service]

# Restart services
./scripts/minervia.sh restart

# Backup database
./scripts/minervia.sh backup

# View port usage
./scripts/minervia.sh ports
```

## Development

See [DEV.md](DEV.md) for development setup and guidelines.

## Documentation

- [Deployment Guide](docs/DEPLOYMENT.md) - Comprehensive deployment instructions
- [Quick Start](docs/QUICKSTART.md) - 5-minute setup guide
- [Development Guide](DEV.md) - Development environment setup

## License

Copyright © 2024 Minervia Institute. All rights reserved.

## Support

For issues and questions, please contact the development team or refer to the documentation.
