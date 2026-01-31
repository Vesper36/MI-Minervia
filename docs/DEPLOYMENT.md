# Minervia Institute Education Platform - Deployment Guide

## Prerequisites

- Docker 24.0+
- Docker Compose 2.20+
- Kubernetes 1.28+ (for production)
- Helm 3.12+ (for Kubernetes deployment)

## Environment Variables

### Backend

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `DB_PASSWORD` | MySQL database password | Yes | - |
| `REDIS_PASSWORD` | Redis password | No | - |
| `JWT_SECRET` | JWT signing secret (min 256 bits) | Yes | - |
| `JWT_ACCESS_EXPIRATION` | Access token TTL (ms) | No | 1800000 |
| `JWT_REFRESH_EXPIRATION` | Refresh token TTL (ms) | No | 1209600000 |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker addresses | No | localhost:9092 |
| `MAIL_HOST` | SMTP server host | Yes | - |
| `MAIL_PORT` | SMTP server port | No | 587 |
| `MAIL_USERNAME` | SMTP username | Yes | - |
| `MAIL_PASSWORD` | SMTP password | Yes | - |
| `OPENAI_API_KEY` | OpenAI API key for LLM | No | - |
| `OPENAI_BASE_URL` | OpenAI API base URL | No | https://api.openai.com |
| `EMAIL_WEBHOOK_SIGNING_KEY` | Generic webhook signing key | No | - |
| `MAILGUN_WEBHOOK_SIGNING_KEY` | Mailgun webhook key | No | - |
| `SENDGRID_WEBHOOK_PUBLIC_KEY` | SendGrid webhook public key | No | - |

### Frontend

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `NEXT_PUBLIC_API_URL` | Backend API URL | Yes | - |
| `NEXT_PUBLIC_WS_URL` | WebSocket URL | Yes | - |

## Docker Compose Deployment

### Development

```yaml
# docker-compose.dev.yml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: minervia
      MYSQL_USER: minervia
      MYSQL_PASSWORD: minervia_dev
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk
    ports:
      - "9092:9092"

  backend:
    build: ./backend
    environment:
      SPRING_PROFILES_ACTIVE: dev
      DB_PASSWORD: minervia_dev
      JWT_SECRET: dev-secret-key-for-development-only-256-bits-minimum
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - redis
      - kafka

  frontend:
    build: ./frontend
    environment:
      NEXT_PUBLIC_API_URL: http://localhost:8080
      NEXT_PUBLIC_WS_URL: ws://localhost:8080
    ports:
      - "3000:3000"
    depends_on:
      - backend

volumes:
  mysql_data:
```

### Production

```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  backend:
    image: minervia/backend:latest
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      KAFKA_BOOTSTRAP_SERVERS: ${KAFKA_BOOTSTRAP_SERVERS}
      MAIL_HOST: ${MAIL_HOST}
      MAIL_USERNAME: ${MAIL_USERNAME}
      MAIL_PASSWORD: ${MAIL_PASSWORD}
      OPENAI_API_KEY: ${OPENAI_API_KEY}
    ports:
      - "8080:8080"
    deploy:
      replicas: 2
      resources:
        limits:
          memory: 1G
        reservations:
          memory: 512M

  frontend:
    image: minervia/frontend:latest
    environment:
      NEXT_PUBLIC_API_URL: https://api.minervia.edu
      NEXT_PUBLIC_WS_URL: wss://api.minervia.edu
    ports:
      - "3000:3000"
    deploy:
      replicas: 2
```

## Kubernetes Deployment

### Namespace

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: minervia
```

### ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: minervia-config
  namespace: minervia
data:
  SPRING_PROFILES_ACTIVE: "prod"
  KAFKA_BOOTSTRAP_SERVERS: "kafka.minervia.svc.cluster.local:9092"
```

### Secrets

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: minervia-secrets
  namespace: minervia
type: Opaque
stringData:
  DB_PASSWORD: "<your-db-password>"
  JWT_SECRET: "<your-jwt-secret-min-256-bits>"
  MAIL_PASSWORD: "<your-mail-password>"
  OPENAI_API_KEY: "<your-openai-key>"
```

### Backend Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: minervia-backend
  namespace: minervia
spec:
  replicas: 3
  selector:
    matchLabels:
      app: minervia-backend
  template:
    metadata:
      labels:
        app: minervia-backend
    spec:
      containers:
        - name: backend
          image: minervia/backend:latest
          ports:
            - containerPort: 8080
          envFrom:
            - configMapRef:
                name: minervia-config
            - secretRef:
                name: minervia-secrets
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 5
```

### Service

```yaml
apiVersion: v1
kind: Service
metadata:
  name: minervia-backend
  namespace: minervia
spec:
  selector:
    app: minervia-backend
  ports:
    - port: 8080
      targetPort: 8080
  type: ClusterIP
```

### Ingress

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: minervia-ingress
  namespace: minervia
  annotations:
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  ingressClassName: nginx
  tls:
    - hosts:
        - api.minervia.edu
        - minervia.edu
      secretName: minervia-tls
  rules:
    - host: api.minervia.edu
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: minervia-backend
                port:
                  number: 8080
    - host: minervia.edu
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: minervia-frontend
                port:
                  number: 3000
```

## Database Migration

Flyway migrations run automatically on application startup. For manual migration:

```bash
./gradlew flywayMigrate -Dflyway.url=jdbc:mysql://localhost:3306/minervia \
  -Dflyway.user=minervia -Dflyway.password=<password>
```

## Health Checks

- Backend: `GET /actuator/health`
- API Documentation: `GET /swagger-ui.html`

## Monitoring

### Prometheus Metrics

Add to `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### Logging

Logs are output to stdout in JSON format for production. Configure log aggregation (ELK, Loki) as needed.

## Backup Strategy

### Database

```bash
mysqldump -h <host> -u minervia -p minervia > backup_$(date +%Y%m%d).sql
```

### Redis

Redis is used for caching and rate limiting. Data loss is acceptable; no backup required.

## Security Checklist

- [ ] JWT_SECRET is at least 256 bits and randomly generated
- [ ] Database passwords are strong and unique
- [ ] HTTPS is enforced in production
- [ ] Webhook signing keys are configured
- [ ] Rate limiting is enabled
- [ ] CORS is properly configured
- [ ] Audit logging is enabled
