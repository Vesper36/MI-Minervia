# Tasks: High Completion Phase

## 1. 数据库迁移

- [x] 1.1 创建 majors 表迁移脚本 (V3__majors_table.sql)
- [x] 1.2 添加 majors 种子数据 (CS, BA, ENG, MED)
- [x] 1.3 创建 audit_notifications 表迁移脚本
- [x] 1.4 添加 students 表 password_hash 字段迁移
- [x] 1.5 添加 students 表 welcome_email_sent_at 字段迁移
- [x] 1.6 添加 registration_applications 表 rejection_email_sent_at 字段迁移

## 2. 邮件服务实现

- [x] 2.1 创建 EmailService 接口 (service/email/EmailService.kt)
- [x] 2.2 创建 EmailTemplate 枚举 (VERIFICATION, WELCOME, REJECTION)
- [x] 2.3 创建 EmailDeliveryResult 数据类
- [x] 2.4 实现 SendGridSmtpEmailService
- [x] 2.5 创建邮件模板目录结构 (resources/email-templates/{locale}/)
- [x] 2.6 创建验证码邮件模板 (verification.html)
- [x] 2.7 创建欢迎邮件模板 (welcome.html)
- [x] 2.8 创建拒绝通知邮件模板 (rejection.html)
- [x] 2.9 集成 EmailVerificationService.sendVerificationCode() 调用 EmailService
- [x] 2.10 集成 StudentService.createStudentFromApplication() 发送欢迎邮件
- [x] 2.11 集成 RegistrationService.rejectApplication() 发送拒绝邮件

## 3. Major 数据管理

- [x] 3.1 创建 Major 实体类 (domain/entity/Major.kt)
- [x] 3.2 创建 MajorRepository 接口
- [x] 3.3 创建 MajorService 服务类
- [x] 3.4 修改 StudentService.getMajorCode() 从数据库查询

## 4. 异步任务步骤集成

- [x] 4.1 解决 LlmPolishService Bean 冲突 (删除 service/identity/LlmPolishService.kt 重复类)
- [x] 4.2 实现 RegistrationStepExecutor.executeIdentityRulesStep() 集成 IdentityGenerationService
- [x] 4.3 实现 RegistrationStepExecutor.executeLlmPolishStep() 集成 LlmPolishService
- [x] 4.4 实现 PlaceholderPhotoService 生成 UI Avatars URL
- [x] 4.5 实现 RegistrationStepExecutor.executePhotoGenerationStep() 集成 PlaceholderPhotoService
- [x] 4.6 实现 RegistrationService.approveApplication() 触发异步任务 (Outbox 模式)
- [x] 4.7 添加步骤幂等性检查 (检查 Student 是否已存在)

## 5. 审计告警通知

- [x] 5.1 创建 AlertNotificationService 接口
- [x] 5.2 实现 EmailAlertNotificationService
- [x] 5.3 实现告警去重逻辑 (Redis key: alert:{type}:{actor}:{hour})
- [x] 5.4 创建 audit_notifications 表 Repository
- [x] 5.5 修改 AuditAlertService.notifySuperAdmins() 调用 AlertNotificationService
- [x] 5.6 实现通知记录持久化

## 6. 学生认证系统

- [x] 6.1 创建 StudentAuthController (/api/student/auth/*)
- [x] 6.2 实现学生登录 API (POST /api/student/auth/login)
- [x] 6.3 实现学生 Token 刷新 API (POST /api/student/auth/refresh)
- [x] 6.4 实现学生登出 API (POST /api/student/auth/logout)
- [x] 6.5 创建 StudentJwtService (actor_type=STUDENT claim)
- [x] 6.6 创建 StudentAuthenticationFilter
- [x] 6.7 配置 SecurityConfig 学生端点权限

## 7. WebSocket 生产配置

- [x] 7.1 创建 WebSocketProperties 配置类
- [x] 7.2 修改 WebSocketConfig 使用配置驱动的 allowedOrigins
- [x] 7.3 添加 application-prod.yml WebSocket 配置

## 8. 前端 Marketing 页面

- [x] 8.1 创建 (marketing) 路由组目录结构
- [x] 8.2 创建 MarketingLayout 组件
- [x] 8.3 创建 MarketingNavbar 组件
- [x] 8.4 创建 MarketingFooter 组件
- [x] 8.5 创建首页 (marketing)/page.tsx
- [x] 8.6 创建关于页面 (marketing)/about/page.tsx
- [x] 8.7 创建专业页面 (marketing)/programs/page.tsx
- [x] 8.8 创建招生页面 (marketing)/admissions/page.tsx
- [x] 8.9 为所有 Marketing 页面添加 generateMetadata
- [x] 8.10 添加 Marketing.* i18n 翻译键 (en.json)
- [x] 8.11 添加 Marketing.* i18n 翻译键 (pl.json)
- [x] 8.12 添加 Marketing.* i18n 翻译键 (zh-CN.json)

## 9. 前端 Student Portal 页面

- [x] 9.1 创建 StudentAuthContext 组件
- [x] 9.2 创建 StudentLayout 组件 (带侧边栏)
- [x] 9.3 创建学生登录页面 (portal)/login/page.tsx
- [x] 9.4 创建学生仪表板页面 (portal)/dashboard/page.tsx
- [x] 9.5 创建学生个人资料页面 (portal)/profile/page.tsx
- [x] 9.6 创建学生课程页面 (portal)/courses/page.tsx
- [x] 9.7 实现 Portal 路由保护中间件
- [x] 9.8 添加 Portal.* i18n 翻译键 (en.json)
- [x] 9.9 添加 Portal.* i18n 翻译键 (pl.json)
- [x] 9.10 添加 Portal.* i18n 翻译键 (zh-CN.json)

## 10. 测试

- [ ] 10.1 编写 EmailService 单元测试
- [ ] 10.2 编写 StudentAuthController 集成测试
- [ ] 10.3 编写 RegistrationStepExecutor 集成测试
- [ ] 10.4 编写 PBT-EMAIL-01 邮件幂等性测试
- [ ] 10.5 编写 PBT-PORTAL-01 学生会话隔离测试
- [ ] 10.6 编写 PBT-ASYNC-01 步骤完成顺序测试
- [ ] 10.7 编写 PBT-MARKETING-02 i18n 完整性测试
- [ ] 10.8 编写前端 Marketing 页面组件测试
- [ ] 10.9 编写前端 Portal 页面组件测试
- [ ] 10.10 运行完整测试套件验证
