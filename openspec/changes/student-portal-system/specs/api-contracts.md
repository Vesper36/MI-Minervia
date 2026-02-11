# API Contracts Specification

> 定义所有 API 端点的请求/响应契约、错误码、验证规则

## 错误码枚举

### 文档系统错误码

| 错误码 | HTTP 状态 | 说明 |
|--------|----------|------|
| `DOCUMENT_NOT_FOUND` | 404 | 文档不存在 |
| `DOCUMENT_INVALID_TYPE` | 422 | 不支持的文件类型 |
| `DOCUMENT_TOO_LARGE` | 422 | 文件超过大小限制 |
| `DOCUMENT_UPLOAD_FAILED` | 500 | 上传失败 |
| `DOCUMENT_NOT_ACTIVE` | 400 | 文档未激活 |
| `DOCUMENT_SERVICE_UNAVAILABLE` | 503 | R2 服务不可用 |

### 邮件系统错误码

| 错误码 | HTTP 状态 | 说明 |
|--------|----------|------|
| `EMAIL_SEND_FAILED` | 500 | 邮件发送失败 |
| `EMAIL_SUPPRESSED` | 400 | 收件人在黑名单 |
| `EMAIL_INVALID_RECIPIENT` | 422 | 无效的收件人地址 |

### 权限错误码

| 错误码 | HTTP 状态 | 说明 |
|--------|----------|------|
| `INSUFFICIENT_PERMISSIONS` | 403 | 权限不足 |
| `AUDITOR_WRITE_FORBIDDEN` | 403 | 审计员无写权限 |

---

## 文档管理 API 详细规范

### POST /api/student/documents

**请求验证规则**：
- `fileName`: 必填，长度 1-255，仅允许 `[a-zA-Z0-9._-]`
- `contentType`: 必填，必须在白名单内
- `sizeBytes`: 必填，范围 1 - 10485760 (10MB)

**白名单 Content-Type**：
```
application/pdf
image/jpeg
image/png
application/vnd.openxmlformats-officedocument.wordprocessingml.document
```

**成功响应 (200)**：
```json
{
  "success": true,
  "data": {
    "documentId": 123,
    "uploadUrl": "https://...",
    "expiresAt": "2026-02-10T05:15:00Z",
    "requiredHeaders": {
      "Content-Type": "application/pdf",
      "Content-Length": "1048576"
    }
  },
  "timestamp": "2026-02-10T05:00:00Z"
}
```

**错误响应示例**：
```json
{
  "success": false,
  "error": {
    "code": "DOCUMENT_TOO_LARGE",
    "message": "File size exceeds 10MB limit",
    "details": {
      "maxSize": 10485760,
      "actualSize": 15728640
    }
  },
  "timestamp": "2026-02-10T05:00:00Z"
}
```

---

### POST /api/student/documents/{id}/complete

**路径参数**：
- `id`: 文档 ID（必须是当前学生拥有的文档）

**请求体**：
```json
{
  "etag": "\"abc123\"",
  "sha256": "optional-hash"
}
```

**验证流程**：
1. 验证文档归属（student_id 匹配）
2. 验证状态为 PENDING_UPLOAD
3. HEAD 请求 R2 验证对象存在
4. 验证 Content-Type 和 Content-Length 匹配
5. 更新状态为 ACTIVE

**幂等性保证**：
- 重复调用返回相同结果
- 已 ACTIVE 的文档返回 200 + 当前状态

**错误场景**：
- 文档不存在 → 404 `DOCUMENT_NOT_FOUND`
- 文档不属于当前学生 → 403 `INSUFFICIENT_PERMISSIONS`
- 对象不存在 → 400 `DOCUMENT_UPLOAD_FAILED`
- 大小/类型不匹配 → 400 `DOCUMENT_UPLOAD_FAILED`

---

