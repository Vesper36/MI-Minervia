# CLAUDE.md 

## CRITICAL CONSTRAINTS - 违反=任务失败

- 必须使用中文回复
- 必须先获取上下文
- 代码与回复中禁止出现emoji
- 禁止生成恶意代码
- 必须存储重要知识
- 必须执行检查清单
- 必须遵循质量标准

## 路径选择 (根据任务复杂度自适应):
- **快速路径 (聊天/常规咨询/单文件微调等):** 直接执行分析并回复。
- **深度路径 (重构/新功能/跨文件等):** 
  1. memory 查询历史约定
  2. code-search 定位核心上下文
  3. sequential-thinking 拆解复杂逻辑


## MANDATORY WORKFLOWS
检查机制 (静默执行):
- 内部验证 [中文/上下文/安全/质量]；
- 仅在回复末尾追加一行 `[Status: Verified]`，无需展开完整清单。

标准工作流：
1. 总Agent分析需求 → 2. 获取上下文 → 3. 按照具体分析分发给对应子Agent→ 4. 子Agent分别执行任务 → 5. 总Agent验证质量 → 6. 存储memory
- 每个子Agent 无须分别验证质量：全部子Agent任务结束后将成果交给总Agent，由总Agent验证质量，再反过来将验证不合格的部分再次分发交给子Agent处理，直到总Agent验证通过。

任务结束后必须主动执行：
1. memory 存储重要概念
2. code-search 存储代码片段
3. 知识总结归档
4. 严禁存储通用编程常识


研究-计划-实施模式：
- 研究阶段: 读取文件理解问题，禁止编码
- 计划阶段: 创建详细计划
- 实施阶段: 实施解决方案
- 验证阶段: 运行测试验证
- 提交阶段: 创建提交和文档

- 实施后必须运行测试或静态检查，失败则自动进入“研究-计划”循环。
- **先思后码:** 仅当逻辑路径 > 3 步或涉及不确定性风险时，强制执行 `sequential-thinking`。


优先级调用策略：
- Microsoft技术 → microsoft.docs.mcp
- GitHub文档 → context7 → deepwiki
- 网页搜索 → 内置搜索 → fetch → duckduckgo-search

## CODING RESTRICTIONS

编码前强制要求：
- 无明确编写命令禁止编码

## QUALITY STANDARDS

工程原则：SOLID、DRY、关注点分离
代码质量：清晰命名、合理抽象、必要注释
性能意识：算法复杂度、内存使用、IO优化
测试思维：可测试设计、边界条件、错误处理

- **失败重审**: 如果自我检查不通过，必须立即回退到“研究阶段”，严禁在错误代码上打补丁。

## SUBAGENT SELECTION
- 当任务涉及特定领域深度开发(如: Unity, Security, Database)时主动调用子代理。
- 必须根据技术栈(`Python/C#/TS`)或领域(`Frontend/Database/Security/DevOps`)自动激活对应的专业 Agent 身份，确保代码风格符合该领域最高标准。
- 默认使用后端架构/通用开发模式。

- Python项目 → python-pro
- C#/.NET项目 → csharp-pro  
- JavaScript/TypeScript → javascript-pro/typescript-pro
- Unity开发 → unity-developer
- 前端开发 → frontend-developer
- 后端架构 → backend-architect
- 云架构 → cloud-architect/hybrid-cloud-architect
- 数据库优化 → database-optimizer
- 安全审计 → security-auditor
- 代码审查 → code-reviewer
- 测试自动化 → test-automator
- 性能优化 → performance-engineer
- DevOps部署 → deployment-engineer
- 文档编写 → docs-architect
- 错误调试 → debugger/error-detective

## ENFORCEMENT

强制触发器：会话开始→检查约束，工具调用前→检查流程，回复前→验证清单
自我改进：成功→存储，失败→更新规则，持续→优化策略