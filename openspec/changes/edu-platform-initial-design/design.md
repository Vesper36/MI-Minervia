# Design: Minervia Institute 教育平台技术设计

## Context

### 背景
创建一个完整的教育机构在线平台，模拟真实大学的所有核心功能。该平台需要满足波兰教育法和 GDPR 合规要求，同时提供高度真实的用户体验。

### 当前状态
- 空白项目，从零开始构建
- 用户已有独立的 MySQL 数据库 VPS
- 需要集成 AI 生成技术（身份信息和照片）
- 需要自建邮件服务器

### 约束
- 必须符合波兰教育法（Prawo oświatowe）和 GDPR (RODO)
- 数据保留期限 5 年
- 邮件发送限制默认每日 1 件
- 所有生成内容必须标记为"模拟/测试"用途

### 利益相关者
- 学生用户（注册、使用 EDU 邮箱、生成证明文件）
- 管理员（审批注册、管理账号、监控邮件）
- 系统运维（维护邮件服务器、AI 服务、数据库）

## Goals / Non-Goals

**Goals:**
- 构建功能完整的教育平台（官网、门户、邮箱、管理后台）
- 实现高度真实的用户体验（与真实大学无法区分）
- 确保 GDPR 和波兰教育法合规
- 支持多语言（英语、波兰语、中文、其他欧洲语言）
- 实现智能身份信息生成和 AI 照片生成
- 提供完整的审计日志和数据可视化

**Non-Goals:**
- 不实现真实的学术管理系统（课程管理、成绩录入等）
- 不提供在线学习平台（LMS）
- 不实现支付系统（学费缴纳）
- 不提供移动应用（仅 Web 平台）

## Decisions

### 1. 前端技术栈

**决策**: Next.js 14+ (App Router) + TypeScript + Tailwind CSS + shadcn/ui

**理由**:
- Next.js 14 的 App Router 支持服务端组件（RSC），适合官网 SEO 优化
- TypeScript 提供类型安全，减少运行时错误
- Tailwind CSS 快速构建响应式界面
- shadcn/ui 基于 Radix UI，兼顾管理后台效率和官网可定制性

**备选方案**:
- Vue 3 + Nuxt 3: 学习曲线较平缓，但生态不如 React 成熟
- React + Vite: 构建速度快，但缺少 SSR 和 SEO 优化

### 2. 后端技术栈

**决策**: Kotlin/Java + Spring Boot + MySQL

**理由**:
- Spring Boot 生态成熟，适合企业级应用
- Kotlin 提供更简洁的语法和空安全
- MySQL 为用户现有资源，节省成本
- 支持复杂业务逻辑和事务管理

**备选方案**:
- Node.js + NestJS: 开发效率高，但不如 Spring Boot 成熟
- Python + FastAPI: 适合快速迭代，但性能不如 JVM
- Rust + Actix-web: 性能极佳，但学习曲线陡峭

### 3. 代码隔离策略

**决策**: Next.js Route Groups 分离公开官网和隐藏门户

**实现**:
```
app/
├── (marketing)/          # 公开官网
│   ├── page.tsx         # 首页
│   ├── about/           # 关于我们
│   └── admissions/      # 招生信息
├── (portal)/            # 隐藏门户
│   ├── dashboard/       # 学生仪表盘
│   ├── email/           # 邮箱系统
│   └── credentials/     # 证明文件
└── (admin)/             # 管理后台
    ├── students/        # 学生管理
    ├── registrations/   # 注册审批
    └── analytics/       # 数据可视化
```

**理由**:
- 物理隔离确保隐藏门户不会在官网导航中暴露
- 中间件层面拦截，确保 `(portal)` 和 `(admin)` 路径必须鉴权
- 共享底层设计系统，保持品牌一致性

### 4. 身份信息生成算法

**决策**: 规则引擎 + 约束校验 + LLM 润色（三段式）

**实现流程**:
1. **规则引擎**: 根据国别/文化规则生成结构化字段
   - 姓名：基于国家姓名库（波兰、中国、美国等）
   - 生日：根据入学年份反推合理年龄范围
   - 学号：按照 `{年份}{学院代码}{序号}` 格式生成
   - 时间线：入学日期、录取日期、学期设定

2. **约束校验**: 确保逻辑一致性
   - 年龄与学籍一致性（18-25 岁本科生）
   - 时间线一致性（录取日期 < 入学日期）
   - 专业与学院一致性

3. **LLM 润色**: 生成自然语言描述
   - 家庭背景描述
   - 个人兴趣爱好
   - 学术目标

**理由**:
- 规则引擎保证逻辑一致性和可审计性
- 约束校验防止数据矛盾
- LLM 润色提升真实感和多样性

**备选方案**:
- 纯规则引擎：一致性强但多样性不足
- 纯 AI 生成：多样性强但一致性弱，成本高

### 5. 照片生成方案

**决策**: FLUX.1 自建���署（GPU）

**实现**:
- 使用 FLUX.1-dev 模型（开源）
- 部署在云 GPU 实例（NVIDIA A100/H100）
- 通过 API 接口调用生成服务
- 生成参数：
  - 证件照：正面、白色背景、正式着装
  - 校园照片：校园场景、休闲着装、自然表情

**理由**:
- 开源免费，可私有化部署
- 完全可控，无第三方依赖
- 长期成本低于 API 调用

**备选方案**:
- API 调用（Replicate/FAL.ai）：快速启动但长期成本高
- Stable Diffusion：成熟但质量不如 FLUX.1

### 6. 邮件系统架构

**决策**: 自建 Postfix + Dovecot + Rspamd（收件）+ 托管SMTP外发

**架构**:
```
[外发邮件] → [托管SMTP中继: SendGrid/Mailgun] → [收件方]
[收件邮件] → [自建Postfix MTA] → [Rspamd 反垃圾] → [Dovecot IMAP/POP3] → [用户]
```

**配置要点**:
- 外发使用托管SMTP中继（解决IP信誉问题）
- 收件保持自建（完全可控）
- SPF/DKIM/DMARC 配置
- Rspamd 集成 ClamAV 防病毒
- Maildir 格式存储，支持备份到 S3

**CONSTRAINT [MAIL-OUTBOUND]**: 外发邮件使用托管SMTP中继服务（SendGrid或Mailgun），不自建外发。

**理由**:
- 收件完全可控，支持深度定制
- 外发通过托管服务保证送达率
- 降低IP信誉管理复杂度

### 7. AI 生成执行策略

**决策**: 异步生成 + Spring WebSocket/STOMP 实时进度

**实现流程**:
1. 用户提交注册申请
2. 后端创建异步任务（Kafka 消息队列）
3. 任务状态：`PENDING` -> `GENERATING_IDENTITY` -> `GENERATING_PHOTOS` -> `COMPLETED`
4. 前端通过 STOMP 客户端（`@stomp/stompjs`）订阅任务进度
5. 实时更新进度条和状态提示
6. 完成后通知用户（浏览器通知 + 邮件）

**技术参数**:
- 消息队列：Kafka（高吞吐量、持久化、分布式）
- 实时通信：Spring WebSocket/STOMP（原生支持，无需额外依赖）
- 并发限制：同时处理 5 个 AI 生成任务（小规模 <50/天）
- 失败处理：阻塞式重试 3 次（指数退避），全部失败后标记 `FAILED`，管理员手动介入

**CONSTRAINT [ASYNC-IDEMPOTENCY]**: 任务处理使用 applicationId 作为幂等键。Kafka重复投递不产生重复账户/邮件/身份记录。数据库使用唯一约束强制。

**CONSTRAINT [ASYNC-OUTBOX]**: 批准操作使用事务性Outbox模式:
1. 在同一事务中更新申请状态 + 插入outbox表
2. 独立进程轮询outbox发送Kafka消息
3. Kafka不可用时任务保留在outbox，不阻塞批准操作

**PBT [PBT-07]**: 任务幂等性 - 重复投递/重试不创建重复记录
**PBT [PBT-09]**: 重试边界 - 最多3次，指数退避后进入FAILED终态

**理由**:
- AI 生成耗时长（10-60 秒），同步等待体验差
- Spring WebSocket/STOMP 生态成熟，无需第三方依赖
- Kafka 支持异步任务持久化和重试机制
- 阻塞式重试确保生成质量，防止空数据进入系统

**备选方案**:
- Socket.IO：需要 netty-socketio 或独立 Node.js 服务器
- NATS JetStream：轻量级但生态不如 Kafka 成熟
- 同步生成：实现简单但体验差

### 8. 数据库架构

**决策**: MySQL 主库 + Redis 缓存 + MySQL分区审计（MVP阶段不用ES）

**架构**:
```
[应用层]
    ↓
[MySQL 主库] ← [读写分离] → [MySQL 从库]
    ↓
[Redis 缓存] (会话/验证码/限流, AOF持久化)
    ↓
[MySQL 分区表] (审计日志按月分区)
```

**表设计要点**:
- 学���表：按入学年份分区（注意唯一约束需包含分区键）
- 审计日志表：按月分区，仅存假名标识（PII分离存储）
- 全局唯一字段（email/student_number）使用独立查找表

**CONSTRAINT [DB-AUDIT-PII-SEPARATION]**: 审计日志表仅存储假名标识(user_id_hash)，可识别信息(姓名/邮箱)存储在独立的`audit_pii`表，通过log_id关联。GDPR删除时只需清理audit_pii表。

**CONSTRAINT [DB-ES-THRESHOLD]**: MVP阶段使用MySQL分区+索引。当审计查询量 >1000次/天 或 数据量 >500万行 时引入Elasticsearch。

**PBT [PBT-10]**: 审计日志不可变 - UPDATE/DELETE或哈希篡改被完整性校验检测
**PBT [PBT-11]**: 时间戳单调性 - (created_at, id) 排序反映追加顺序

**理由**:
- MySQL 为用户现有资源，节省成本
- Redis 提升高频访问性能（需开启AOF防止重启丢失计数器）
- 延迟引入ES降低MVP复杂度

### 9. 安全参数

**决策**:
- 密码复杂度：最小 8 位，包含大小写字母 + 数字
- 密码加密：bcrypt (cost=12)
- 登录失败：5 次失败后锁定 30 分钟
- JWT 过期：管理员可自定义（无强制上限）

**CONSTRAINT [JWT-NO-REVOCATION]**: 当前版本不实现JWT主动吊销。角色/密码变更后旧token在过期前仍有效。后续版本可引入refresh token + 吊销列表。

**理由**:
- bcrypt cost=12 平衡安全性和性能
- 5 次失败 + 30 分钟锁定防止暴力破解
- 无上限JWT简化实现，适合小规模信任环境

### 10. 多语言支持

**决策**: next-intl + 基于路由的国际化

**实现**:
```
/en/...        # 英语（默认）
/pl/...        # 波兰语
/zh-CN/...     # 简体中文
/zh-TW/...     # 繁体中文
/de/...        # 德语
/fr/...        # 法语
```

**内容管理**:
- 短文案：JSON 翻译文件
- 长内容：Headless CMS（Strapi 或 Contentlayer）

**理由**:
- 基于路由的国际化 SEO 友好
- JSON 文件适合短文案，CMS 适合长内容
- next-intl 生态成熟，支持服务端组件

## Risks / Trade-offs

### 1. 邮件服务器运维风险
**风险**: IP 信誉管理困难，可能被标记为垃圾邮件
**缓解**:
- 独立 IP 池，定期监控信誉
- 配置 SPF/DKIM/DMARC
- 使用 Rspamd 防止发送垃圾邮件
- 建立退信处理和黑名单修复流程

### 2. AI 生成成本和性能
**风险**: GPU 成本高，生成速度慢
**缓解**:
- 使用云 GPU 按需计费
- 批量生成降低单次成本
- 实现缓存机制（相似参数复用）
- 设置生成超时和降级策略

### 3. 数据一致性风险
**风险**: 身份信息生成可能出现逻辑矛盾
**缓解**:
- 强制约束校验，失败即回滚
- 所有字段基于单一时间线模型派生
- 完整的单元测试覆盖
- 审计日志记录所有生成过程

### 4. GDPR 合规风险
**风险**: 数据删除与审计日志冲突
**缓解**:
- 分离存储"可识别数据"与"审计事件"
- 审计仅保留不可识别主键与散列
- 执行可逆映射清理
- 明确的数据保留政策（5 年）

### 5. 法律和伦理风险
**风险**: 创建虚假教育机构可能违法
**缓解**:
- 所有生成内容标记为"模拟/测试"
- 限制证明文件使用范围
- 实现水印或防伪机制
- 定期法律合规审查

### 6. 系统复杂度风险
**风险**: 多个子系统集成复杂，维护成本高
**缓解**:
- 模块化单体架构，明确领域边界
- 完整的 API 文档（OpenAPI）
- 自动化测试和 CI/CD
- 详细的运维文档

## Migration Plan

### 阶段 1: 基础设施搭建（Week 1-2）
1. 配置 MySQL 数据库（用户现有 VPS）
2. 部署 Redis 缓存
3. 搭建 Postfix + Dovecot 邮件服务器
4. 配置 SPF/DKIM/DMARC

### 阶段 2: 核心功能开发（Week 3-6）
1. 实现注册流程（注册码 + 管理员批准）
2. 开发身份信息生成系统
3. 集成 FLUX.1 照片生成
4. 实现 EDU 邮箱系统

### 阶段 3: 前端开发（Week 7-10）
1. 构建公开官网（多语言）
2. 开发隐藏学生门户
3. 实现管理后台
4. 集成 STOMP/WebSocket 实时进度

### 阶段 4: 测试和优化（Week 11-12）
1. 功能测试和集成测试
2. 性能优化和压力测试
3. 安全审计和渗透测试
4. GDPR 合规检查

### 回滚策略
- 数据库备份：每日全量备份 + 实时增量备份
- 代码版本控制：Git 分支管理
- 灰度发布：先在测试环境验证，再逐步上线
- 监控告警：实时监控关键指标，异常自动回滚

## Resolved Constraints Summary

### 规模目标
- 每天 <50 个注册
- 同时 5 个 AI 生成任务
- MVP 阶段无需水平扩展

### 关键技术决策
1. **实时协议**: STOMP/WebSocket (Spring原生)
2. **消息队列**: Kafka + 事务性Outbox
3. **审计存储**: MySQL分区 (MVP阶段)
4. **外发邮件**: 托管SMTP中继 (SendGrid/Mailgun)
5. **JWT策略**: 管理员可配置无上限，无主动吊销

### 注册流程约束
1. **注册码**: 验证即消费，一次性使用
2. **草稿持久化**: 24h临时Draft + Magic Link恢复
3. **状态机终态**: REJECTED, FAILED, COMPLETED + 允许重新申请
4. **学年选择**: 自动切换 (9月1日前后)

### PBT属性清单
| ID | 领域 | 不变量 |
|----|------|--------|
| PBT-01 | identity | 学号全局唯一+格式匹配 |
| PBT-02 | identity | 年龄/时间线一致性 |
| PBT-03 | identity | 姓名/国籍匹配 |
| PBT-04 | registration | 状态转换顺序性 |
| PBT-05 | registration | 限流阈值验证 |
| PBT-06 | registration | 注册码消费原子性 |
| PBT-07 | async | 任务幂等性 |
| PBT-08 | async | 进度单调性 |
| PBT-09 | async | 重试边界 |
| PBT-10 | audit | 日志不可变性 |
| PBT-11 | audit | 时间戳单调性 |
| PBT-12 | async | Kafka分区键一致性 |
| PBT-13 | async | SimpleBroker无持久化 |
| PBT-14 | auth | JWT无吊销验证 |
| PBT-15 | rate-limit | 限流降级一致性 |

## Additional Resolved Constraints (2026-01-30)

### Kafka 配置

**CONSTRAINT [KAFKA-PARTITION-KEY]**: 使用 `applicationId` 作为分区键。所有与同一申请相关的事件路由到同一分区，保证单申请事件有序性。

**CONSTRAINT [KAFKA-TOPIC-CONFIG]**: 主题配置默认值:
- partitions=6
- replication_factor=3
- retention.ms=604800000 (7天)

**PBT [PBT-12]**: Kafka 分区键一致性 - 同一 applicationId 的事件始终使用相同分区键，映射到单一分区。

### STOMP WebSocket 配置

**CONSTRAINT [STOMP-SIMPLE-BROKER]**: 使用 Spring 内置 SimpleBroker (内存模式):
- 无需外部消息中间件
- 适合 <50/天 的规模
- 认证通过 JWT Token 在 WebSocket 握手时验证
- 无持久化：断开连接后消息丢失，需依赖轮询降级

**PBT [PBT-13]**: SimpleBroker 无持久化 - 断开期间发送的消息不会在重连后送达，只有当前订阅者收到消息。

### JWT 策略

**CONSTRAINT [JWT-ADMIN-CONFIGURABLE]**: JWT TTL 由管理员配置，无强制上限。当前版本不实现主动吊销机制。token 仅依赖签名和 exp 验证。

**PBT [PBT-14]**: JWT 无吊销 - 未过期 token 始终有效，不受后续 TTL 配置变更或密码重置影响。

### Redis 降级策略

**CONSTRAINT [RATE-LIMIT-MYSQL-FALLBACK]**: Redis 不可用时降级到 MySQL `rate_limits` 表:
```sql
CREATE TABLE rate_limits (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  limit_key VARCHAR(255) NOT NULL,
  count INT NOT NULL DEFAULT 0,
  window_start DATETIME NOT NULL,
  window_seconds INT NOT NULL,
  UNIQUE KEY uk_key_window (limit_key, window_start)
);
```
- 定时任务每小时清理过期记录 (window_start + window_seconds < NOW())
- Redis 恢复后优先使用 Redis，MySQL 数据作为备份

**PBT [PBT-15]**: 限流降级一致性 - Redis/MySQL 切换不导致计数丢失或重复，决策与单源模型一致。

### Outbox 轮询策略

**CONSTRAINT [OUTBOX-POLLER-CONFIG]**: 事务性 Outbox 轮询配置:
- 轮询间隔: 1秒
- 批量大小: 500条
- 重试策略: 指数退避 1s-60s，最多10次
- 死信处理: 超过10次后移入 outbox_dead_letter 表，人工介入

### AI 生成超时

**CONSTRAINT [AI-GENERATION-TIMEOUT]**: AI 生成步骤超时配置:
- LLM 调用: 60秒
- 照片生成: 180秒
- 单任务总时长上限: 300秒
- 超时后标记为 FAILED，进入重试队列

### 外部调用重试

**CONSTRAINT [EXTERNAL-RETRY-POLICY]**: 外部服务调用重试策略:
- 幂等调用: 最多3次重试
- 退避策略: 指数退避 1s-8s + 随机抖动 (0-500ms)
- 非幂等调用: 不自动重试，记录日志人工处理

### 邮件退信处理

**CONSTRAINT [EMAIL-BOUNCE-HANDLING]**: 邮件退信处理策略:
- 硬退信 (5xx): 立即标记邮箱为 suppressed，不再发送
- 软退信 (4xx): 72小时内最多重试5次
- 投诉 (spam report): 立即标记为 suppressed
- 管理员可手动解除 suppressed 状态

### 数据保留策略

**CONSTRAINT [MAILDIR-RETENTION]**: 邮件存储保留策略:
- 在线保留: 30天
- 备份: 每日增量 + 每周全量
- 备份保留: 90天

**CONSTRAINT [AUDIT-PARTITION-AUTOMATION]**: 审计日志分区��动化:
- 分区粒度: 月
- 自动创建: 未来3个月分区
- 自动清理: 超过12个月的分区 (保留哈希摘要用于完整性验证)

**CONSTRAINT [REDIS-AOF-POLICY]**: Redis 持久化策略:
- appendfsync=everysec
- RDB 快照: 每6小时

## Open Questions

1. **GPU 资源**: 使用哪个云服务商的 GPU 实例？（AWS/GCP/Azure）
2. **CMS 选型**: 使用 Strapi 还是 Contentlayer 管理长内容？
3. **监控方案**: 使用哪个监控工具？（Prometheus + Grafana / Datadog）
4. **CI/CD**: 使用哪个 CI/CD 平台？（GitHub Actions / GitLab CI）
5. **域名注册**: minervia.edu.plli 域名如何注册？（.plli 是否为真实 TLD？）
6. **法律咨询**: 是否需要聘请法律顾问进行合规审查？
