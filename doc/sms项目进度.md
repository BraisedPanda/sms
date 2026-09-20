# SMS 项目进度与企业级 AI 能力评估

## 2026-09-17 认证与模块化进度

- 完成 `sms-system` API/Provider 拆分，Web 仅保留 BFF、JWT 过滤和用户上下文解析。
- 完成带行锁的会话轮换、退出、踢下线、Redis JTI 吊销与 MySQL 会话过期清理闭环。
- 完成 AI 的 API/Provider 边界和 Redis Stream SSE 网关；尚未完成真实 Redis/MySQL/Nacos 联调。
- 完成聊天、取消和知识入库的 Web BFF/Dubbo 边界，AI Provider 不再直接暴露业务 REST 或自行解析 JWT。
- 完成登录页简化、登录后路由恢复以及用户列表、动态菜单后端接入。
- 补充系统管理员、角色、菜单和按钮权限初始化数据；生产环境需在首次登录后立即更换初始化密码。

> 更新日期：2026-09-17
>
> 评估范围：后端 Maven 模块、`sms-ui`、环境配置、初始化 SQL、AI 会话编排、知识库和认证授权代码。
>
> 完成度口径：按企业级 AI 平台的功能覆盖度评估，不等同于生产就绪度。未经过真实依赖环境、并发、权限和故障恢复验证的能力不能视为可上线能力。

## 1. 项目目标

SMS 的目标是建设面向企业业务的 AI 平台，而非单次聊天接口。完整目标能力包括：

```text
身份、租户与权限
  -> 会话与上下文
  -> 模型路由、配额与成本控制
  -> 任务规划与受控工具调用
  -> 企业知识库与可追溯 RAG
  -> 流式交互、运营管理和人工干预
  -> 审计、评测、监控、告警与持续交付
```

当前项目处于“核心工程原型已成型、待完成企业化闭环”的阶段。后端主要模块可以定向编译，AI 会话、工具、RAG 和认证均已有基础实现；但关键业务边界、真实数据验证、运营界面、可靠性和交付治理仍未完成。

## 2. 总体进度

| 指标 | 当前评估 | 说明 |
|---|---:|---|
| 企业级 AI 功能覆盖度 | **43%** | 已有 AI 编排、工具、RAG 数据模型和 Web BFF/JWT/RBAC 鉴权，仍缺企业级闭环。 |
| 工程基础完成度 | **68%** | 多模块结构、分层、持久化、配置治理、共享认证能力和定向验证已具备。 |
| 生产就绪度 | **25%** | 已建立 AI 鉴权边界，仍缺真实依赖联调、数据隔离、可观测性、测试矩阵、部署与容灾。 |
| 本阶段目标 | **已完成** | 已完成 AI API/Provider 分层、会话链路优化、配置治理及 Web 统一鉴权。 |

### 企业级能力完成度

| 能力域 | 完成度 | 当前状态 |
|---|---:|---|
| 架构分层与配置治理 | **75%** | `sms-ai` 已拆分 API/Provider 并保留内部业务分层；`.env` 仅保留密钥、凭据和环境地址，稳定默认值位于各服务 YAML。 |
| 模型接入与会话编排 | **60%** | 支持模型定义、别名、OpenAI 兼容适配、Redis 记忆和 `PLAN -> TOOL -> COMPOSE`。 |
| 工具/Agent 治理 | **45%** | 已有工具注册、领域执行器、日志、重试和租约；缺审批、版本、策略和数据权限。 |
| 企业知识库与 RAG | **35%** | 已有元数据、分块、embedding、pgvector/Milvus 适配和入库接口；未完成真实验收与运营闭环。 |
| 认证、RBAC 与数据隔离 | **65%** | Web 解析 Auth0 JWT，并经 System 加载会话、角色和按钮权限；核心 run、知识库、工具与日志链路已实施租户/用户范围校验，权限运营界面和全域 ACL 尚未完成。 |
| 前端产品闭环 | **30%** | 登录续期、注销、用户列表和动态菜单已接入 `sms-web`；SSE 聊天、知识库和其他运营页面尚未接真实后端。 |
| 可靠性与可观测性 | **20%** | 有 run/step、日志和局部重试；没有 trace、指标、告警、Outbox 或完整恢复机制。 |
| 测试、交付与运维 | **30%** | 有核心安全契约定向测试、受影响模块编译和前端类型检查；没有完整测试矩阵、CI/CD、自动迁移治理和生产部署验收。 |

## 3. 已完成功能

### 3.1 AI 会话与任务编排

- `ConversationApplicationService` 已实现 `PLAN -> TOOL -> COMPOSE` 会话编排。
- 支持 `idempotencyKey`；重复请求复用 `AiTaskRun`，只有成功 claim 的请求启动工作流并记录请求日志。
- 已持久化 `ai_task_run`、`ai_task_step`、AI 请求日志和工具执行日志。
- 支持工具步骤超时、指数退避重试、租约超时回收和取消标记。
- SSE 支持 `start`、`planning`、`executing`、`generating`、`token`、`done`、`error` 事件。
- 工作流线程池、步骤超时、最大尝试次数、缓存 TTL、SSE 超时和聊天记忆窗口均配置化。

### 3.2 模型、工具与业务集成

- 模型定义可从数据库加载，支持别名、启用状态、回退别名和 OpenAI/DeepSeek 兼容适配。
- API Key 支持从 Spring `Environment` 读取，并回退到操作系统环境变量。
- 已有学生查询与知识检索两个领域工具方向。
- `AiToolRegistry` 校验工具定义与 Java 执行器的领域一致性，并记录工具调用结果。

### 3.3 知识库与向量数据

- 已有知识库、文档、版本、分块明细和入库任务的 MySQL 数据结构。
- 已实现 MySQL 分块读取、OpenAI 兼容 embedding 请求、PostgreSQL/pgvector upsert，以及 pgvector/Milvus 检索适配。
- 入库接口已支持按 `knowledgeBaseId`、`documentVersionId`、`limit` 和 `indexRevision` 过滤，不再覆盖请求参数。
- RAG 工具结果具备文档、分块、得分和元数据等来源追溯字段。

### 3.4 安全与系统权限

- `sms-web` 已接入 Spring Security、JWT、BCrypt 和 MyBatis-Plus。
- JWT 签发与校验已下沉至 `sms-common-core`，使用 Auth0 `java-jwt`，可统一传递用户、会话、租户、角色、权限与数据范围声明。
- 已实现登录、刷新令牌、JWT 过滤、无状态会话校验和用户状态校验；刷新令牌轮换时会重新加载角色和按钮权限。
- 已实现用户角色和按钮权限查询；角色授权点使用 `ROLE_<roleCode>`，按钮授权点使用 `auth_remark`。
- 已增加 `sys_user`、`sys_role`、`sys_menu`、`sys_button`、关联表及 `sys_user_session` 实体与 DDL。
- `sms-web` 是 AI 的唯一外部 HTTP 入口，在本地校验 JWT 后通过 System 校验 Redis/MySQL 会话与吊销状态，再将可信用户和会话标识经 Dubbo 传给 AI Provider。
- AI Provider 不再暴露业务 REST 或独立 JWT 过滤链；聊天、取消和知识入库均使用 `sms-ai-api` 契约。
- AI 任务查询、取消与幂等键复用已校验租户、用户及会话归属，禁止跨租户或跨用户操作任务。
- 前端登录与用户信息接口保留原契约，开发代理已转发到 `sms-web:9090`。

### 3.5 工程与配置

- `sms-ai` 已完成应用层、领域层、基础设施层和接口层分包。
- AI 请求日志和工具执行日志实体已迁入 `sms-common-persistent`。
- 共享实体审计字段已统一为 `createBy`、`modifyBy`、`createTime`、`updateTime`，数据库列统一为 `create_by`、`modify_by`、`create_time`、`update_time`；MySQL 与 PostgreSQL 均提供幂等迁移脚本。
- 根目录 `.env` 仅保留密钥、账号密码和部署环境地址；稳定参数默认值已回归各服务 YAML，并保留 `${ENV:default}` 覆盖形式。
- 环境变量已去除 `SMS_` 前缀；`.env.example` 对敏感凭据保持空白，对本地环境地址提供可替换样例。

## 4. 未完成功能与企业级差距

### P0：安全边界与身份上下文

| 差距 | 当前情况 | 企业级目标 |
|---|---|---|
| 租户隔离 | JWT、会话、run、知识库、向量检索、工具与日志已携带并校验 `tenantId`；其他业务域和知识库细粒度 ACL 尚未全覆盖。 | 将同一约束扩展到全部业务域，并按部署需要增加数据库 RLS 或等效纵深防御。 |
| 内部服务信任 | Web -> AI -> Knowledge 使用短时 HMAC 签名调用上下文和结构化审计日志，已提供网络访问矩阵。 | 在生产环境落实安全组/NetworkPolicy，并使用 mTLS 或服务网格管理服务身份与密钥轮换。 |
| RBAC 运营 | 已有登录和权限查询，没有用户/角色/菜单/按钮的管理 API 和前端。 | 支持授权分配、会话注销、权限变更即时生效和审计。 |
| 数据保护 | 工具输入、返回结果、Prompt/SSE 和相关日志已实施白名单、脱敏、长度限制与不可信数据分隔；分级、加密和留存删除策略尚未完成。 | 补齐数据分级、静态加密、密钥轮换、留存与删除机制。 |

### P0：RAG 可用性

| 差距 | 当前情况 | 企业级目标 |
|---|---|---|
| 向量维度 | pgvector DDL、Milvus/embedding 配置和运行时校验已统一为 `1536`；尚未在可用 PostgreSQL 与 embedding 服务上完成运行验证。 | 在目标环境执行迁移，并通过真实模型完成入库与检索维度验收。 |
| 真实模型验证 | 未证明当前 embedding 服务、模型名和维度可用。 | 完成真实文档入库、重复入库、召回、来源引用的端到端验收。 |
| 文档生命周期 | 只有分块和入库骨架，没有上传、解析、切分策略版本和异步任务运营。 | 支持文件上传、解析、切分、队列、失败重放、版本切换和删除。 |
| RAG 安全 | 检索已强制租户过滤、`ACTIVE` 状态和受控来源回传；知识库成员 ACL、文档有效版本策略仍未完成。 | 在检索、工具与回答侧执行统一 ACL、有效版本和来源引用策略。 |

### P1：AI/Agent 治理

- 缺少 JSON Schema 或 Bean Validation 级别的规划结果和工具参数契约校验。
- 缺少工具定义的审批、版本、灰度、禁用、缓存刷新和变更审计能力。
- 缺少模型路由策略、限流、熔断、供应商故障切换、配额和成本预算。
- 工具仅有局部重试与租约恢复，缺少跨实例 Worker、消息队列、Outbox 和端到端幂等保障。
- 流式取消是协作式取消，不能保证上游模型连接和外部工具已被主动中止。

### P1：前端与运营能力

- 登录续期、注销、动态菜单和用户列表已接后端；角色切换、按钮级页面控制及用户/角色/菜单写操作尚未闭环。
- 聊天 UI 尚未实现 SSE 事件解析、`runId` 保存、取消、重连、历史恢复和失败重试。
- 缺少知识库、文档、分块、入库任务、AI run/step、工具执行日志的运营管理页面。
- 缺少模型、工具、提示词模板和权限的配置运营界面。

### P1：可靠性、观测与交付

- 缺少统一 `traceId`、OpenTelemetry、Micrometer 指标、Prometheus/Grafana 仪表盘和告警。
- 缺少模型调用成本、延迟、成功率、RAG 命中率、工具错误率和队列积压指标。
- 缺少 run/step 状态机的显式迁移约束、乐观锁和完整崩溃恢复策略。
- 缺少数据库迁移版本管理、CI 质量门禁、容器化部署、健康检查、备份和灾备演练。

## 5. 待优化功能

| 优化项 | 现状 | 优化方向 | 优先级 |
|---|---|---|---|
| 会话缓存 | 业务上下文按用户与会话缓存。 | 增加版本、权限范围和显式失效策略，避免旧业务结果污染新会话。 | P1 |
| 状态机 | run/step 状态以字符串维护。 | 定义合法迁移表、乐观锁和状态事件审计。 | P1 |
| 令牌会话 | MySQL 保存 token SHA-256 摘要与 JTI，Redis 保存在线 JTI 和吊销键，Web 每次请求经 System 校验。 | 增加设备会话列表、批量下线、异常登录审计和 Redis 故障策略。 | P1 |
| 错误契约 | SSE 与 REST 错误格式不完全统一。 | 定义稳定错误码、可展示消息和 requestId/runId 关联。 | P1 |
| 配置校验 | 配置变量可绑定，但缺少统一启动校验。 | 使用 `@ConfigurationProperties` 和 Bean Validation 校验必填项、范围及组合关系。 | P1 |
| 知识入库 | 单进程同步处理。 | 改为异步任务，支持批量、限流、断点续传和失败重放。 | P1 |
| 工具结果 | 结果可进入模型上下文。 | 增加字段白名单、数据脱敏、最大返回量和 Prompt 注入防护。 | P0 |

## 6. 下一步建议

### 阶段 A：建立安全可用的最小闭环（P0）

1. **已完成**：run、知识库、日志和向量记录已增加租户范围；run 查询/取消校验租户与用户；内部 RPC 使用短时 HMAC 服务身份上下文并记录审计日志，网络隔离矩阵见 `doc/internal-rpc-security.md`。
2. **已完成**：MySQL 基础 DDL、租户迁移和系统初始数据已执行；Admin、超级管理员、菜单、按钮和关联均为 `ENABLED`，初始密码为 BCrypt 哈希。
3. **部分完成**：代码、pgvector DDL 和运行时校验已统一为 1536 维，入库按租户和文档版本幂等，SSE 回传受控来源字段；当前 PostgreSQL `localhost:5432` 未启动，配置的 embedding 上游返回 `model_not_found` 且模型列表没有 embedding 候选，真实入库/重复导入/召回验收仍阻塞。
4. **已完成（定向测试）**：聊天、取消、run 查询和知识入库增加认证上下文、参数边界、跨租户/跨用户拒绝及 `400/403/404` 错误契约测试。
5. **已完成**：工具执行强制租户/用户范围和参数上限；进入 Prompt/SSE/日志的工具结果使用字段白名单、脱敏、截断和不可信数据分隔。

### 阶段 B：完成产品和运营闭环（P1）

1. 完成前端按钮权限呈现、设备会话管理和用户/角色/菜单写操作。
2. 完成 SSE 聊天客户端，支持状态展示、取消、断线恢复和失败重试。
3. 实现用户/角色/菜单/按钮管理，知识库/文档/入库任务管理，以及 AI run/step/工具日志查询。
4. 将知识入库改为异步任务，增加文档上传、解析、切分策略和索引版本管理。
5. 建立模型、工具和提示词的版本、审批、灰度、限流和成本控制能力。

### 阶段 C：提升生产可靠性（P1/P2）

1. 接入 OpenTelemetry、Micrometer、Prometheus/Grafana 和集中日志，建立告警阈值。
2. 采用消息队列/Worker 与 Outbox 提升任务跨实例执行和崩溃恢复能力。
3. 建立数据库迁移、契约测试、模型 Mock、端到端测试、前端类型检查和安全扫描的 CI 质量门禁。
4. 完成容器化部署、健康检查、配置密钥管理、备份恢复和容量压测。
5. 建立离线评测集，持续跟踪规划准确率、工具选择、RAG 命中率、引用正确率、拒答率和成本。

## 7. 已验证内容

以下定向验证已通过：

```text
mvn -pl sms-common-core,sms-ai/sms-ai-provider,sms-web,sms-knowledge/sms-knowledge-provider -am "-Dtest=InternalCallSignerTest,JwtTokenServiceTest,AiSafetyPolicyTest,AiTaskRunServiceTest,AiControllerSecurityTest,ApiExceptionHandlerTest,KnowledgeServiceSecurityTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl sms-ai/sms-ai-provider,sms-web,sms-system/sms-system-provider,sms-knowledge/sms-knowledge-provider -am -DskipTests compile
mvn -pl sms-student/sms-student-provider,sms-ai/sms-ai-provider,sms-system/sms-system-provider,sms-knowledge/sms-knowledge-provider -am -DskipTests compile
pnpm run build
```

15 个定向测试已通过，覆盖共享 JWT、内部 RPC 签名、run 归属、工具边界、Web 命令边界与错误契约、知识检索租户签名；受影响模块编译通过。MySQL 8.0.43 的 `sms_dev` 已执行基础 DDL、租户迁移和 RBAC 初始化，迁移重复执行通过；10 张目标表均有 `tenant_id`，Admin 使用 BCrypt 且状态为 `ENABLED`。此前前端生产构建已通过，本轮未重复执行。

尚未通过的外部集成：PostgreSQL/pgvector 连接被拒绝；embedding 上游对 `text-embedding-3-small` 返回 `model_not_found`，且 `/models` 未返回 embedding 候选。因此真实文档入库、重复导入、召回质量和来源端到端验收仍需在可用模型与 PostgreSQL 服务就绪后执行。Redis、Nacos、Milvus 和 HTTP/SSE 端到端测试本轮未执行。

审计列改名后的 Student、AI、System、Knowledge 及直接依赖模块编译通过。迁移脚本已生成但未对现有 MySQL/PostgreSQL 实例执行，部署新版服务前需先执行对应数据库脚本。

## 8. 阶段验收标准

- 用户可通过 `sms-ui` 登录 `sms-web`，令牌刷新、注销和权限变更按预期生效。
- AI 请求只能从认证上下文获取用户和会话，且无法跨用户取消或复用 run；完成租户模型和数据范围过滤后，才可验证跨租户隔离。
- 文档可以完成可重复的向量入库，检索返回正确的来源、分块、得分和权限范围。
- SSE 客户端可以展示规划、工具执行、流式 token、完成和错误，并可以取消任务。
- 核心路径具备回归测试、监控指标和失败告警；部署过程可重复并有数据恢复预案。
