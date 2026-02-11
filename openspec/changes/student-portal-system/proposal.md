# Proposal: 学生门户系统完整实现

## Context

### 用户需求
实现隐蔽隐藏之下真实的学生登录系统，包括：
- 学生注册与登录
- 管理员管理与审批
- 学生后台门户
- 邮件收发系统
- 课程管理
- 设置配置
- 证件证明材料生成

### 现有系统状态
基于代码库调研，系统已实现以下核心功能：

**后端（Kotlin + Spring Boot）**：
- JWT 双 Token 认证机制（管理员 + 学生分离）
- 学生注册流程（多步骤 + 邮箱验证）
- 管理员审批系统（批准/拒绝申请）
- 学生身份生成（LLM 驱动）
- 邮件服务（SendGrid SMTP + 模板渲染）
- 审计日志（Elasticsearch）
- 异步任务（Kafka + Outbox 模式）

**前端（Next.js + TypeScript）**：
- 学生门户页面（dashboard、profile、courses、documents、settings）
- 学生认证上下文（login、logout、token 管理）
- 营销网站（多语言支持）

**缺失部分**：
- 管理员前端页面（完全缺失）
- 学生文档存储与管理（仅占位符）
- 邮件发送失败重试机制
- 审计员角色权限


## Discovered Constraints

### 硬约束（Hard Constraints）

#### 认证授权
- JWT 双 Token 机制：Access Token 30分钟，Refresh Token 14天
- 管理员与学生认证完全分离（不同的 JWT Service）
- Token 撤销使用 Redis 黑名单机制
- 管理员支持 TOTP 二步验证（可选）
- 账户锁定机制：失败登录超限后锁定

#### 角色权限
- 管理员三级角色：SUPER_ADMIN、ADMIN、AUDITOR（新增）
- SUPER_ADMIN 继承所有 ADMIN 权限
- AUDITOR 仅可查看审计日志和统计数据
- 学生状态：ACTIVE、SUSPENDED、GRADUATED、EXPELLED
- 权限控制使用 `@PreAuthorize` 注解

#### 数据模型
- 学生学号（studentNumber）全局唯一
- 学生 EDU 邮箱（eduEmail）全局唯一
- 管理员用户名（username）全局唯一
- 注册码（code）全局唯一，大写字母+数字

#### 邮件系统
- 邮件发送前必须检查黑名单
- 邮件模板支持多语言（en/pl/zh-CN）
- 邮箱验证码 6 位数字，15 分钟有效期，最多 5 次尝试
- 邮件反弹处理：硬反弹立即黑名单，软反弹 72 小时内 5 次自动黑名单
- 邮件发送失败使用 Kafka 异步队列重试（新增）

#### 文档存储
- 使用 Cloudflare R2 对象存储（兼容 S3 API）
- 文档访问需要学生 JWT 认证
- 文档 URL 使用预签名机制（有效期 1 小时）


### 软约束（Soft Constraints）

#### 代码规范
- Kotlin 惯用语法（data class、sealed class、extension functions）
- 服务层使用构造器注入
- DTO 与实体分离
- 异常处理统一（NoSuchElementException、IllegalArgumentException、BadCredentialsException）
- 事务管理使用 `@Transactional` 标注

#### 前端规范
- 组件使用函数式组件 + Hooks
- 样式使用 Tailwind CSS utility classes
- 国际化通过 next-intl 管理
- 路由分组：`(admin)`、`(portal)`、`(marketing)`
- API 调用使用专用客户端（apiClient、studentApiClient）

#### 命名约定
- 后端路由前缀：`/api/admin/*`、`/api/student/*`、`/api/auth/*`
- DTO 命名：`*Request`、`*Response`、`*Dto`
- 前端页面：`page.tsx`、组件：`*.tsx`
- 测试文件：`*Test.kt`、`*.test.tsx`


## Dependencies

### 跨模块依赖关系

#### 认证授权依赖
- AuthService → JwtService → TokenRevocationService (Redis)
- StudentAuthService → StudentJwtService → TokenRevocationService
- SecurityConfig → JwtAuthenticationFilter → JwtService
- 前端 auth-context → apiClient → 后端 /api/auth/*

#### 学生管理依赖
- StudentService → IdentityGenerationService → LLM (OpenAI)
- StudentService → EmailService → EmailTemplateRenderer
- StudentPortalController → StudentService → StudentRepository
- 前端 student-auth-context → studentApiClient → 后端 /api/student/*

#### 管理员管理依赖
- AdminService → AdminRepository → Admin Entity
- RegistrationService → RegistrationApplicationRepository
- RegistrationService → Kafka (异步任务) → IdentityGenerationService
- 前端 admin pages → apiClient → 后端 /api/admin/*

#### 邮件系统依赖
- EmailService → EmailBounceService → EmailSuppressionRepository
- EmailService → SendGrid SMTP → JavaMailSender
- EmailVerificationService → EmailService → EmailTemplateRenderer
- EmailRetryService (新增) → Kafka → EmailService

#### 文档系统依赖
- DocumentService (新增) → Cloudflare R2 SDK
- StudentPortalController → DocumentService
- 前端 documents page → studentApiClient → 后端 /api/student/documents


## Risks

### 安全风险
1. **管理员前端缺失**：无法使用后端 API，需完整实现
2. **邮件模板 XSS**：参数未进行 HTML 转义
3. **CSV 公式注入**：防护不完整
4. **验证码存储**：未加密存储
5. **IP 限制缺失**：无白名单、无登录地点限制
6. **注册码滥用**：无使用次数限制、无 IP 绑定

### 架构风险
1. **邮件发送失败**：当前不重试，需实现 Kafka 异步重试
2. **LLM 降级**：身份生成失败时质量不一致
3. **批量操作**：部分成功结果需客户端处理
4. **文档功能缺失**：当前为空实现

### 运维风险
1. **异步任务失败**：需验证重试机制
2. **审计日志不完整**：管理员操作未记录
3. **权限粒度粗**：需添加审计员角色


## Success Criteria

### 认证授权
- [ ] 管理员能成功登录并获得有效的 JWT Token
- [ ] SUPER_ADMIN 能创建新的管理员账户并分配角色
- [ ] AUDITOR 能查看审计日志但无法修改数据
- [ ] 学生能通过 EDU 邮箱或学号登录
- [ ] Token 刷新机制正常工作
- [ ] 登出后 Token 立即失效
- [ ] TOTP 二步验证正常工作
- [ ] 账户锁定机制正常触发

### 学生管理
- [ ] 管理员能查看待审批的学生申请列表
- [ ] 管理员能批准申请，触发异步学生身份生成
- [ ] 管理员能拒绝申请，发送拒绝邮件
- [ ] 管理员能搜索、编辑、暂停学生账户
- [ ] 学生身份生成成功率 > 95%
- [ ] 批量操作支持事务回滚

### 注册码管理
- [ ] 管理员能生成单个或批量注册码
- [ ] 注册码验证正确识别过期、已使用、已撤销状态
- [ ] 管理员能撤销未使用的注册码


### 邮件系统
- [ ] 邮件发送前正确检查黑名单
- [ ] 邮件模板多语言渲染正常
- [ ] 邮箱验证码在 15 分钟内有效
- [ ] 验证码尝试 5 次失败后需要重新请求
- [ ] 邮件反弹正确处理并更新黑名单
- [ ] 邮件发送失败自动进入 Kafka 重试队列
- [ ] 重试机制使用指数退避策略

### 文档系统
- [ ] 学生能上传文档到 Cloudflare R2
- [ ] 学生能查看自己的文档列表
- [ ] 学生能下载文档（使用预签名 URL）
- [ ] 文档访问需要 JWT 认证
- [ ] 文档 URL 1 小时后自动过期
- [ ] 支持的文档类型：PDF、JPG、PNG、DOCX

### 管理员前端
- [ ] 管理员登录页面正常工作
- [ ] 管理员仪表板显示待审批申请数、学生统计
- [ ] 申请审批页面支持列表、详情、批准/拒绝操作
- [ ] 学生管理页面支持搜索、编辑、暂停/激活
- [ ] 注册码管理页面支持生成、查看、撤销
- [ ] 管理员管理页面支持创建、更新角色、激活/停用（SUPER_ADMIN only）
- [ ] 审计日志页面支持查看操作记录（AUDITOR + SUPER_ADMIN）
- [ ] 系统配置页面支持修改系统参数（SUPER_ADMIN only）


## Implementation Sequence

### Phase 1: 基础设施增强（优先级：高）
1. 添加 AUDITOR 角色到 AdminRole 枚举
2. 实现邮件发送失败 Kafka 重试机制
3. 集成 Cloudflare R2 SDK
4. 修复邮件模板 XSS 漏洞（HTML 转义）

### Phase 2: 文档系统实现（优先级：高）
1. 创建 Document 实体和 DocumentRepository
2. 实现 DocumentService（上传、下载、列表、删除）
3. 实现 DocumentController（/api/student/documents）
4. 实现前端文档页面（上传、列表、下载）

### Phase 3: 管理员前端实现（优先级：高）
1. 创建管理员登录页面（/[locale]/admin/login）
2. 创建管理员仪表板（/[locale]/admin/dashboard）
3. 创建申请审批页面（/[locale]/admin/applications）
4. 创建学生管理页面（/[locale]/admin/students）
5. 创建注册码管理页面（/[locale]/admin/codes）
6. 创建管理员管理页面（/[locale]/admin/admins）- SUPER_ADMIN only
7. 创建审计日志页面（/[locale]/admin/audit）- AUDITOR + SUPER_ADMIN
8. 创建系统配置页面（/[locale]/admin/config）- SUPER_ADMIN only

### Phase 4: 安全增强（优先级：中）
1. 实现 IP 白名单机制
2. 实现注册码使用次数限制
3. 加密存储邮箱验证码
4. 完善 CSV 公式注入防护

### Phase 5: 测试与优化（优先级：中）
1. 补充管理员前端组件测试
2. 补充文档系统集成测试
3. 补充邮件重试机制测试
4. 性能优化与压力测试

