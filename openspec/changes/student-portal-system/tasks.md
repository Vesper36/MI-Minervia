# Tasks: 学生门户系统完整实现

> 基于 design.md 和 specs/ 生成的可执行任务清单
> 最后更新：2026-02-10

## Phase 1: 数据库与基础设施 (优先级: 高)

### Task 1.1: 数据库迁移 - AUDITOR 角色
**文件**: `backend/src/main/resources/db/migration/V8__add_auditor_role.sql`
**内容**:
```sql
ALTER TABLE admins 
MODIFY COLUMN role ENUM('SUPER_ADMIN', 'ADMIN', 'AUDITOR') 
NOT NULL DEFAULT 'ADMIN';
```
**验证**: 创建 AUDITOR 角色管理员并登录

---

### Task 1.2: 数据库迁移 - student_documents 表
**文件**: `backend/src/main/resources/db/migration/V9__create_student_documents.sql`
**依赖**: 无
**验证**: 表创建成功，索引存在

---

### Task 1.3: 数据库迁移 - email_deliveries 表
**文件**: `backend/src/main/resources/db/migration/V10__create_email_deliveries.sql`
**依赖**: 无
**验证**: 表创建成功，索引存在

---

### Task 1.4: R2 配置集成
**文件**: 
- `backend/src/main/kotlin/edu/minervia/platform/config/R2Config.kt`
- `backend/src/main/resources/application.yml`

**实现**:
```kotlin
@Configuration
@ConfigurationProperties(prefix = "app.r2")
data class R2Properties(
    var enabled: Boolean = true,
    var endpoint: String = "",
    var bucket: String = "",
    var region: String = "auto",
    var accessKey: String = "",
    var secretKey: String = "",
    var uploadUrlTtl: Int = 900,
    var downloadUrlTtl: Int = 3600,
    var maxFileSize: Long = 10485760,
    var allowedContentTypes: List<String> = listOf()
)
```

**验证**: 配置加载成功，S3Client 初始化

---

## Phase 2: 后端核心服务 (优先级: 高)

### Task 2.1: StudentDocument 实体
**文件**: `backend/src/main/kotlin/edu/minervia/platform/domain/entity/StudentDocument.kt`
**依赖**: Task 1.2
**关键字段**: id, studentId, objectKey, status, createdAt

---

### Task 2.2: StudentDocumentRepository
**文件**: `backend/src/main/kotlin/edu/minervia/platform/domain/repository/StudentDocumentRepository.kt`
**方法**:
- `findByStudentIdAndStatus(studentId, status, pageable)`
- `findByIdAndStudentId(id, studentId)`

---

### Task 2.3: R2StorageService
**文件**: `backend/src/main/kotlin/edu/minervia/platform/service/storage/R2StorageService.kt`
**方法**:
- `generatePresignedUploadUrl(objectKey, contentType, ttl): String`
- `generatePresignedDownloadUrl(objectKey, ttl): String`
- `headObject(objectKey): ObjectMetadata`
- `deleteObject(objectKey)`

**依赖**: Task 1.4

---

### Task 2.4: DocumentService
**文件**: `backend/src/main/kotlin/edu/minervia/platform/service/DocumentService.kt`
**方法**:
- `initializeUpload(studentId, request): InitUploadResponse`
- `completeUpload(studentId, documentId, request): DocumentDto`
- `listDocuments(studentId, pageable): Page<DocumentDto>`
- `getDownloadUrl(studentId, documentId): DownloadUrlResponse`
- `deleteDocument(studentId, documentId)`

**依赖**: Task 2.2, Task 2.3

---

### Task 2.5: EmailDelivery 实体
**文件**: `backend/src/main/kotlin/edu/minervia/platform/domain/entity/EmailDelivery.kt`
**依赖**: Task 1.3
**关键字段**: id, dedupeKey, status, attemptCount, nextAttemptAt

---

### Task 2.6: EmailDeliveryRepository
**文件**: `backend/src/main/kotlin/edu/minervia/platform/domain/repository/EmailDeliveryRepository.kt`
**方法**:
- `findByDedupeKey(dedupeKey): EmailDelivery?`
- `findByStatusAndNextAttemptAtBefore(status, time): List<EmailDelivery>`

---

### Task 2.7: EmailDeliveryService
**文件**: `backend/src/main/kotlin/edu/minervia/platform/service/email/EmailDeliveryService.kt`
**方法**:
- `createDelivery(recipient, template, params): EmailDelivery`
- `sendAsync(deliveryId)`
- `markSent(deliveryId, providerMessageId)`
- `markFailed(deliveryId, error)`
- `scheduleRetry(deliveryId, attemptCount)`

**依赖**: Task 2.6

---

### Task 2.8: Kafka EmailDeliveryConsumer
**文件**: `backend/src/main/kotlin/edu/minervia/platform/service/async/EmailDeliveryConsumer.kt`
**功能**: 消费 email.deliveries.send topic，调用 EmailService 发送
**依赖**: Task 2.7

---

## Phase 3: 后端 API 层 (优先级: 高)

### Task 3.1: StudentDocumentController
**文件**: `backend/src/main/kotlin/edu/minervia/platform/web/controller/StudentDocumentController.kt`
**端点**:
- `POST /api/student/documents`
- `POST /api/student/documents/{id}/complete`
- `GET /api/student/documents`
- `GET /api/student/documents/{id}/download-url`
- `DELETE /api/student/documents/{id}`

**依赖**: Task 2.4

---

### Task 3.2: AdminStatisticsController
**文件**: `backend/src/main/kotlin/edu/minervia/platform/web/controller/AdminStatisticsController.kt`
**端点**:
- `GET /api/admin/statistics/students`
- `GET /api/admin/statistics/registrations`
- `GET /api/admin/statistics/emails`

**权限**: AUDITOR 可访问

---

## Phase 4: 前端实现 (优先级: 高)

### Task 4.1: 管理员布局组件
**文件**: `frontend/src/app/[locale]/(admin)/admin/layout.tsx`
**功能**: Shell + Sidebar，根据角色显示菜单
**依赖**: 无

---

### Task 4.2: 管理员仪表板页面
**文件**: `frontend/src/app/[locale]/(admin)/admin/dashboard/page.tsx`
**功能**: 显示统计卡片（学生数、待审批数、邮件数）
**依赖**: Task 3.2

---

### Task 4.3: 申请审批页面
**文件**: `frontend/src/app/[locale]/(admin)/admin/applications/page.tsx`
**功能**: 列表、详情、批准/拒绝操作
**依赖**: 后端已有 API

---

### Task 4.4: 学生管理页面
**文件**: `frontend/src/app/[locale]/(admin)/admin/students/page.tsx`
**功能**: 搜索、列表、封禁/解封
**依赖**: 后端已有 API

---

### Task 4.5: 注册码管理页面
**文件**: `frontend/src/app/[locale]/(admin)/admin/codes/page.tsx`
**功能**: 生成单个/批量、列表、撤销
**依赖**: 后端已有 API

---

### Task 4.6: 审计日志页面
**文件**: `frontend/src/app/[locale]/(admin)/admin/audit/page.tsx`
**功能**: 查看审计日志、导出
**权限**: AUDITOR + SUPER_ADMIN
**依赖**: 后端已有 API

---

### Task 4.7: 系统配置页面
**文件**: `frontend/src/app/[locale]/(admin)/admin/config/page.tsx`
**功能**: 修改系统配置
**权限**: SUPER_ADMIN only
**依赖**: 后端已有 API

---

### Task 4.8: 管理员管理页面
**文件**: `frontend/src/app/[locale]/(admin)/admin/admins/page.tsx`
**功能**: 创建管理员、分配角色
**权限**: SUPER_ADMIN only
**依赖**: 后端已有 API

---

### Task 4.9: 学生文档上传组件
**文件**: `frontend/src/components/portal/document-upload.tsx`
**功能**: 文件选择、上传进度、错误处理
**依赖**: Task 3.1

---

### Task 4.10: 学生文档列表页面
**文件**: `frontend/src/app/[locale]/(portal)/portal/documents/page.tsx`
**功能**: 显示文档列表、下载、删除
**依赖**: Task 3.1, Task 4.9

---

## Phase 5: 测试与验证 (优先级: 中)

### Task 5.1: DocumentService 单元测试
**文件**: `backend/src/test/kotlin/edu/minervia/platform/service/DocumentServiceTest.kt`
**覆盖**: 上传初始化、完成、列表、删除逻辑

---

### Task 5.2: EmailDeliveryService 单元测试
**文件**: `backend/src/test/kotlin/edu/minervia/platform/service/EmailDeliveryServiceTest.kt`
**覆盖**: 幂等键生成、状态机转换、重试调度

---

### Task 5.3: AUDITOR 权限集成测试
**文件**: `backend/src/test/kotlin/edu/minervia/platform/integration/AuditorPermissionTest.kt`
**覆盖**: 访问控制矩阵验证

---

### Task 5.4: 文档上传流程集成测试
**文件**: `backend/src/test/kotlin/edu/minervia/platform/integration/DocumentUploadIntegrationTest.kt`
**覆盖**: 完整上传流程（mock R2）

---

### Task 5.5: 前端组件测试
**文件**: `frontend/src/__tests__/document-upload.test.tsx`
**覆盖**: DocumentUpload 组件、错误处理

---

## 验收标准

### 功能验收
- [ ] AUDITOR 可查看审计日志和统计，无法修改数据
- [ ] 学生可上传文档到 R2，列表显示，下载正常
- [ ] 邮件发送失败自动重试（最多 3 次）
- [ ] 管理员 8 个页面全部可访问且功能正常

### 性能验收
- [ ] 文档上传初始化 < 500ms
- [ ] 预签名 URL 生成 < 200ms
- [ ] 邮件重试队列积压 < 100 条

### 安全验收
- [ ] 学生无法访问他人文档
- [ ] AUDITOR 无法执行写操作
- [ ] 预签名 URL 过期后无法使用

---

