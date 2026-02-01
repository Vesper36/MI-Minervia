## Why

项目核心功能框架已完成，但存在 10 处 TODO 占位符和多个未集成的服务模块。需要完成邮件发送、照片生成、异步任务执行等关键功能的实际集成，并补充缺失的前端页面（公开官网、学生门户），使平台达到生产可用状态。

## What Changes

### 后端服务集成
- 实现邮件发送服务，集成 SendGrid SMTP 中继
- 完成异步任务执行步骤（身份生成、LLM 润色、照片生成）的实际集成
- 实现审计告警通知机制（邮件通知）
- 完善 WebSocket 生产环境配置

### 前端页面补充
- 创建 `(marketing)` 公开官网路由组和页面
- 扩展 `(portal)` 学生门户功能页面

### 代码清理
- 消除所有 TODO 占位符
- 完成 Major 表查询逻辑

## Capabilities

### New Capabilities

- `email-sending`: 邮件发送服务实现，包括验证码邮件、欢迎邮件、拒绝通知邮件，集成 SendGrid API
- `async-task-integration`: 异步任务执行步骤的实际集成，连接 IdentityGenerationService、LlmPolishService 和照片生成
- `marketing-website`: 公开官网前端页面，包括首页、关于页面、专业介绍、招生信息
- `student-portal`: 学生门户扩展页面，包括仪表板、个人资料、课程列表

### Modified Capabilities

- `audit-logging`: 补充告警通知的实际实现（邮件发送），原 spec 中定义了告警机制但未指定通知渠道

## Impact

### 后端代码
- `EmailVerificationService.kt` - 集成实际邮件发送
- `StudentService.kt` - 添加欢迎邮件发送
- `RegistrationService.kt` - 添加拒绝邮件和异步任务触发
- `RegistrationStepExecutor.kt` - 集成三个执行步骤
- `AuditAlertService.kt` - 实现通知机制
- `WebSocketConfig.kt` - 生产环境配置

### 前端代码
- 新增 `app/[locale]/(marketing)/` 路由组
- 扩展 `app/[locale]/(portal)/` 路由组

### 依赖
- SendGrid SDK (已在 pom 中配置)
- 无新增外部依赖

### API
- 无 API 变更，仅内部实现完善
