## Context

### 背景
Minervia Institute 教育平台核心功能框架已完成，但存在 10 处 TODO 占位符和多个未集成的服务模块。需要完成邮件发送、异步任务执行、前端页面等关键功能的实际集成。

### 当前状态
- 后端: Kotlin + Spring Boot 3.x + MySQL + Redis + Kafka
- 前端: Next.js 14 + TypeScript + Tailwind + shadcn/ui + next-intl
- 已完成: 管理后台、注册流程框架、身份生成引擎、审计日志
- 待完成: 邮件发送、异步任务步骤集成、公开官网、学生门户

### 约束
- 必须使用 SendGrid SMTP 中继发送邮件 (CONSTRAINT [MAIL-OUTBOUND])
- 前端路由必须遵循 [locale]/(marketing|portal|admin)/ 结构
- 学生认证必须与管理员认证隔离
- 照片生成暂用占位符图片

### 利益相关者
- 学生用户: 注册、登录、查看个人信息
- 管理员: 审批注册、管理学生
- 系统运维: 监控邮件发送、审计告警

## Goals / Non-Goals

**Goals:**
- 消除所有 TODO 占位符，完成服务集成
- 实现邮件发送功能 (验证码、欢迎邮件、拒绝通知)
- 完成异步任务三步骤的实际执行逻辑
- 创建公开官网 (marketing) 页面
- 扩展学生门户 (portal) 功能页面
- 实现审计告警的实际通知机制

**Non-Goals:**
- 不实现 FLUX.1 照片生成 (使用占位符)
- 不实现 Headless CMS (使用静态 JSON/MDX)
- 不实现 Slack/PagerDuty 告警 (仅邮件)
- 不修改现有 API 接口

## Decisions

### 1. 邮件服务架构

**决策**: EmailService 接口 + SendGridSmtpEmailService 实现

**实现**:
```kotlin
interface EmailService {
    fun send(to: String, template: EmailTemplate, params: Map<String, Any>): EmailDeliveryResult
}

class SendGridSmtpEmailService(
    private val mailSender: JavaMailSender,
    private val templateEngine: TemplateEngine,
    private val bounceService: EmailBounceService
) : EmailService
```

**配置**:
- SMTP Host: smtp.sendgrid.net
- SMTP Port: 587
- Username: apikey
- Password: ${SENDGRID_API_KEY}

**理由**:
- 接口抽象支持测试和未来供应商切换
- 复用 Spring Boot Starter Mail
- 与现有 EmailBounceService 集成

**备选方案**:
- SendGrid Web API: 更强大但需要额外依赖
- 直接 JavaMailSender: 缺少抽象，难以测试

### 2. 学生认证系统

**决策**: 独立认证系统，与管理员完全隔离

**实现**:
```kotlin
// 新增端点
POST /api/student/auth/login
POST /api/student/auth/refresh
POST /api/student/auth/logout

// JWT Claims
{
  "sub": "student_id",
  "actor_type": "STUDENT",
  "email": "student@minervia.edu.pl"
}
```

**数据模型**:
- 复用现有 students 表
- 新增 student_password_hash 字段
- 新增 student_sessions 表 (可选)

**理由**:
- 完全隔离避免权限泄露
- 学生和管理员有不同的安全需求
- 符合最小权限原则

**备选方案**:
- 共享认证 + 角色区分: 实现简单但隔离性差
- Magic Link: 用户体验好但不适合频繁登录

### 3. 照片生成策略

**决策**: UI Avatars 占位符图片

**实现**:
```kotlin
fun generatePlaceholderPhotoUrl(name: String): String {
    val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8)
    return "https://ui-avatars.com/api/?name=$encodedName&size=256&background=random"
}
```

**理由**:
- 零成本、零延迟
- 确定性生成 (相同名字相同头像)
- 后续可无缝替换为 FLUX.1

**备选方案**:
- DiceBear: 更多样式但 URL 更长
- FLUX.1: 真实照片但需要 GPU 资源

### 4. Major 数据管理

**决策**: 数据库表 + Flyway 迁移

**实现**:
```sql
CREATE TABLE majors (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(10) NOT NULL UNIQUE,
    name_en VARCHAR(100) NOT NULL,
    name_pl VARCHAR(100) NOT NULL,
    name_zh VARCHAR(100),
    faculty_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 种子数据
INSERT INTO majors (code, name_en, name_pl) VALUES
('CS', 'Computer Science', 'Informatyka'),
('BA', 'Business Administration', 'Administracja Biznesu'),
('ENG', 'Engineering', 'Inżynieria'),
('MED', 'Medicine', 'Medycyna');
```

**理由**:
- 支持动态管理专业
- 支持多语言名称
- 与现有 Flyway 迁移流程一致

**备选方案**:
- YAML 配置: 简单但不支持动态管理

### 5. 前端路由结构

**决策**: 严格遵循 [locale]/(group)/ 结构

**实现**:
```
app/[locale]/
├── (marketing)/
│   ├── layout.tsx          # MarketingLayout
│   ├── page.tsx            # Homepage
│   ├── about/page.tsx
│   ├── programs/page.tsx
│   └── admissions/page.tsx
├── (portal)/
│   ├── layout.tsx          # StudentLayout (with auth check)
│   ├── login/page.tsx      # Public
│   ├── register/...        # Public (existing)
│   ├── dashboard/page.tsx  # Protected
│   ├── profile/page.tsx    # Protected
│   └── courses/page.tsx    # Protected
└── (admin)/                # Existing
```

**理由**:
- 符合 CONSTRAINT [FRONTEND-ROUTE-STRUCTURE]
- 路由组提供布局隔离
- 中间件可按组应用认证

### 6. 异步任务步骤执行

**决策**: 每步独立事务 + 状态检查

**实现**:
```kotlin
class RegistrationStepExecutor {
    @Transactional(propagation = REQUIRES_NEW)
    fun executeIdentityRulesStep(application: RegistrationApplication) {
        if (studentRepository.existsByApplicationId(application.id)) {
            log.info("Student already exists, skipping identity step")
            return
        }
        // 执行身份生成
    }
}
```

**理由**:
- 每步独立事务避免长事务
- 状态检查实现幂等性
- 符合 CONSTRAINT [AI-STEP-TRANSACTION]

### 7. LlmPolishService Bean 冲突解决

**决策**: 删除重复 Bean，保留 identity.llm 包下的实现

**实现**:
- 删除 `service.llm.LlmPolishService`
- 保留 `service.identity.llm.LlmPolishService`
- 更新所有引用

**理由**:
- identity.llm 包下的实现更完整
- 避免 Spring Bean 名称冲突

### 8. WebSocket 生产环境配置

**决策**: 配置驱动的 Origin 限制

**实现**:
```yaml
# application-prod.yml
app:
  websocket:
    allowed-origins:
      - https://minervia.edu.pl
      - https://www.minervia.edu.pl
```

```kotlin
@ConfigurationProperties(prefix = "app.websocket")
data class WebSocketProperties(
    val allowedOrigins: List<String> = listOf("http://localhost:3000")
)
```

**理由**:
- 环境隔离
- 启动时验证配置
- 避免硬编码

## Risks / Trade-offs

### 1. 邮件发送失败风险
**风险**: SendGrid 不可用导致用户无法收到验��码
**缓解**:
- 实现重试机制 (3次指数退避)
- 记录发送状态供管理员查看
- 考虑备用 SMTP 服务器

### 2. 学生认证复杂度
**风险**: 独立认证系统增加维护成本
**缓解**:
- 复用现有 JWT 工具类
- 共享 Token 刷新逻辑
- 统一的认证中间件模式

### 3. 占位符图片依赖外部服务
**风险**: UI Avatars 服务不可用
**缓解**:
- 使用 CDN 缓存
- 实现本地 fallback (默认头像)
- 监控服务可用性

### 4. i18n 内容同步
**风险**: 多语言内容不同步导致缺失翻译
**缓解**:
- 构建时验证所有 locale 键完整性
- TypeScript 类型检查翻译键
- CI 流程检查

## Migration Plan

### 阶段 1: 后端服务集成
1. 创建 majors 表迁移脚本
2. 实现 EmailService 接口和 SendGrid 实现
3. 集成 RegistrationStepExecutor 三个步骤
4. 实现 AuditAlertService 邮件通知
5. 解决 LlmPolishService Bean 冲突

### 阶段 2: 学生认证
1. 添加学生密码字段迁移
2. 实现学生认证 API
3. 创建 StudentAuthContext 前端组件

### 阶段 3: 前端页面
1. 创建 (marketing) 路由组和页面
2. 扩展 (portal) 路由组页面
3. 添加 i18n 翻译内容

### 阶段 4: 测试和验证
1. 运行所有 PBT 测试
2. 验证邮件发送流程
3. 验证学生登录流程

### 回滚策略
- 数据库迁移: Flyway 支持回滚
- 代码变更: Git revert
- 配置变更: 环境变量回滚

## Open Questions

1. **邮件模板设计**: 是否需要设计师提供 HTML 模板？
2. **学生密码策略**: 是否与管理员使用相同的密码复杂度要求？
3. **Marketing 页面内容**: 具体文案由谁提供？
