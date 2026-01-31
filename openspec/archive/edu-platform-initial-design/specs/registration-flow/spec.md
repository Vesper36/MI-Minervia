# Specification: Registration Flow

## ADDED Requirements

### Requirement: 注册码验证
系统必须要求用户在注册时提供有效的注册码。注册码由管理员生成和管理，每个注册码只能使用一次。

**CONSTRAINT [REG-CODE-CONSUME]**: 注册码采用"验证即消费"策略。验证成功时立即标记为已用，不可再被其他申请使用。申请失败/被拒后注册码不回收（一次性使用）。

**PBT [PBT-06] 注册码消费原子性**:
- INVARIANT: 一个注册码最多只能被一个申请成功声明；一旦使用/撤销/过期则不可再用
- FALSIFICATION: 并发声明同一注册码，断言最多一个成功；状态变更后尝试声明
- BOUNDARY: 同毫秒并发声明、声明期间过期、事务回滚后的状态

#### Scenario: 有效注册码验证成功
- **WHEN** 用户输入有效且未使用的注册码
- **THEN** 系统验证通过，允许用户继续注册流程

#### Scenario: 无效注册码被拒绝
- **WHEN** 用户输入不存在的注册码
- **THEN** 系统显示错误消息"注册码无效"，阻止继续注册

#### Scenario: 已使用注册码被拒绝
- **WHEN** 用户输入已被其他用户使用的注册码
- **THEN** 系统显示错误消息"注册码已被使用"，阻止继续注册

#### Scenario: 过期注册码被拒绝
- **WHEN** 用户输入已过期的注册码
- **THEN** 系统显示错误消息"注册码已过期"，阻止继续注册

### Requirement: 外部邮箱验证
系统必须要求用户使用外部邮箱（非 EDU 邮箱）进行注册，并通过验证码验证邮箱所有权。

**CONSTRAINT [EMAIL-VERIFY-SINGLE]**: 每个邮箱同时只允许一个有效验证码。重新发送时旧验证码立即失效，重试计数器原子重置。验证码存储为哈希值。

#### Scenario: 发送验证码到外部邮箱
- **WHEN** 用户输入外部邮箱地址并点击"发送验证码"
- **THEN** 系统发送 6 位数字验证码到该邮箱，验证码有效期 15 分钟

#### Scenario: 验证码验证成功
- **WHEN** 用户在 15 分钟内输入正确的验证码
- **THEN** 系统验证通过，标记邮箱为已验证

#### Scenario: 验证码验证失败
- **WHEN** 用户输入错误的验证码
- **THEN** 系统显示错误消息"验证码错误"，允许重试（最多 5 次）

#### Scenario: 验证码过期
- **WHEN** 用户在 15 分钟后输入验证码
- **THEN** 系统显示错误消息"验证码已过期"，允许重新发送

#### Scenario: 重新发送验证码
- **WHEN** 用户点击"重新发送验证码"
- **THEN** 系统发送新的验证码，旧验证码失效

### Requirement: 学生信息选择
系统必须允许用户自行选择专业、班级、身份类型（本地人/国际生）和具体国家。

#### Scenario: 选择专业和班级
- **WHEN** 用户从下拉列表中选择专业和班级
- **THEN** 系统记录用户选择，用于后续身份信息生成

#### Scenario: 选择本地人身份
- **WHEN** 用户选择"本地人"身份类型
- **THEN** 系统自动设置国家为"波兰"，生成符合波兰文化的身份信息

#### Scenario: 选择国际生身份
- **WHEN** 用户选择"国际生"身份类型
- **THEN** 系统显示国家选择列表，用户必须选择具体国家

#### Scenario: 国际生选择国家
- **WHEN** 国际生用户从列表中选择国家（如中国、美国、德国等）
- **THEN** 系统记录国家选择，用于生成符合该国文化的身份信息

### Requirement: 管理员批准
系统必须要求管理员手动批准所有学生注册申请。未经批准的申请不能激活账户。

#### Scenario: 提交注册申请
- **WHEN** 用户完成所有注册步骤并提交申请
- **THEN** 系统创建待审批的注册申请，状态为"待批准"

#### Scenario: 管理员批准申请
- **WHEN** 管理员审查申请并点击"批准"
- **THEN** 系统触发身份信息生成流程，创建学生账户

#### Scenario: 管理员拒绝申请
- **WHEN** 管理员审查申请并点击"拒绝"，提供拒绝原因
- **THEN** 系统标记申请为"已拒绝"，发送拒绝通知邮件到用户外部邮箱

#### Scenario: 批准后自动生成账户
- **WHEN** 管理员批准申请
- **THEN** 系统自动生成学号、EDU 邮箱、身份信息和照片

### Requirement: 异步身份生成
系统必须在管理员批准后异步生成学生身份信息和照片，并通过实时进度更新通知用户。

#### Scenario: 启动异步生成任务
- **WHEN** 管理员批准注册申请
- **THEN** 系统创建异步任务，状态为"生成中"

#### Scenario: 实时进度更新
- **WHEN** 异步任务执行过程中
- **THEN** 系统通过 **STOMP/WebSocket** 向用户推送进度更新（生成身份信息 -> 生成照片 -> 完成）
- **NOTE**: 前端使用 `@stomp/stompjs` 客户端库

**CONSTRAINT [REALTIME-TARGET]**: 实时进度同时推送到:
1. 管理后台审批页面（管理员监控）
2. 申请人状态页面（通过邮件中的magic link访问）

**CONSTRAINT [RECONNECT-FALLBACK]**: WebSocket断开时前端自动轮询 `/api/applications/{id}/status` 端点（间隔5秒），重连后发送当前状态快照。

**PBT [PBT-08] 进度单调性**:
- INVARIANT: 进度更新按 PENDING -> GENERATING_IDENTITY -> GENERATING_PHOTOS -> COMPLETED 顺序，不可回退
- FALSIFICATION: 乱序/重放进度事件，断言状态机拒绝回退
- BOUNDARY: 重复进度事件、COMPLETED后的延迟事件、任务重启后的状态恢复

#### Scenario: 生成成功通知
- **WHEN** 异步任务完成
- **THEN** 系统发送欢迎邮件到用户外部邮箱，包含 EDU 邮箱地址和登录链接

#### Scenario: 生成失败处理
- **WHEN** 异步任务失败（AI 生成超时或错误）
- **THEN** 系统标记任务为"失败"，通知管理员手动处理

### Requirement: Linux.do 社区登录集成
系统必须支持通过 Linux.do 社区账号进行注册，但仍需要注册码验证和管理员批准。

#### Scenario: 通过 Linux.do 登录
- **WHEN** 用户点击"使用 Linux.do 登录"
- **THEN** 系统重定向到 Linux.do OAuth 授权页面

#### Scenario: OAuth 授权成功
- **WHEN** 用户在 Linux.do 授权成功并返回
- **THEN** 系统获取用户基本信息（用户名、邮箱），自动填充注册表单

#### Scenario: OAuth 用户仍需注册码
- **WHEN** OAuth 授权成功的用户尝试注册
- **THEN** 系统仍要求输入注册码，验证流程与普通注册相同

#### Scenario: OAuth 用户关联已有账户
- **WHEN** OAuth 授权成功的用户已有校内账户
- **THEN** 系统自动登录到已有账户，跳过注册流程

### Requirement: 注册流程状态机
系统必须维护注册流程的状态机，确保每个步骤按顺序执行。

**CONSTRAINT [FSM-TERMINAL-STATES]**: 状态机包含以下终态:
- REJECTED: 管理员拒绝申请
- FAILED: 身份生成失败3次后
- COMPLETED: 成功完成

**CONSTRAINT [FSM-REAPPLY]**: 终态(REJECTED/FAILED/COMPLETED)的用户可用同一邮箱提交新申请，但需要新的注册码。

**PBT [PBT-04] 状态转换顺序性**:
- INVARIANT: 状态严格按 UNSTARTED -> CODE_VERIFIED -> EMAIL_VERIFIED -> INFO_SELECTED -> PENDING_APPROVAL -> GENERATING -> COMPLETED (或终态REJECTED/FAILED) 顺序转换，不可跳跃或回退
- FALSIFICATION: 生成随机API调用序列，断言非法转换被拒绝
- BOUNDARY: 浏览器关闭后恢复、重复执行同一步骤、终态处理

#### Scenario: 状态转换顺序
- **WHEN** 用户开始注册
- **THEN** 系统按以下顺序转换状态：未开始 → 注册码验证 → 邮箱验证 → 信息选择 → 待批准 → 生成中 → 已完成

#### Scenario: 阻止跳过步骤
- **WHEN** 用户尝试跳过某个步骤（如未验证邮箱就提交申请）
- **THEN** 系统阻止操作，显示错误消息"请完成所有必需步骤"

#### Scenario: 状态持久化
- **WHEN** 用户在注册过程中关��浏览器
- **THEN** 系统保存当前状态，用户重新打开时可以从上次位置继续

**CONSTRAINT [DRAFT-PERSISTENCE]**: 注册草稿通过临时Application Draft ID持久化（服务端存储）:
- 草稿有效期: 24小时
- 恢复方式: 通过邮件发送的magic link访问
- 草稿包含: 当前状态、已验证字段、用户选择
- 安全: Draft ID为UUID v4，不可枚举

### Requirement: 注册限流和防滥用
系统必须实现注册限流机制，防止恶意批量注册。

**CONSTRAINT [RATE-LIMIT-REDIS]**: 限流计数器存储在Redis（开启AOF持久化）。Redis不可用时降级到MySQL计数器。

**PBT [PBT-05] 限流阈值验证**:
- INVARIANT: IP <= 3次/小时, 邮箱 <= 1次/24小时, 验证码 <= 5次/小时；超限拒绝，未超限接受
- FALSIFICATION: 生成窗口边界附近的请求流，断言接受/拒绝与阈值匹配
- BOUNDARY: 恰好达到限制vs限制+1、跨窗口重置边界、同IP/邮箱并行请求

#### Scenario: IP 地址限流
- **WHEN** 同一 IP 地址在 1 小时内尝试注册超过 3 次
- **THEN** 系统阻止该 IP 继续注册，显示错误消息"请求过于频繁，请稍后再试"

#### Scenario: 邮箱地址限流
- **WHEN** 同一邮箱地址在 24 小时内尝试注册超过 1 次
- **THEN** 系统阻止该邮箱继续注册，显示错误消息"该邮箱已提交注册申请"

#### Scenario: 验证码发送限流
- **WHEN** 同一邮箱在 1 小时内请求验证码超过 5 次
- **THEN** 系统阻止发送验证码，显示错误消息"验证码请求过于频繁"
