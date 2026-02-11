[根目录](../CLAUDE.md) > **backend**

# Backend 模块 - Kotlin Spring Boot 后端服务

> 最后更新：2026-02-09

## 变更记录 (Changelog)

### 2026-02-09
- 初始化模块文档
- 完成包结构扫描
- 识别核心服务层：认证、注册、审计、身份生成、邮件、限流
- 记录 15 个 Controller、159 个 Kotlin 文件
- 标注测试覆盖：单元测试、集成测试、属性测试

---

## 模块职责

Backend 是基于 Kotlin + Spring Boot 3.4.1 的后端服务，负责：

- **认证授权**：JWT 双 Token 机制（Admin + Student）、TOTP 二步验证、OAuth（LinuxDo）
- **学生注册**：多步骤注册流程、邮箱验证、实时状态推送（WebSocket）
- **管理员功能**：申请审核、学生管理、注册码生成、系统配置
- **审计日志**：完整操作记录、Elasticsearch 搜索、完整性校验（哈希链）
- **身份生成**：LLM 驱动的学生身份信息生成（OpenAI 集成）
- **异步任务**：Kafka + Outbox 模式处理长时任务
- **限流保护**：Redis + MySQL 双写限流、邮件反弹处理

---

## 入口与启动

**主类**：`src/main/kotlin/edu/minervia/platform/MinerviaApplication.kt`

```kotlin
@SpringBootApplication
class MinerviaApplication

fun main(args: Array<String>) {
    runApplication<MinerviaApplication>(*args)
}
```

**启动命令**：
```bash
# 开发环境
./gradlew bootRun

# 生产环境
./gradlew build
java -jar build/libs/minervia-platform-0.0.1-SNAPSHOT.jar
```

**端口**：8080

---

## 对外接口

### API 端点清单

| Controller | 路径前缀 | 职责 | 认证要求 |
|-----------|---------|------|---------|
| `AuthController` | `/api/auth` | 管理员登录、登出、Token 刷新 | 部分公开 |
| `StudentAuthController` | `/api/student/auth` | 学生登录、登出、Token 刷新 | 部分公开 |
| `RegistrationController` | `/api/registration` | 学生注册流程 | 公开 |
| `RegistrationCodeController` | `/api/admin/codes` | 注册码管理 | Admin JWT |
| `StudentController` | `/api/admin/students` | 学生管理 | Admin JWT |
| `StudentPortalController` | `/api/student/portal` | 学生门户数据 | Student JWT |
| `AuditLogController` | `/api/admin/audit` | 审计日志查询 | Admin JWT |
| `StatisticsController` | `/api/admin/statistics` | 统计数据 | Admin JWT |
| `SystemConfigController` | `/api/admin/config` | 系统配置 | Admin JWT |
| `IdentityGenerationController` | `/api/admin/identity` | 身份生成与导出 | Admin JWT |
| `ProgressController` | `/api/progress` | 任务进度查询 | 公开 |
| `TotpController` | `/api/admin/totp` | TOTP 二步验证 | Admin JWT |
| `OAuthController` | `/api/oauth` | OAuth 登录（LinuxDo） | 公开 |
| `BounceWebhookController` | `/api/webhooks/bounce` | 邮件反弹 Webhook | Webhook 签名 |
| `EmailSuppressionAdminController` | `/api/admin/email-suppression` | 邮件黑名单管理 | Admin JWT |
| `AdminManagementController` | `/api/admin/management` | 管理员账户管理 | Admin JWT |

**API 文档**：http://localhost:8080/swagger-ui.html

---

## 关键依赖与配置

### 依赖（build.gradle.kts）

**核心框架**：
- Spring Boot 3.4.1
- Kotlin 2.1.0
- JDK 21

**数据层**：
- Spring Data JPA
- MySQL Connector
- Flyway（数据库迁移）
- Spring Data Redis

**消息队列**：
- Spring Kafka

**安全**：
- Spring Security
- JWT (jjwt 0.12.6)

**文档**：
- SpringDoc OpenAPI 2.7.0

**测试**：
- JUnit 5
- Mockito Kotlin 5.4.0
- Testcontainers 1.20.4（MySQL、Kafka）
- jqwik 1.9.2（属性测试）

### 配置文件

**application.yml**：
- 数据库连接：MySQL 8.0
- Redis 配置
- Kafka 配置
- JWT 配置（Access Token 30min、Refresh Token 14天）
- OpenAI 配置（LLM 集成）
- 邮件配置
- WebSocket 配置

**application-prod.yml**：生产环境配置

**环境变量**：
- `JWT_SECRET`：JWT 签名密钥（必须）
- `DB_PASSWORD`：数据库密码
- `REDIS_PASSWORD`：Redis 密码
- `OPENAI_API_KEY`：OpenAI API 密钥
- `MAIL_HOST`、`MAIL_USERNAME`、`MAIL_PASSWORD`：邮件服务配置

---

## 数据模型

### 核心实体（domain/entity/）

| 实体 | 表名 | 职责 |
|------|------|------|
| `Admin` | `admins` | 管理员账户 |
| `Student` | `students` | 学生信息 |
| `RegistrationApplication` | `registration_applications` | 注册申请 |
| `RegistrationCode` | `registration_codes` | 注册码 |
| `EmailVerificationCode` | `email_verification_codes` | 邮箱验证码 |
| `AuditLog` | `audit_logs` | 审计日志 |
| `SystemConfig` | `system_configs` | 系统配置 |
| `Major` | `majors` | 专业信息 |
| `StudentFamilyInfo` | `student_family_info` | 学生家庭信息 |
| `EmailSuppression` | `email_suppressions` | 邮件黑名单 |
| `OutboxEvent` | `outbox_events` | Outbox 事件 |
| `TaskProgress` | `task_progress` | 任务进度 |
| `AuditNotification` | `audit_notifications` | 审计告警 |

### 枚举类型（domain/enums/）

- `AdminRole`：管理员角色（SUPER_ADMIN、ADMIN、VIEWER）
- `StudentStatus`：学生状态（PENDING、APPROVED、REJECTED、ENROLLED）
- `ApplicationStatus`：申请状态
- `RegistrationCodeStatus`：注册码状态
- `IdentityType`：身份类型（LOCAL、INTERNATIONAL）
- `EmailSuppressionReason`：邮件黑名单原因（BOUNCE、COMPLAINT、UNSUBSCRIBE）

### 数据库迁移（resources/db/migration/）

- `V1__init.sql`：初始化表结构
- `V2__outbox_and_task_progress.sql`：Outbox 模式与任务进度
- `V3__task_progress_retry_count.sql`：任务重试计数
- `V4__audit_retention.sql`：审计日志保留策略
- `V5__rate_limits.sql`：限流表
- `V6__email_suppression.sql`：邮件黑名单
- `V7__majors_and_student_fields.sql`：专业与学生字段

---

## 测试与质量

### 单元测试（src/test/kotlin/.../service/）

- `AuthServiceTest`：认证服务测试
- `RegistrationCodeServiceTest`：注册码服务测试
- `EmailBounceServiceTest`：邮件反弹服务测试
- `RateLimitServiceTest`：限流服务测试
- `SendGridSmtpEmailServiceTest`：邮件发送服务测试

**运行**：`./gradlew test`

### 集成测试（src/test/kotlin/.../integration/）

- `BaseIntegrationTest`：集成测试基类（Testcontainers）
- `AuthControllerIntegrationTest`：认证接口测试
- `RegistrationCodeControllerIntegrationTest`：注册码接口测试
- `StudentControllerIntegrationTest`：学生管理接口测试
- `AuditLogControllerIntegrationTest`：审计日志接口测试
- `StudentAuthControllerIntegrationTest`：学生认证接口测试
- `RegistrationStepExecutorIntegrationTest`：注册步骤执行器测试

**运行**：`./gradlew integrationTest`

### 属性测试（src/test/kotlin/.../pbt/）

- `CorePropertyTests`：核心业务逻辑属性测试
- `AdvancedPropertyTests`：高级场景属性测试
- `IntegrationPropertyTests`：集成场景属性测试
- `HighCompletionPropertyTests`：高完成度场景属性测试

**框架**：jqwik

---

## 常见问题 (FAQ)

### Q1: 如何添加新的 API 端点？

1. 在 `web/controller/` 创建 Controller
2. 在 `service/` 创建 Service
3. 在 `web/dto/` 创建 DTO
4. 在 `integration/` 添加集成测试
5. 重启服务，访问 Swagger UI 验证

### Q2: 如何执行数据库迁移？

```bash
./gradlew flywayMigrate
```

### Q3: 如何调试 Kafka 消息？

查看日志：
```bash
docker logs minervia-kafka -f
```

### Q4: JWT Token 过期时间如何配置？

在 `application.yml` 中：
```yaml
app:
  jwt:
    access-token-expiration: 1800000  # 30分钟
    refresh-token-expiration: 1209600000  # 14天
```

---

## 相关文件清单

**核心包结构**：
```
src/main/kotlin/edu/minervia/platform/
├── MinerviaApplication.kt          # 主入口
├── config/                         # 配置类
│   ├── SecurityConfig.kt
│   ├── WebSocketConfig.kt
│   ├── KafkaConfig.kt
│   └── ...
├── domain/                         # 领域层
│   ├── entity/                     # 实体
│   ├── repository/                 # 仓库接口
│   └── enums/                      # 枚举
├── service/                        # 服务层
│   ├── async/                      # 异步任务
│   ├── audit/                      # 审计服务
│   ├── auth/                       # 认证服务
│   ├── email/                      # 邮件服务
│   ├── identity/                   # 身份生成
│   ├── oauth/                      # OAuth
│   └── ratelimit/                  # 限流
├── web/                            # Web 层
│   ├── controller/                 # 控制器
│   ├── dto/                        # DTO
│   └── GlobalExceptionHandler.kt  # 全局异常处理
└── security/                       # 安全层
    ├── JwtService.kt
    ├── JwtAuthenticationFilter.kt
    └── ...
```

**资源文件**：
```
src/main/resources/
├── application.yml                 # 主配置
├── application-prod.yml            # 生产配置
├── db/migration/                   # Flyway 迁移脚本
├── llm-templates/                  # LLM 提示模板
└── email-templates/                # 邮件模板
```

---

## 下一步建议

1. **深度分析服务层**：
   - `service/async/` - Kafka + Outbox 模式实现细节
   - `service/audit/` - Elasticsearch 集成与哈希链完整性校验
   - `service/identity/` - LLM 驱动的身份生成逻辑

2. **API 契约详细文档**：
   - 为每个 Controller 生成详细的 API 文档
   - 记录请求/响应示例
   - 标注错误码与异常处理

3. **性能优化点**：
   - 分析 Redis 缓存策略
   - 审查数据库查询性能
   - 评估 Kafka 消费者配置
