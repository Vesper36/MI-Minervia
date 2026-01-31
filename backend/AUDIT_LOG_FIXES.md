# 审计日志模块性能和正确性修复

## 修复日期
2026-01-30

## 修复的问题

### Critical 级别

#### 1. 哈希计算不一致 (FIXED)
**问题**: `AuditLogService`, `AuditIntegrityService`, `AuditLogQueryService` 中的哈希计算逻辑不同，导致完整性验证失败。

**修复**:
- 创建 `AuditHashCalculator` 组件统一哈希计算逻辑
- 所有服务注入并使用共享的哈希计算器
- 文件: `backend/src/main/kotlin/edu/minervia/platform/service/audit/AuditHashCalculator.kt`

#### 2. 时间戳精度问题 (FIXED)
**问题**: `createdAt` 使用 `Instant.now()` 包含纳秒，但数据库 TIMESTAMP 列只保存到秒，导致哈希验证失败。

**修复**:
- 在 `AuditHashCalculator` 中使用 `normalizeTimestamp()` 方法
- 只使用 `epochSecond` 进行哈希计算，忽略纳秒部分
- 确保创建和验证使用相同的时间戳精度

#### 3. 批量封禁阈值错误 (FIXED)
**问题**: 使用 `>= 10` 而不是 `> 10`，在恰好 10 次时触发告警。

**修复**:
- 修改 `AuditLogRepository.findBulkBanActors` 的 HAVING 子句
- 从 `having count(a) >= :threshold` 改为 `having count(a) > :threshold`
- 文件: `backend/src/main/kotlin/edu/minervia/platform/domain/repository/AuditLogRepository.kt:63`

### High 级别

#### 4. 空哈希值处理 (FIXED)
**问题**: 空哈希值被视为有效，无法检测到哈希生成失败或篡改。

**修复**:
- 在 `AuditIntegrityService` 和 `AuditLogQueryService` 中
- 将 `hashValue == null` 的日志标记为无效
- 记录警告日志以便追踪
- 文件:
  - `backend/src/main/kotlin/edu/minervia/platform/service/audit/AuditIntegrityService.kt:113-116`
  - `backend/src/main/kotlin/edu/minervia/platform/service/audit/AuditLogQueryService.kt:145-147`

### Medium 级别

#### 5. 异常登录检测性能优化 (FIXED)
**问题**: `checkAnomalousLogins` 加载完整实体列表到内存进行分组。

**修复**:
- 添加 `findAnomalousLoginActors` 查询方法进行数据库级聚合
- 使用 `count(distinct a.ipAddress)` 在数据库层面计算唯一 IP 数
- 只在需要时加载具体 IP 地址列表
- 文件:
  - `backend/src/main/kotlin/edu/minervia/platform/domain/repository/AuditLogRepository.kt:73-85`
  - `backend/src/main/kotlin/edu/minervia/platform/service/audit/AuditAlertService.kt:56-76`

## 已有的优化（由 Codex 完成）

### 1. 统计聚合优化
- `getStatsSummary` 使用 JPQL 投影查询
- 不再加载完整实体列表到内存
- 文件: `backend/src/main/kotlin/edu/minervia/platform/service/audit/AuditLogQueryService.kt:91-117`

### 2. 定时完整性检查优化
- `scheduledIntegrityCheck` 使用分页查询
- 批量大小: 500 条/页
- 文件: `backend/src/main/kotlin/edu/minervia/platform/service/audit/AuditIntegrityService.kt:72-93`

### 3. 批量封禁检测优化
- `checkBulkBanOperations` 使用数据库级聚合
- 不再加载完整实体列表
- 文件: `backend/src/main/kotlin/edu/minervia/platform/service/audit/AuditAlertService.kt:36-49`

### 4. 导出功能优化
- `exportLogs` 使用流式写入
- 不再将完整数据集加载到内存
- 批量大小: 1000 条/批
- 文件:
  - `backend/src/main/kotlin/edu/minervia/platform/service/audit/AuditLogQueryService.kt:75-89`
  - `backend/src/main/kotlin/edu/minervia/platform/web/controller/AuditLogController.kt:75-125`

### 5. 异步日志记录优化
- 移除 `CompletableFuture.supplyAsync` 的双重包装
- `@Async` 注解已经确保异步执行
- 文件: `backend/src/main/kotlin/edu/minervia/platform/service/audit/AuditLogService.kt:68-70`

## 验证清单

### 编译验证
- [ ] `./gradlew compileKotlin` 成功
- [ ] 无编译错误或警告

### 单元测试
- [ ] 测试哈希计算一致性
- [ ] 测试时间戳精度处理
- [ ] 测试空哈希值检测
- [ ] 测试阈值边界条件

### 集成测试
- [ ] 测试审计日志创建和验证
- [ ] 测试批量封禁告警（10次不触发，11次触发）
- [ ] 测试异常登录告警
- [ ] 测试导出功能（大数据集）

### 性能测试
- [ ] 测试 `getStatsSummary` 在大数据集下的内存使用
- [ ] 测试 `scheduledIntegrityCheck` 的执行时间
- [ ] 测试 `exportLogs` 的内存使用和响应时间

## 相关文件

### 新增文件
- `backend/src/main/kotlin/edu/minervia/platform/service/audit/AuditHashCalculator.kt`

### 修改文件
- `backend/src/main/kotlin/edu/minervia/platform/domain/repository/AuditLogRepository.kt`
- `backend/src/main/kotlin/edu/minervia/platform/service/audit/AuditLogService.kt`
- `backend/src/main/kotlin/edu/minervia/platform/service/audit/AuditIntegrityService.kt`
- `backend/src/main/kotlin/edu/minervia/platform/service/audit/AuditLogQueryService.kt`
- `backend/src/main/kotlin/edu/minervia/platform/service/audit/AuditAlertService.kt`

## 注意事项

1. **数据库迁移**: 如果之前已有审计日志数据，旧的哈希值可能无法验证通过（因为时间戳精度问题）。建议：
   - 在生产环境部署前清空审计日志表，或
   - 重新计算所有现有日志的哈希值

2. **告警阈值**: 批量封禁告警现在在 >10 次时触发（之前是 >=10）。确认这符合业务需求。

3. **空哈希值**: 现在空哈希值会被标记为无效。确保所有新日志都正确生成哈希值。

## 下一步

1. 运行完整的测试套件
2. 在测试环境验证修复
3. 更新相关文档
4. 部署到生产环境
