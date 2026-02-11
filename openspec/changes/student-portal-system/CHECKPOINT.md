# Context Checkpoint: 学生门户系统完整实现

> CCG:SPEC:PLAN 流程完成检查点
> 生成时间：2026-02-10

## 执行摘要

本次规划通过 **CCG:SPEC:PLAN** 多模型协作流程，完成了"学生门户系统完整实现"的详细设计与任务分解。

### 核心成果

1. **设计文档** (`design.md`)
   - 6 个架构决策记录（ADR）
   - 完整的数据模型设计
   - API 契约定义
   - 安全与配置方案

2. **技术规范** (`specs/api-contracts.md`)
   - 统一错误码体系
   - 详细的请求/响应格式
   - 验证规则与错误处理

3. **任务清单** (`tasks.md`)
   - 29 个可执行任务
   - 5 个实施阶段
   - 明确的依赖关系与验收标准

---

## 实施范围确认

### 包含功能

✅ **管理员前端**（8 个页面）
- 仪表板、申请审批、学生管理、注册码管理
- 管理员管理、审计日志、系统配置

✅ **文档存储系统**
- Cloudflare R2 集成
- 预签名 URL 直传
- 完整的上传/下载/删除流程

✅ **邮件重试机制**
- Kafka 异步队列
- 指数退避重试（最多 3 次）
- 幂等性保证

✅ **审计员角色**
- AUDITOR 枚举值
- 只读权限矩阵
- 前端权限门控

---

## 关键技术决策

### ADR-001: 文档存储 - Cloudflare R2
**选择理由**: 减少后端带宽，S3 兼容 API
**权衡**: 需要两阶段 finalize，URL 泄漏风险
**配置**: Upload TTL 15min, Download TTL 1h, 最大 10MB

### ADR-002: 邮件重试 - DB + Kafka
**选择理由**: 精确控制 backoff/幂等/可观测
**权衡**: 需要新增 email_deliveries 表
**配置**: 指数退避 1s→2s→4s→8s，最多 3 次

### ADR-003: 权限模型 - 枚举 + @PreAuthorize
**选择理由**: 快速落地，改动小
**权衡**: 扩展性受限
**约束**: AUDITOR 仅读 audit-logs + statistics

---

## 数据模型变更

### 新增表
1. **student_documents** (9 列)
   - 主键: id (BIGINT AUTO_INCREMENT)
   - 唯一键: object_key
   - 外键: student_id → students(id) ON DELETE RESTRICT
   - 索引: (student_id, created_at), (student_id, status)

2. **email_deliveries** (12 列)
   - 主键: id (BIGINT AUTO_INCREMENT)
   - 唯一键: dedupe_key
   - 索引: (status, next_attempt_at), (recipient_email, created_at)

### 修改表
- **admins.role**: 扩展 ENUM 增加 'AUDITOR'

---

## 风险与缓解

### 高风险项
1. **R2 不可用** → 降级策略：返回 503，前端隐藏上传入口
2. **Kafka 不可用** → 邮件写入 PENDING 状态，恢复后补投递
3. **AUDITOR 越权** → 网关级 + 方法级双重权限检查

### 中风险项
1. **预签名 URL 泄漏** → 短 TTL (15min/1h) + UUID 对象键
2. **邮件重试耗尽** → DLT 队列 + 告警通知
3. **文档上传未完成** → 定时清理 PENDING_UPLOAD (24h)

---

## 实施计划

### Phase 1: 基础设施 (2-3 天)
- 数据库迁移 (3 个脚本)
- R2 配置集成
- Kafka topic 创建

### Phase 2: 后端核心 (5-7 天)
- 实体 + Repository (4 个)
- 核心服务 (DocumentService, EmailDeliveryService, R2StorageService)
- Kafka Consumer

### Phase 3: 后端 API (2-3 天)
- StudentDocumentController (5 个端点)
- AdminStatisticsController (3 个端点)

### Phase 4: 前端实现 (7-10 天)
- 管理员 8 个页面
- 学生文档上传/列表页面
- 权限门控组件

### Phase 5: 测试验证 (3-5 天)
- 单元测试 (5 个测试类)
- 集成测试 (2 个测试类)
- 前端组件测试

**总计：19-28 天**

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

## 待批准事项

### 需要确认的配置参数
1. R2 Upload URL TTL: 15 分钟（是否需要调整？）
2. 邮件最大重试次数: 3 次（是否需要调整？）
3. 文档最大大小: 10MB（是否需要调整？）

### 需要确认的实施优先级
1. 是否先实施后端再实施前端？
2. 是否需要分批发布（如先发布文档系统，再发布邮件重试）？

---

## 下一步行动

**选项 A: 批准并开始实施**
- 执行命令: `/ccg:spec-impl`
- 将按 tasks.md 顺序执行 29 个任务

**选项 B: 调整设计**
- 指出需要修改的部分
- 我将更新相关制品

**选项 C: 补充规范**
- 指出需要补充的规范细节
- 我将扩展 specs/ 目录

---

