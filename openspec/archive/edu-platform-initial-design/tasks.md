# Tasks: Minervia Institute 教育平台初始设计

## Phase 0: 项目脚手架 (Foundation)
- [x] 初始化前端项目 (Next.js 14 + TypeScript + Tailwind + shadcn/ui)
- [x] 初始化后端项目 (Kotlin + Spring Boot 3.x + Gradle)
- [x] 配置 MySQL 数据库连接和基础表结构
- [x] 配置 Redis 缓存连接
- [x] 搭建 Docker Compose 开发环境
- [x] 配置 CI/CD 基础流程 (lint/test/build)

## Phase 1: 核心实体和数据模型
- [x] 设计并实现学生(Student)实体和表结构
- [x] 设计并实现管理员(Admin)实体和表结构
- [x] 设计并实现注册申请(RegistrationApplication)实体和表结构
- [x] 设计并实现注册码(RegistrationCode)实体和表结构
- [x] 设计并实现审计日志(AuditLog)实体和表结构
- [x] 实现 JPA Repository 层
- [x] 编写 Flyway/Liquibase 数据库迁移脚本

## Phase 2: 认证和授权模块
- [x] 实现管理员登录 API (JWT)
- [x] 实现密码加密 (bcrypt cost=12)
- [x] 实现登录失败锁定 (5次/30分钟)
- [x] 实现 RBAC 权限控制 (超级管理员/普通管理员)
- [x] 实现 JWT 双 Token 机制 (Access 30min + Refresh 14d) [NEW]
- [x] 实现 Redis 吊销列表 (jti only) [NEW]
- [x] 实现吊销触发器 (登出/密码重置/角色变更) [NEW]
- [x] 实现 JWT Token 刷新机制
- [x] 实现管理员会话超时 (1小时无操作)
- [x] 实现 TOTP 两步验证 (可选)

## Phase 3: 注册流程模块 (registration-flow)
### 3.1 注册码管理
- [x] 实现注册码生成 API
- [x] 实现注册码批量生成 API
- [x] 实现注册码验证 API
- [x] 实现注册码撤销 API
- [x] 实现注册码列表查询 API
- [x] 实现注册码过期检测

### 3.2 邮箱验证
- [x] 实现发送验证码 API
- [x] 实现验证码校验 API
- [x] 实现验证码过期处理
- [x] 实现验证码发送限流 (5次/小时)

### 3.3 注册申请
- [x] 实现提交注册申请 API
- [x] 实现注册状态机 (未开始->注册码验证->邮箱验证->信息选择->待批准->生成中->已完成)
- [x] 实现 IP 限流 (3次/小时)
- [x] 实现邮箱限流 (1次/24小时)

### 3.4 Linux.do OAuth
- [x] 实现 Linux.do OAuth 回调处理
- [x] 实现 OAuth 用户关联逻辑

## Phase 4: 身份信息生成模块 (identity-generation)
### 4.1 规则引擎
- [x] 实现姓名生成器 (按国家/文化)
- [x] 实现出生日期生成器 (年龄约束)
- [x] 实现学号生成器 (格式: 年份+学院代码+序号)
- [x] 实现时间线生成器 (录取日期/入学日期)

### 4.2 约束校验
- [x] 实现年龄与学籍一致性校验
- [x] 实现时间线一致性校验
- [x] 实现专业与学院一致性校验
- [x] 实现学号唯一性校验

### 4.3 LLM 润色
- [x] 实现 LLM 调用服务 (家庭背景/兴趣爱好/学术目标)
- [x] 实现 LLM 失败降级 (预设模板)
- [x] 创建细粒度降级模板 (4国籍 x 4专业 x 2身份类型 = 32模板) [NEW]
- [x] 模板目录: config/llm-templates/{nationality}/{major}/{identity_type}.yaml [NEW]

### 4.4 学习时间线生成
- [x] 实现学期生成 (秋季/春季)
- [x] 实现课程列表生成 (按专业)
- [x] 实现成绩生成 (2.0-5.0 波兰评分)
- [x] 实现 GPA 计算

### 4.5 家庭信息生成
- [x] 实现父母姓名生成
- [x] 实现父母职业生成
- [x] 实现家庭地址生成

### 4.6 安全机制
- [x] 实现禁用姓名列表检查
- [x] 实现模拟数据标记 (is_simulated)
- [x] 实现生成种子和版本记录

### 4.7 批量和导出
- [x] 实现批量生成 API
- [x] 实现导出 JSON/CSV API

## Phase 5: 管理后台 API (admin-backend)
### 5.1 学生管理
- [x] 实现学生列表查询 API (分页/搜索/筛选)
- [x] 实现学生详情查询 API
- [x] 实现手动创建学生 API
- [x] 实现修改学生信息 API
- [x] 实现封禁学生 API
- [x] 实现解封学生 API

### 5.2 注册审批
- [x] 实现待审批列表查询 API
- [x] 实现审批详情查询 API
- [x] 实现批准注册 API
- [x] 实现拒绝注册 API
- [x] 实现批量批准 API

### 5.3 系统配置
- [x] 实现配置查询 API
- [x] 实现配置修改 API (邮件配额/注册码有效期/JWT过期时间等)

### 5.4 数据可视化
- [x] 实现学生统计 API
- [x] 实现邮件统计 API
- [x] 实现注册统计 API

## Phase 6: 审计日志模块 (audit-logging)
- [x] 实现审计日志记录服务 (异步队列)
- [x] 实现审计日志查询 API (时间/类型/操作人/对象)
- [x] 实现审计日志导出 API (CSV/JSON)
- [x] 实现审计日志完整性校验 (哈希)
- [x] 实现审计日志保留策略 (5年)
- [x] 实现 Elasticsearch 同步
- [x] 实现告警机制 (批量封禁/异常登录/配置修改)

## Phase 7: 异步任务系统
### 7.1 Kafka 配置 (CONSTRAINT: KAFKA-PARTITION-KEY, KAFKA-TOPIC-CONFIG)
- [x] 配置 Kafka broker 连接 (partitions=6, replication=3, retention=7天)
- [x] 实现 applicationId 分区键策略
- [x] 创建主题: registration-tasks, progress-events

### 7.2 Outbox 模式 (CONSTRAINT: OUTBOX-POLLER-CONFIG)
- [x] 创建 outbox 表和 outbox_dead_letter 表
- [x] 实现事务性 Outbox 插入 (批准事务内)
- [x] 实现轮询服务 (间隔1s, 批量500, 指数退避1s-60s)
- [x] 实现死信处理 (>10次移入 dead_letter)

### 7.3 STOMP WebSocket (CONSTRAINT: STOMP-SIMPLE-BROKER)
- [x] 配置 Spring SimpleBroker (内存模式)
- [x] 实现 WebSocket JWT 握手认证
- [x] 实现进度订阅端点 /topic/applications/{id}/progress
- [x] 实现状态轮询降级端点 GET /api/applications/{id}/status
- [x] 创建 task_progress 表 (独立持久化) [NEW]
- [x] 实现 progress 事件同步写入 DB [NEW]
- [x] 实现版本/时间戳检查防止过时更新 [NEW]

### 7.4 任务执行
- [x] 实现身份生成异步任务 (幂等: applicationId去重)
- [x] 实现任务状态管理 (PENDING/GENERATING_IDENTITY/GENERATING_PHOTOS/COMPLETED/FAILED)
- [x] 实现 AI 生成超时控制 (LLM=60s, 照片=180s, 总时长=300s)
- [x] 实现失败重试机制 (3次指数退避1s-8s+抖动)
- [x] 实现每步独立事务 (IDENTITY_RULES/IDENTITY_LLM/PHOTO_GENERATION) [NEW]
- [x] 实现超时清理 + 重排机制 [NEW]

## Phase 8: 前端 - 管理后台
- [x] 实现管理员登录页面
- [x] 实现仪表盘页面
- [x] 实现学生管理页面
- [x] 实现注册审批页面
- [x] 实现注册码管理页面
- [x] 实现审计日志页面
- [x] 实现系统配置页面

## Phase 9: 前端 - 注册流程
- [x] 实现注册码验证步骤
- [x] 实现邮箱验证步骤
- [x] 实现信息选择步骤 (专业/班级/身份类型/国家)
- [x] 实现注册进度追踪页面 (WebSocket)
- [x] 实现 Linux.do OAuth 登录按钮
- [x] 实现路由结构重构 app/[locale]/(marketing|portal|admin) [NEW]
- [x] 实现 localStorage 草稿优先 + 服务端备份 [NEW]
- [x] 实现 WS/轮询混合同步 + 版本检查 [NEW]

## Phase 10: 国际化和多语言
- [x] 配置 next-intl
- [x] 实现基于路由的国际化 (/en, /pl, /zh-CN 等)
- [x] 翻译管理后台文案
- [x] 翻译注册流程文案

## Phase 11: 测试和文档
### 11.1 后端测试
- [x] 编写后端单元测试 (JUnit 5)
- [x] 编写后端集成测试
- [x] 编写 API 文档 (OpenAPI/Swagger)
- [x] 编写部署文档

### 11.2 PBT 测试 (Property-Based Testing)
- [x] PBT-01: 学号唯一性和格式测试
- [x] PBT-02: 年龄/时间线一致性测试
- [x] PBT-03: 姓名/国籍匹配测试
- [x] PBT-04: 状态转换顺序性测试
- [x] PBT-05: 限流阈值边界测试
- [x] PBT-06: 注册码消费原子性测试 (并发声明)
- [x] PBT-07: 任务幂等性测试 (重复投递)
- [x] PBT-08: 进度单调性测试
- [x] PBT-09: 重试边界测试
- [x] PBT-10: 审计日志不可变性测试
- [x] PBT-11: 时间戳单调性测试
- [x] PBT-12: Kafka分区键一致性测试
- [x] PBT-13: SimpleBroker无持久化测试
- [x] PBT-14: JWT无吊销验证测试
- [x] PBT-15: 限流降级一致性测试
- [x] PBT-16: JWT Access Token 30min 边界测试 [NEW]
- [x] PBT-17: JWT Refresh Token 14d 边界测试 [NEW]
- [x] PBT-18: JWT 吊销列表隔离性测试 [NEW]
- [x] PBT-19: Progress 表独立性测试 [NEW]
- [x] PBT-20: 轮询间隔切换测试 [NEW]
- [x] PBT-21: AI 超时清理原子性测试 [NEW]
- [x] PBT-22: AI 步骤事务隔离测试 [NEW]
- [x] PBT-23: LLM 国籍模板覆盖测试 [NEW]
- [x] PBT-24: LLM 专业模板覆盖测试 [NEW]
- [x] PBT-25: LLM 身份类型隔离测试 [NEW]

### 11.3 前端测试
- [x] 编写前端组件测试 (Vitest)

## Phase 12: 限流降级模块 (CONSTRAINT: RATE-LIMIT-MYSQL-FALLBACK)
- [x] 创建 rate_limits 表 (limit_key, count, window_start, window_seconds)
- [x] 实现 RateLimitService 抽象接口
- [x] 实现 RedisRateLimitService (主实现)
- [x] 实现 MySQLRateLimitService (降级实现)
- [x] 实现健康检查和自动切换逻辑
- [x] 实现定时清理过期记录任务 (每小时)

## Phase 13: 邮件退信处理 (CONSTRAINT: EMAIL-BOUNCE-HANDLING)
- [x] 创建 email_suppression 表
- [x] 实现 SMTP Webhook 处理 (SendGrid/Mailgun bounce callback)
- [x] 实现硬退信处理 (立即 suppress)
- [x] 实现软退信重试逻辑 (72h/5次)
- [x] 实现投诉处理 (spam report -> suppress)
- [x] 管理员手动解除 suppress API
