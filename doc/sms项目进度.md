# SMS 项目 AI 进度与企业化建议

> 评估日期：2026-08-31  
> 评估范围：`sms-ai`、AI 相关公共模型、学生领域服务、数据库初始化脚本及前端聊天页面。  
> 技术基线：Java 21、Spring Boot 3.5.0、LangChain4j 1.18.1-beta28、Redis、Nacos、Dubbo、MyBatis-Plus。

## 一、结论摘要

项目已经搭出了“AI 服务独立部署 + LangChain4j + 规划器 + 业务工具抽象 + Redis”的骨架。当前 `chat` 任务已经可以通过 Redis 会话记忆调用流式模型并返回 SSE token；业务工具任务仍未完成，因此尚不能视为完整的 AI 对话/智能查询闭环。

建议先完成一条可验收的垂直链路：用户问题 → 规划 → 参数校验 → 学生查询工具 → 结果摘要 → 流式回答 → Redis 会话记忆。链路稳定后，再扩展知识库、更多领域工具和复杂 Agent。

## 二、当前架构与代码依据

| 层次 | 现状 | 主要依据 |
|---|---|---|
| AI 服务 | 独立 Spring Boot 应用，端口 9091，启用 Nacos/Dubbo | `sms-ai/src/main/java/com/xqy/sms/ai/AiApplication.java`、`sms-ai/src/main/resources/application.yml` |
| 模型接入 | LangChain4j OpenAI ChatModel 与 StreamingChatModel 自动配置；规划和聊天服务分别创建助手 | `sms-ai/pom.xml`、`AiPlanService`、`AiChatService` |
| 普通对话 | `AiChatAssistant.sampleChat` 和 `TokenStream chat` 接口已声明；`/sample-chat` 已调用同步模型 | `service/chat/assistant/AiChatAssistant.java`、`AiChatController.java` |
| 任务规划 | 通过提示词要求 JSON 数组，支持 domain/tool/query/missingArgs，解析后做少量修复 | `service/plan/AiPlanService.java` |
| 工具抽象 | 有 `AiToolExecutor`、`ExecutableAiTool`、`AiToolRegister`、数据库 Mapper/Service 和工具定义实体/DDL | `ai/model/*`、`ai/mapper/AiToolDefinitionMapper.java`、`ai/service/tool/AiToolDefinitionService.java` |
| 业务数据 | 学生服务仅提供增删改查和无条件 `listStudents()`，通过 Dubbo 暴露 | `sms-student-api`、`StudentServiceImpl.java` |
| 会话记忆 | Redis 的 LangChain4j `ChatMemoryStore` 已实现 JSON 读写 | `ai/store/RedisChatMemoryStore.java` |
| 前端 | 聊天页面和抽屉均使用本地 mock 消息，发送消息只追加到数组 | `sms-ui/src/views/template/chat/index.vue`、`components/core/layouts/art-chat-window/index.vue` |

## 三、已实现的 AI 能力

状态说明：**已具备**表示代码可被直接调用；**雏形**表示已有接口/模型但未形成可用闭环。

### 1. OpenAI 兼容模型接入（已具备，配置不可直接用于生产）

- `langchain4j-open-ai-spring-boot-starter` 和 `langchain4j-spring-boot-starter` 已加入 `sms-ai/pom.xml`。
- `AiPlanService` 使用 `AiServices.builder` 创建同步规划助手和带流式模型的聊天助手。
- `/api/ai/sample-chat?question=...` 已能走 `AiChatAssistant.sampleChat`，前提是模型地址、密钥和网络可用。
- 当前模型配置写在 `application.yml`，包含明文 API Key，且同步/流式模型的地址、密钥和模型名不一致；这只能作为开发占位配置。

### 2. 自然语言到任务 JSON 的规划（已具备，尚未执行）

- `planTasks(question, businessContext)` 会读取可用工具定义并调用 `AiPlanAssistant.plan`。
- 提示词明确规定 `domain`、`toolName`、`reason`、`QueryCriteria`、逻辑过滤树和允许的操作符。
- `parseTaskList` 支持去除代码块、截取数组、JSON 反序列化、domain 小写化、补齐缺失列表，以及单一 domain 工具的名称修复。
- 初始化数据脚本已提供 `student/query_student` 定义；应用运行时由 `AiToolDefinitionService` 从 `ai_tool_definition` 表读取定义。

### 3. 查询条件数据模型（已具备，缺少安全执行器）

- `QueryCriteria` 支持 `limit` 和递归 `QueryFilterNode`。
- 过滤节点支持 `and/or/not`、字段、操作符、单值和多值，理论上可转换为 MyBatis-Plus 查询条件。
- 当前仓库没有将该模型转换为 `QueryWrapper` 的实现，也没有字段白名单、limit 上限或类型转换。

### 4. Redis 聊天记忆存储（已接入聊天助手）

- `RedisChatMemoryStore` 使用 LangChain4j 官方序列化器保存消息，具备读、写、删能力。
- `AiChatService` 创建 `MessageWindowChatMemory`，通过 `@MemoryId` 按会话加载 Redis 消息并限制窗口为 40 条。

### 5. 工具注册抽象与数据库定义读取（框架已具备，执行器仍缺失）

- `AiToolRegister` 使用并发 Map 按 domain/toolName 注册、查询和执行工具，并可提供定义列表。
- `ExecutableAiTool` 将公开定义和执行器绑定，便于按领域扩展。
- `AiToolDefinitionMapper` 基于 MyBatis-Plus `BaseMapper` 查询 `ai_tool_definition`，`AiToolDefinitionService` 实现 `AiToolDefinitionProvider`，`AiPlanService` 注入该 Provider 后使用数据库返回的全部定义。
- 目前仍没有发现 `AiToolRegister` 的 Spring Bean、任何 `AiToolExecutor` 实现或把学生服务注册为工具的代码；数据库定义只是规划元数据，尚不能触发真实业务执行。

## 四、未完成或不可用的能力

### P0：业务工具端到端链路已完成首个场景

`chat` 任务已由 `AiChatService.streamChat` 调用 LangChain4j `TokenStream`，通过 `token/done/error` 事件回传，并使用 Redis 会话记忆。`student/query_student` 已完成首个业务执行链路：规划任务由 `taskExecutor` 并发执行，经 `StudentBusinessService` 调用 Dubbo 学生服务，再将统一结果交给 `AiChatService.answer` 流式总结。其他业务工具仍需按同一模式扩展。

需要补齐：任务编排器、工具执行、结果汇总提示词、流式 token 回传、完成/错误事件、客户端断开取消和幂等请求。

### P1：学生 AI 查询工具能力仍需扩展

已新增 `StudentBusinessService.queryStudent(QueryCriteria)`、学生 Dubbo `queryStudents` 接口和 MyBatis-Plus 查询实现；当前支持 `name LIKE/EQ`、`gender EQ`、`studentNo EQ` 和 limit，因此“查询姓张的学生”可转为姓名 LIKE 条件执行。`Student` 实体和 DDL 仍没有 `grade`、`className` 字段，且适配器暂不支持 OR/NOT 逻辑；若要兑现工具定义中的全部参数，需要先扩展学生数据模型和查询协议。

### P1：业务上下文治理仍不完整

Redis store 已接入 LangChain4j ChatMemory；业务上下文仍只按拼接 key 读取，未定义写入、过期、版本、租户隔离或并发更新策略。`BusinessContext` 模型目前没有调用方。

### P1：工具定义中心仍缺少管理能力

已补齐 Mapper 和只读 Service，但仍缺少管理接口、缓存刷新、启用状态过滤、版本和审核流程。当前 Provider 按需求读取表中全部记录，规划器尚未按 `enable` 过滤；生产环境应明确“全部读取”与“仅启用工具可见”的边界，并默认只暴露已审核启用定义。

### P1：前端未接入 AI API

模板聊天页和全局聊天抽屉中的 `sendMessage` 只向本地 `messages` 数组追加用户消息，没有 `axios` 请求、SSE 解析、流式渲染、重连、取消、错误提示或会话 ID 管理。当前页面展示的是演示数据，不是后端 AI 功能。

### P1：请求、权限和数据保护不足

`AiTaskRequest` 没有 Bean Validation；控制器没有认证/授权、租户信息或限流。`/api/ai/test1` 直接返回全部学生，存在敏感数据越权和大结果集风险。用户输入和 Redis 上下文直接拼接到提示词，尚无提示词注入防护和敏感信息脱敏。

### P1：配置与依赖存在运行风险

- `application.yml` 明文保存 API Key，应立即吊销并改为环境变量/密钥中心；提交历史也应检查泄露。
- 模型地址、密钥和模型名分散且流式配置缺少 `base-url`，多环境 profile 未建立。
- Spring Boot 3 使用 Jakarta 命名空间，销毁注解已统一为 `jakarta.annotation.PreDestroy`。
- SSE 超时应使用配置化的毫秒值（当前代码已调整为 `120_000L`，即 120 秒），避免模型响应尚未开始就断开。
- 使用无界 `LinkedBlockingQueue` 时线程池的 `MAX_POOL_SIZE=10` 实际不会在队列堆积时扩容，突发请求可能无限排队；应使用有界队列和拒绝策略。

### P2：测试、可观测性和运维能力不足

现有测试只有 `contextLoads`，没有规划 JSON、查询转换、工具注册、SSE、Redis 记忆、异常和权限测试。使用 JDK 21.0.12 执行 `./mvnw.cmd -pl sms-ai -am test -DskipTests` 已通过编译（本次未执行测试）。项目也没有 AI 请求日志规范、traceId、token/耗时/成本指标、模型重试与熔断、审计日志、数据保留策略或健康检查。

## 五、建议的企业级目标架构

```text
API Gateway/认证
       |
AiChatController（DTO 校验、租户/用户上下文、限流）
       |
ConversationOrchestrator
  |-- ChatMemory（Redis，TTL/版本/并发控制）
  |-- Planner（结构化输出 + Schema 校验）
  |-- Policy/Permission（工具、字段、行级权限）
  |-- ToolExecutor（学生、课程、统计等领域适配器）
  |-- AnswerComposer（引用工具结果，流式输出）
       |
SSE/WebSocket Gateway ---- 前端流式聊天组件
```

工具定义应是“元数据 + 代码执行器”的组合：数据库只保存可审计的描述、版本、启用状态和参数 Schema；实际执行器由代码注册，不能让模型直接生成 SQL 或调用任意 Bean。所有字段、操作符、排序、limit 和返回列都必须白名单化。

## 六、下一步实施路线

### 第 1 阶段：先打通最小闭环（P0，1 个迭代）

1. 修复 JDK/构建基线：统一 JDK 21、Maven Wrapper、CI 编译矩阵；已将 `javax.annotation` 改为 `jakarta.annotation`。
2. 扩展学生查询 API：新增分页查询 DTO 和 Dubbo 方法；明确补充 `grade/className` 字段，或从规划提示词/示例中删除不存在的字段。
3. 扩展学生查询适配器：补齐 `grade/className` 数据模型，完善字段/操作符/类型/limit 白名单，继续禁止原始 SQL。
4. 注册一个真正的 `AiToolRegister` Bean，并加载启用的工具定义；当前数据库读取 Service 已完成，下一步是启动时校验定义与执行器一一匹配。
5. 重写 `startChat` 编排：空问题校验 → 规划 → 缺参追问 → 权限检查 → 执行工具 → 结果摘要 → `AiChatAssistant.chat` 流式回答。
6. 修复 SSE 生命周期：事件协议固定为 `start/token/tool/error/done`，使用配置化超时，捕获异步异常，支持断开取消和队列拒绝。
7. 接入 `MessageWindowChatMemory` + `RedisChatMemoryStore`，按租户/用户/会话生成不可猜测的 key，设置 TTL 和最大消息窗口。

### 第 2 阶段：生产安全与可运营（P1，1~2 个迭代）

1. 使用 `@Validated`/`@Valid` 校验请求；从认证上下文获取 userId/tenantId，不信任客户端传入 userId。
2. API Key 放入 Nacos 加密配置、Vault/KMS 或环境变量，按 dev/test/prod 分 profile，立即轮换仓库中的现有密钥。
3. 增加工具级 RBAC、字段级脱敏、行级数据权限和审批；学生手机号/邮箱默认不返回给模型。
4. 为模型客户端配置连接/读取超时、有限重试、指数退避、熔断、降级回复和 token/cost 上限。
5. 增加 Micrometer 指标：请求量、成功率、首 token 延迟、总耗时、输入/输出 token、工具耗时、异常类型和队列深度；日志使用 traceId 且禁止记录原始隐私数据。
6. 前端接入真实 SSE，支持增量 Markdown、停止生成、重试、网络断线提示、消息持久化和会话切换。

### 第 3 阶段：扩展 AI 能力（P2，稳定性达标后）

- 增加课程/班级/考勤/成绩等只读工具，统一工具 Schema 和版本管理。
- 引入 RAG：文档解析、分块、embedding、向量库、权限过滤、引用来源和索引更新任务；不要把 RAG 与结构化查询混为一套执行器。
- 对高风险写操作使用显式确认、审批和幂等键；模型默认只读。
- 建立离线评测集（意图识别、工具选择、参数准确率、答案正确率、拒答率）和上线回归门禁。

## 七、建议的验收标准

- 给定“查询一年级 1 班姓王的女生”，规划结果只能选择已启用的学生查询工具，字段映射与数据表一致，非法字段/操作符被拒绝。
- 工具执行返回分页结果，手机号/邮箱等敏感字段按策略脱敏；无权限或缺少参数时不执行并给出追问。
- `/api/ai/chat` 在首 token 前发送 `start`，过程中发送有序 `token/tool`，成功发送一次 `done`，异常发送 `error` 并关闭；客户端断开后后台任务可取消。
- 同一租户同一会话的历史消息可从 Redis 恢复，具有 TTL、窗口上限和并发更新保护；不同租户 key 不可互相读取。
- 单元测试覆盖规划解析、Schema 校验、QueryWrapper 转换、工具注册冲突、权限/脱敏和 SSE 异常；集成测试覆盖 Redis、Dubbo stub 和模型 mock。
- CI 在干净环境使用 JDK 21 完成 `./mvnw verify`，前端完成类型检查和构建；不依赖本地 Nacos、MySQL、Redis 或真实模型密钥。

## 八、优先级清单

| 优先级 | 必须解决的问题 | 完成定义 |
|---|---|---|
| P0 | 工具执行编排、学生查询首个场景、JDK/注解兼容、SSE 超时 | “姓张的学生有哪些”可稳定端到端返回 |
| P1 | 密钥泄露、认证/租户/数据脱敏、工具定义中心、Redis memory 接线、前端真实接入 | 可在测试环境安全试用并可观测 |
| P2 | RAG、多领域工具、写操作审批、评测与成本治理 | 具备持续扩展和上线门禁 |

## 九、代码优化方向（按投入产出排序）

1. 将 `AiPlanService` 拆为 Planner、Orchestrator、ToolRegistry、MemoryService、SsePublisher，避免一个类同时负责模型装配、线程池、提示词、解析和传输。
2. 用 Java record + Jackson/JSON Schema 定义请求、规划结果和工具参数；替换基于字符串截取 JSON 数组的容错逻辑，失败时返回可诊断错误而不是静默空列表。
3. 为 `AiTask` 增加任务 ID、会话 ID、状态、耗时、错误码和重试次数；工具结果增加脱敏后的展示字段与来源信息。
4. 将线程池、SSE 超时、Redis TTL、上下文长度、最大工具调用次数和模型参数全部配置化，并在启动时做配置校验。
5. 使用构造器注入、统一异常处理（`@RestControllerAdvice`）和标准错误码；移除 `test1` 这类无权限的全量数据调试接口。
6. 统一命名（例如 `toolName`，去掉 `toolname`/拼写兼容别名），为公共 DTO 添加不可变性、边界校验和 OpenAPI 文档。
