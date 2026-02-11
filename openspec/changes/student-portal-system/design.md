# Design: 学生门户系统完整实现

> 基于 proposal.md 和多模型分析结果生成
> 最后更新：2026-02-10

## 设计概述

本设计文档定义了学生门户系统的技术实现方案，包括：
- 管理员前端（8 个页面）
- 文档存储系统（Cloudflare R2）
- 邮件重试机制（Kafka）
- 审计员角色（AUDITOR）

### 设计原则

1. **向后兼容**：保留现有 API 端点，通过别名/适配层支持前端
2. **最小权限**：AUDITOR 角色仅只读访问审计日志和统计数据
3. **幂等性优先**：所有副作用操作（邮件、文档）必须幂等
4. **降级优雅**：R2/Kafka 不可用时系统可降级运行

---

## 架构决策记录（ADR）

### ADR-001: 文档存储方案

**决策**：使用 Cloudflare R2 + 预签名 URL 直传

**理由**：
- 减少后端带宽压力（前端直传）
- S3 兼容 API，SDK 成熟
- 成本低于 AWS S3

**权衡**：
- 需要两阶段 finalize 处理一致性
- URL 泄漏风险需严格 TTL 控制

**替代方案**：
- 后端代理上传：带宽成本高，但权限控制更强
- MinIO 自建：运维复杂度高

**实现约束**：
- Upload URL TTL: 15 分钟
- Download URL TTL: 1 小时
- 最大文件大小: 10MB（可配置）
- 允许类型: PDF, JPEG, PNG, DOCX

---

### ADR-002: 邮件重试机制

**决策**：DB (email_deliveries) + Kafka worker

**理由**：
- 精确实现 backoff/幂等/可观测
- 敏感数据可改为 DB 引用或加密存储
- 避免在 consumer 里 sleep/阻塞

**权衡**：
- 需要新增表与状态机/调度逻辑

**替代方案**：
- Spring Kafka RetryableTopic：实现快但可观测性差
- 仅 DB + @Scheduled：不满足"Kafka 邮件重试"约束

**实现约束**：
- 重试策略: 指数退避 1s, 2s, 4s, 8s (max 3 次)
- Jitter: ±20%
- DLT 处理: 超过 3 次进入死信队列并告警
- 幂等键: `{eventType}:{entityId}:{recipient}:{template}`

---

### ADR-003: 审计员角色权限模型

**决策**：继续使用枚举角色 + @PreAuthorize

**理由**：
- 改动小、可快速落地 AUDITOR
- 现有代码已使用此模式

**权衡**：
- 权限粒度扩展到更多角色时会变臃肿

**替代方案**：
- RBAC 表驱动：权限可配置但需大范围重构

**实现约束**：
- AUDITOR 允许路径:
  - `GET /api/admin/audit-logs/**`
  - `GET /api/admin/statistics/**`
- AUDITOR 禁止路径:
  - 所有 POST/PUT/DELETE 操作
  - `/api/admin/students/**` (写)
  - `/api/admin/registration-codes/**` (写)
  - `/api/admin/config/**` (写)

---

### ADR-004: API 响应格式标准化

**决策**：统一使用 ApiResponse 包装

**理由**：
- 前端可统一处理成功/失败
- 错误码可枚举且稳定

**格式定义**：
```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "timestamp": "2026-02-10T04:00:00Z"
}
```

**错误格式**：
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "DOCUMENT_NOT_FOUND",
    "message": "Document with ID 123 not found",
    "details": { "documentId": 123 }
  },
  "timestamp": "2026-02-10T04:00:00Z"
}
```

**例外端点**：
- 审计日志导出（CSV 流式）
- 文档下载（预签名 URL 重定向）

---

### ADR-005: 数据库时间字段标准

**决策**：统一使用 `DATETIME(3)` + UTC

**理由**：
- MySQL `DATETIME` 不受时区影响，存储 UTC 更安全
- 毫秒精度满足审计需求

**实现约束**：
- 所有时间字段使用 `DATETIME(3)`
- 应用层统一转换为 UTC 存储
- API 响应使用 ISO 8601 格式（带 Z 后缀）

---

### ADR-006: 前端路由别名策略

**决策**：保留旧路由，新增规范路由

**理由**：
- 避免破坏现有前端调用
- 逐步迁移到规范路由

**映射关系**：
- `/api/admin/stats/*` → `/api/admin/statistics/*` (别名)
- `/api/admin/students/{id}/suspend` → `/api/admin/students/{id}/ban` (别名)
- `/api/admin/students/{id}/reactivate` → `/api/admin/students/{id}/unban` (别名)

**弃用计划**：
- 保留旧路由 6 个月
- 响应 header 添加 `X-Deprecated: true`
- 文档标注 `@Deprecated`

---

## 数据模型设计

### 新增表：student_documents

**用途**：持久化文档元数据与 R2 对象键的绑定

**Schema**：
```sql
CREATE TABLE student_documents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    object_key VARCHAR(512) NOT NULL UNIQUE,
    bucket VARCHAR(128) NOT NULL DEFAULT 'minervia-documents',
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    size_bytes BIGINT NOT NULL,
    status ENUM('PENDING_UPLOAD', 'ACTIVE', 'DELETED') NOT NULL DEFAULT 'PENDING_UPLOAD',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at DATETIME(3) NULL,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE RESTRICT,
    INDEX idx_student_created (student_id, created_at),
    INDEX idx_student_status (student_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**字段说明**：
- `object_key`: R2 对象键，格式 `students/{studentId}/documents/{uuid}_{sanitizedFileName}`
- `status`: 状态机 PENDING_UPLOAD → ACTIVE → DELETED
- `deleted_at`: 软删除时间戳

**约束**：
- `object_key` 全局唯一（避免覆盖）
- `ON DELETE RESTRICT` 防止误删学生导致文档孤儿

---

### 新增表：email_deliveries

**用途**：邮件发送的幂等、重试、可观测性与统计

**Schema**：
```sql
CREATE TABLE email_deliveries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dedupe_key VARCHAR(128) NOT NULL UNIQUE,
    recipient_email VARCHAR(255) NOT NULL,
    template VARCHAR(50) NOT NULL,
    locale VARCHAR(16) NOT NULL DEFAULT 'en',
    params_json TEXT NOT NULL,
    status ENUM('PENDING', 'SENT', 'FAILED', 'SUPPRESSED') NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(3) NULL,
    last_error TEXT NULL,
    provider_message_id VARCHAR(128) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    INDEX idx_status_next_attempt (status, next_attempt_at),
    INDEX idx_recipient_created (recipient_email, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**字段说明**：
- `dedupe_key`: 幂等键，格式 `{eventType}:{entityId}:{recipient}:{template}`
- `params_json`: 模板参数（敏感字段需加密或引用）
- `next_attempt_at`: 下次重试时间（指数退避计算）

**状态机**：
```
PENDING → SENT (成功)
PENDING → FAILED (失败，attempt_count++)
FAILED → PENDING (重试，更新 next_attempt_at)
PENDING → SUPPRESSED (黑名单)
```

---

### 修改表：admins

**变更**：扩展 role 枚举

**Migration**：
```sql
ALTER TABLE admins 
MODIFY COLUMN role ENUM('SUPER_ADMIN', 'ADMIN', 'AUDITOR') 
NOT NULL DEFAULT 'ADMIN';
```

**注意**：MySQL ENUM 回滚困难，视为不可逆变更

---

## API 设计

### 文档管理 API

#### POST /api/student/documents
**功能**：初始化文档上传，返回预签名 PUT URL

**请求**：
```json
{
  "fileName": "transcript.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 1048576
}
```

**响应**：
```json
{
  "success": true,
  "data": {
    "documentId": 123,
    "uploadUrl": "https://r2.cloudflare.com/...",
    "expiresAt": "2026-02-10T05:15:00Z"
  }
}
```

**验证规则**：
- `contentType` 必须在白名单内
- `sizeBytes` ≤ 10MB
- `fileName` 长度 ≤ 255，仅允许字母数字和 `-_.`

---

#### POST /api/student/documents/{id}/complete
**功能**：标记上传完成，验证对象存在

**请求**：
```json
{
  "etag": "\"abc123\"",
  "sha256": "optional-hash"
}
```

**响应**：
```json
{
  "success": true,
  "data": {
    "id": 123,
    "status": "ACTIVE"
  }
}
```

**验证流程**：
1. HEAD 请求验证对象存在
2. 验证 Content-Type 匹配
3. 验证 Content-Length 匹配
4. 更新状态为 ACTIVE

**幂等性**：重复调用返回相同结果

---

#### GET /api/student/documents
**功能**：列出当前学生的文档

**查询参数**：
- `page`: 页码（默认 0）
- `size`: 每页大小（默认 20，最大 100）
- `status`: 过滤状态（可选）

**响应**：
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 123,
        "fileName": "transcript.pdf",
        "contentType": "application/pdf",
        "sizeBytes": 1048576,
        "status": "ACTIVE",
        "createdAt": "2026-02-10T04:00:00Z"
      }
    ],
    "pageInfo": {
      "page": 0,
      "size": 20,
      "total": 5,
      "totalPages": 1
    }
  }
}
```

---

#### GET /api/student/documents/{id}/download-url
**功能**：获取预签名 GET URL

**响应**：
```json
{
  "success": true,
  "data": {
    "downloadUrl": "https://r2.cloudflare.com/...",
    "expiresAt": "2026-02-10T05:00:00Z"
  }
}
```

**约束**：
- 仅 ACTIVE 状态可下载
- TTL: 1 小时

---

#### DELETE /api/student/documents/{id}
**功能**：软删除文档

**响应**：
```json
{
  "success": true,
  "data": {
    "id": 123,
    "status": "DELETED"
  }
}
```

**行为**：
- 设置 `deleted_at` 时间戳
- 状态改为 DELETED
- 异步清理 R2 对象（或标记待清理）

---

### 管理员 API 扩展

#### GET /api/admin/me
**功能**：获取当前管理员信息

**响应**：
```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "admin",
    "role": "SUPER_ADMIN",
    "totpEnabled": true,
    "createdAt": "2026-01-01T00:00:00Z"
  }
}
```

**缓存策略**：`Cache-Control: no-store`

---

#### GET /api/admin/statistics/students
**功能**：学生统计数据

**响应**：
```json
{
  "success": true,
  "data": {
    "total": 150,
    "active": 120,
    "suspended": 10,
    "graduated": 20
  }
}
```

**权限**：AUDITOR 可访问

---

#### GET /api/admin/statistics/registrations
**功能**：注册申请统计

**响应**：
```json
{
  "success": true,
  "data": {
    "pending": 25,
    "approved": 100,
    "rejected": 15
  }
}
```

**权限**：AUDITOR 可访问

---

#### GET /api/admin/statistics/emails
**功能**：邮件发送统计

**响应**：
```json
{
  "success": true,
  "data": {
    "sentToday": 45,
    "sentThisMonth": 1200,
    "failedToday": 2
  }
}
```

**权限**：AUDITOR 可访问

---

## 配置设计

### R2 配置

**配置项**（application.yml）：
```yaml
app:
  r2:
    enabled: true
    endpoint: https://your-account.r2.cloudflarestorage.com
    bucket: minervia-documents
    region: auto
    access-key: ${R2_ACCESS_KEY}
    secret-key: ${R2_SECRET_KEY}
    upload-url-ttl: 900  # 15 分钟（秒）
    download-url-ttl: 3600  # 1 小时（秒）
    max-file-size: 10485760  # 10MB（字节）
    allowed-content-types:
      - application/pdf
      - image/jpeg
      - image/png
      - application/vnd.openxmlformats-officedocument.wordprocessingml.document
```

**降级策略**：
- `enabled: false` 时，文档 API 返回 503 + `DOCUMENT_SERVICE_UNAVAILABLE`
- 前端隐藏文档上传入口

---

### Kafka 配置

**Topic 定义**：
```yaml
app:
  kafka:
    enabled: true
    topics:
      email-deliveries:
        name: email.deliveries.send
        partitions: 3
        replication-factor: 2
        retention-ms: 604800000  # 7 天
      email-dlt:
        name: email.deliveries.dlt
        partitions: 1
        replication-factor: 2
        retention-ms: 2592000000  # 30 天
```

**Consumer 配置**：
```yaml
spring:
  kafka:
    consumer:
      group-id: email-delivery-consumer
      max-poll-records: 10
      enable-auto-commit: false
```

---

### 邮件重试配置

**重试策略**：
```yaml
app:
  email:
    retry:
      enabled: true
      max-attempts: 3
      initial-delay-ms: 1000
      multiplier: 2.0
      max-delay-ms: 8000
      jitter: 0.2
```

**计算公式**：
```
delay = min(initial * (multiplier ^ attempt), max) * (1 + random(-jitter, +jitter))
```

**示例**：
- 第 1 次重试: 1000ms * (1 ± 0.2) = 800-1200ms
- 第 2 次重试: 2000ms * (1 ± 0.2) = 1600-2400ms
- 第 3 次重试: 4000ms * (1 ± 0.2) = 3200-4800ms

---

## 安全设计

### AUDITOR 权限矩阵

**允许访问**：
```
GET /api/admin/audit-logs
GET /api/admin/audit-logs/{id}
GET /api/admin/audit-logs/export
GET /api/admin/audit-logs/statistics
GET /api/admin/statistics/students
GET /api/admin/statistics/registrations
GET /api/admin/statistics/emails
```

**禁止访问**（返回 403）：
```
POST /api/admin/students/{id}/ban
POST /api/admin/students/{id}/unban
POST /api/admin/registration-codes
POST /api/admin/registration-applications/{id}/approve
POST /api/admin/registration-applications/{id}/reject
PUT /api/admin/config/{key}
POST /api/super-admin/**
```

**SecurityConfig 实现**：
```kotlin
http.authorizeHttpRequests { auth ->
    auth
        .requestMatchers("/api/admin/audit-logs/**").hasAnyRole("AUDITOR", "ADMIN", "SUPER_ADMIN")
        .requestMatchers("/api/admin/statistics/**").hasAnyRole("AUDITOR", "ADMIN", "SUPER_ADMIN")
        .requestMatchers(HttpMethod.GET, "/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
        .requestMatchers("/api/super-admin/**").hasRole("SUPER_ADMIN")
}
```

---

### 文档访问控制

**规则**：
1. 学生仅能访问自己的文档
2. 管理员不能访问学生文档（隔离）
3. 预签名 URL 短 TTL 防泄漏

**实现**：
```kotlin
@PreAuthorize("@documentSecurityService.canAccess(#id, authentication)")
fun getDocument(@PathVariable id: Long): DocumentDto
```

**SecurityService**：
```kotlin
fun canAccess(documentId: Long, auth: Authentication): Boolean {
    val document = documentRepository.findById(documentId) ?: return false
    val studentId = (auth.principal as StudentPrincipal).studentId
    return document.studentId == studentId
}
```

---

### 邮件模板 XSS 防护

**问题**：当前 EmailTemplateRenderer 直接字符串替换，未转义参数

**修复**：
```kotlin
fun render(template: String, params: Map<String, String>): String {
    return params.entries.fold(template) { acc, (key, value) ->
        val escaped = HtmlUtils.htmlEscape(value)
        acc.replace("{{$key}}", escaped)
    }
}
```

**白名单字段**（允许 HTML）：
- `emailBody`（仅管理员配置的系统邮件）

---

## 前端设计

### 管理员页面结构

**路由组织**：
```
src/app/[locale]/(admin)/
├── admin/
│   ├── layout.tsx          # 管理员布局（Shell + Sidebar）
│   ├── dashboard/
│   │   └── page.tsx        # 仪表板
│   ├── applications/
│   │   ├── page.tsx        # 申请列表
│   │   └── [id]/page.tsx   # 申请详情
│   ├── students/
│   │   ├── page.tsx        # 学生列表
│   │   └── [id]/page.tsx   # 学生详情
│   ├── codes/
│   │   └── page.tsx        # 注册码管理
│   ├── admins/
│   │   └── page.tsx        # 管理员管理（SUPER_ADMIN only）
│   ├── audit/
│   │   └── page.tsx        # 审计日志（AUDITOR + SUPER_ADMIN）
│   └── config/
│       └── page.tsx        # 系统配置（SUPER_ADMIN only）
```

---

### 权限门控组件

**RoleGuard 组件**：
```tsx
interface RoleGuardProps {
  allowedRoles: AdminRole[]
  children: React.ReactNode
  fallback?: React.ReactNode
}

export function RoleGuard({ allowedRoles, children, fallback }: RoleGuardProps) {
  const { user } = useAuth()
  
  if (!user || !allowedRoles.includes(user.role)) {
    return fallback || <Navigate to="/admin/dashboard" />
  }
  
  return <>{children}</>
}
```

**使用示例**：
```tsx
<RoleGuard allowedRoles={['SUPER_ADMIN']}>
  <AdminManagementPage />
</RoleGuard>
```

---

### 文档上传组件

**DocumentUpload 组件**：
```tsx
export function DocumentUpload() {
  const [file, setFile] = useState<File | null>(null)
  const [uploading, setUploading] = useState(false)
  
  const handleUpload = async () => {
    if (!file) return
    
    setUploading(true)
    try {
      // 1. 初始化上传
      const { documentId, uploadUrl } = await studentApiClient.post('/documents', {
        fileName: file.name,
        contentType: file.type,
        sizeBytes: file.size
      })
      
      // 2. 直传 R2
      await fetch(uploadUrl, {
        method: 'PUT',
        body: file,
        headers: { 'Content-Type': file.type }
      })
      
      // 3. 完成上传
      await studentApiClient.post(`/documents/${documentId}/complete`)
      
      toast.success('文档上传成功')
    } catch (error) {
      toast.error('上传失败')
    } finally {
      setUploading(false)
    }
  }
  
  return (
    <div>
      <input type="file" onChange={(e) => setFile(e.target.files?.[0])} />
      <Button onClick={handleUpload} disabled={!file || uploading}>
        {uploading ? '上传中...' : '上传'}
      </Button>
    </div>
  )
}
```

---

### 状态管理策略

**全局状态**（React Context）：
- 用户认证信息（auth-context）
- 主题设置（theme-context）

**本地状态**（useState/useReducer）：
- 表单数据
- 列表过滤/排序
- 模态框显示

**URL 状态**（useSearchParams）：
- 分页参数（page, size）
- 搜索关键词（q）
- 过滤条件（status, role）

---

### 错误处理

**统一错误处理**：
```tsx
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.data?.error) {
      const { code, message } = error.response.data.error
      
      // 根据错误码显示本地化消息
      const localizedMessage = t(`errors.${code}`, { defaultValue: message })
      toast.error(localizedMessage)
    }
    
    return Promise.reject(error)
  }
)
```

---

## 测试策略

### 后端测试

**单元测试**：
- DocumentService: 上传/完成/列表/删除逻辑
- EmailDeliveryService: 幂等键生成、状态机转换
- R2StorageService: 预签名 URL 生成

**集成测试**：
- DocumentController: 完整上传流程（mock R2）
- EmailDeliveryConsumer: Kafka 消息处理
- AUDITOR 权限: 访问控制矩阵验证

**属性测试**（jqwik）：
- 文档状态机: PENDING_UPLOAD → ACTIVE → DELETED
- 邮件重试: 指数退避算法验证
- 幂等键: 相同事件生成相同 dedupe_key

---

### 前端测试

**组件测试**（Vitest + Testing Library）：
- DocumentUpload: 上传流程、错误处理
- RoleGuard: 权限门控逻辑
- AdminSidebar: 根据角色显示菜单

**集成测试**：
- 管理员登录 → 仪表板 → 审批申请流程
- 学生上传文档 → 列表显示 → 下载流程

---

## 部署清单

### 数据库迁移

1. `V8__add_auditor_role.sql`
2. `V9__create_student_documents.sql`
3. `V10__create_email_deliveries.sql`

### 环境变量

```bash
# R2 配置
R2_ACCESS_KEY=your-access-key
R2_SECRET_KEY=your-secret-key

# Kafka 配置（已有）
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

### Kafka Topic 创建

```bash
kafka-topics.sh --create \
  --topic email.deliveries.send \
  --partitions 3 \
  --replication-factor 2

kafka-topics.sh --create \
  --topic email.deliveries.dlt \
  --partitions 1 \
  --replication-factor 2
```

---

## 监控与告警

### 关键指标

**文档系统**：
- R2 上传成功率
- 预签名 URL 生成延迟
- PENDING_UPLOAD 超时清理数量

**邮件系统**：
- 邮件发送成功率
- 重试队列积压数量
- DLT 消息数量

**权限系统**：
- AUDITOR 403 错误数量（检测越权尝试）

### 告警规则

- R2 不可用 > 5 分钟 → 告警
- DLT 消息 > 10 条 → 告警
- 邮件重试队列积压 > 100 条 → 告警

---

