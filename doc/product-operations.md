# 产品与运营功能使用说明

更新时间：2026-10-09。本版覆盖系统管理、可恢复聊天、知识管理和 AI 运营。代码及定向验证已完成，真实模型、向量存储和多服务端到端仍需目标环境验收。

## 数据库与配置

全新 MySQL 8.0+ 环境依次执行 `doc/sql/table_init.sql`、`doc/sql/data_init.sql`。

已有 MySQL 环境先备份并停止写入，然后依次执行：

```text
doc/sql/table_init.sql
doc/sql/2026-10-09_product_operations.sql
doc/sql/data_init.sql
```

初始化脚本不代替 ALTER TABLE，旧库必须执行增量脚本。迁移覆盖租户/JTI/审计列、按钮菜单范围唯一约束、聊天表、run 关联及知识队列字段；可重复执行，不删除业务记录。MySQL DDL 自动提交，发生错误后需处理原因再重跑。旧任务存在重复 `(tenant_id, document_version_id, index_revision, job_type)` 时唯一索引会明确失败，需先核对记录。种子会更新其固定 ID 对应的内置配置，部署前应检查本地自定义值。

向量入库当前写 PostgreSQL/pgvector，检索默认同样使用 PostgreSQL。新环境执行 `doc/sql/postgres_knowledge_chunk.sql`，使用 `VECTOR(1536)`。已有 PostgreSQL 应先核对 tenant 列、当前审计列、维度及联合唯一索引；此 MySQL 增量脚本不会升级 PostgreSQL，也不会转换旧向量维度。Milvus 检索适配仍保留，本轮没有实现从该入库队列同步数据到 Milvus。

AI/System/Knowledge Provider 使用同一套业务 MySQL 元数据；AI 与 Knowledge 的 `KNOWLEDGE_POSTGRES_*` 必须指向同一向量表。配置有效的 `EMBEDDING_ENDPOINT`、`EMBEDDING_TOKEN`、`EMBEDDING_MODEL`，并保持 `EMBEDDING_DIMENSIONS` 与表结构一致。当前 embedding 使用 AI Provider 的全局模型配置，知识库的模型别名元数据尚未驱动独立模型路由。

各服务共享 `INTERNAL_RPC_SECRET`，Web/System/AI 使用对应 Redis 配置，Dubbo 通过 Nacos 发现服务。默认 HTTP 端口为 Web 9090、AI 9091、Student 9092、Knowledge 9093、System 9094；Provider Dubbo 端口为 Student 20880、Knowledge 20881、System 20882、AI 20883。

`KNOWLEDGE_UPLOAD_DIRECTORY` 默认 `./data/knowledge-uploads`，由 Knowledge Provider 保存原件。多实例应配置一致的持久目录并挂载共享存储；切分结果保存在 MySQL，队列执行无需读取原件。实例重启后不得丢失该目录，原件保留/清理和对象存储仍需部署方补充。

## 系统管理与权限

系统页面提供用户 CRUD、角色分配、角色 CRUD、菜单/按钮授权，以及菜单/按钮 CRUD。新用户密码为必填；编辑时留空保留旧密码。授权按钮同时选择所属菜单及父菜单，避免只有按钮而没有导航入口。

前端按 `system:user:*`、`system:role:*`、`system:menu:*`、`system:button:*`、`system:session:*` 权限显示操作，后端每次根据真实会话和数据库授权校验。R_SUPER 拥有全部功能，新菜单/按钮自动授权给该角色。用户与会话按租户隔离；角色/菜单/按钮目前是共享定义，其写操作只允许 R_SUPER。不能通过分配角色提升自身权限，也不能删除最后一个启用超级管理员。

设备页默认列出本人设备，可退出单设备；退出当前设备会注销前端。具有会话读取/下线权限的管理员可切换租户设备范围。返回字段不含 token、JTI 或摘要。

修改自己的按钮权限后，前端用户信息会刷新；其他在线用户的页面按钮需重新读取用户信息或重新进入登录流程，后端授权立即生效。动态菜单的实时重建尚需浏览器联调。

## 聊天与恢复

请求流程为提交任务、获取 runId、订阅 SSE：

```text
POST /api/ai/chat/runs
GET  /api/ai/runs/{runId}/events?after={Redis事件ID}
GET  /api/ai/runs/{runId}/snapshot
POST /api/ai/runs/{runId}/cancel
```

提交时携带问题、idempotencyKey 和可选 conversationId。请求中的 tenant/user/session 来自认证上下文；SSE、快照、取消与历史只允许对应租户的本人访问，AI 运营读取权限不会开放其他人的聊天流。

页面保存问题、幂等键、runId、游标和部分回答到当前用户的 sessionStorage。连接异常最多尝试 4 次连接；恢复请求沿用同一幂等键/runId，避免重复执行。终态失败的“重新生成”创建新幂等键。页面卸载仅断开连接，后台任务继续；刷新或重进页面可以恢复。REST 和流式 Bearer 请求共用一次刷新令牌请求，重试仍为 401 时退出登录。

Redis 事件保留并带 SSE ID，接续请求使用 after 游标。流过期而 run 已结束时返回完整回答快照及终态，完整快照替换部分内容。停止生成通过后端取消 run、工具 Future 和模型 StreamingHandle；同步规划调用及已产生的外部工具副作用无法保证立即停止。

历史接口为 `GET /api/ai/manage/conversations` 和 `GET /api/ai/conversations/{id}/messages`，支持 current/size 分页。历史下拉目前只显示最近 200 个对话；消息恢复逐页读取。默认按认证会话建立对话，选择旧对话可继续提问，跨登录从历史恢复 Redis 记忆。独立新建/删除对话、保留期限和来源引用 UI 尚未实现。

## 知识上传与异步入库

从知识库页面创建知识库并设置 FIXED 或 PARAGRAPH 策略、块大小和重叠；文档页选择知识库上传文件。支持 UTF-8 TXT/MD/CSV/JSON/HTML、DOCX、文字型 PDF，最大 4MB。扫描 PDF 不支持 OCR。块大小范围 100–8000 个 Unicode codepoint，重叠不超过块大小的一半，每文档最多 10000 块；PDF 限 1000 页和 200 万字符，DOCX 限制解压量并拒绝 DTD/外部实体。

上传在请求内完成解析、切分、原件保存和版本落库，不调用 embedding。每个版本保存内容摘要和策略快照，同文档相同内容与策略返回已有版本。修改知识库策略后再次上传可创建新版本，原版本保持可查询。

在版本抽屉点击“提交入库”，或调用 `POST /api/ai/knowledge/ingestions`。响应返回 QUEUED、jobIds 和 indexRevision，随后在任务页查看进度，每 5 秒自动刷新。未指定文档版本时选择每文档当前版本，每次最多 200 个版本；同版本同 revision 复用已有任务，失败任务通过重试接口重新排队。

AI Provider 调度器每秒轮询，事务内使用 SKIP LOCKED 领取任务，租约 120 秒且每批续租；每批最多 32 个分块。每个领取者有独立 worker token，失去租约/所有权后不能改写任务终态或激活索引。已导入分块通过联合唯一键幂等 upsert，进度落库；超时任务可从已完成位置继续。

任务页支持取消、失败重试，以及成功 revision 的“切换索引”。完整成功之前向量处于 STAGING，旧索引继续生效；完成时最新目标 revision 才自动激活，旧任务晚完成不回滚新索引。手动切换/回退只允许已成功任务，并同步文档当前版本。文档有活跃任务时不可删除，有文档时不可删除知识库；文档删除为软删除。

检索依据实时 MySQL 的 enabled 知识库、ACTIVE 文档及生效 revision，在向量库 topK 前过滤并再次校验返回记录。旧库既有向量若没有对应 active_index_revision，需重新入库建立有效索引元数据。

MySQL 状态和 PostgreSQL 激活无法原子提交：若向量激活成功而 MySQL 提交失败，可能暂时无可检索结果。应检查任务和版本状态，重试失败任务，或对成功 revision 再次执行切换，使两端对齐；不能仅凭向量存在就手工把任务标为成功。跨存储对账与故障注入测试仍待完善。

## 主要管理接口

| 范围 | 接口 | 用途 |
|---|---|---|
| 系统 | `/api/system/manage/{users,roles,menus,buttons}` | GET 分页；POST 创建；PUT/DELETE `/{id}`。 |
| 系统授权 | `/api/system/manage/users/{id}/roles`、`/roles/{id}/permissions` | PUT 分配角色、菜单和按钮。 |
| 设备 | `/api/system/manage/sessions`、`/sessions/{id}/revoke` | GET 查询、POST 下线。 |
| 知识 | `/api/knowledge/manage/{bases,documents,versions,chunks,jobs}` | GET 分页；bases/documents 支持受控写操作。 |
| 上传 | `/api/knowledge/manage/documents/upload` | POST multipart：knowledgeBaseId、可选 documentId、file。 |
| 任务 | `/api/knowledge/manage/jobs/{id}/{retry,cancel}` | POST 重试/取消。 |
| 索引 | `/api/ai/knowledge/versions/{id}/activate` | POST `{ "indexRevision": 1 }`。 |
| AI 运营 | `/api/ai/manage/{runs,steps,logs}` | GET 分页；steps 需 runId，logs 可按 requestId 查询。 |

管理分页使用 current/size，size 最大 200。管理结果 ID 为字符串，前端应保持字符串传递以避免 Snowflake 精度丢失。查询/写入字段由后端白名单限定，错误区分参数、权限、资源不存在和关联冲突。

## 验证与未覆盖范围

从仓库根目录执行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File doc/sql/verify-product-operations.ps1
```

脚本从 .env 读取 MySQL 配置，默认使用 `D:\MySQL\bin\mysql.exe`，可用 `-MySqlExecutable` 覆盖；需有创建/删除随机临时库的权限。它验证新旧结构重复迁移、数据保留、77/50 全授权，并执行 55 个后端定向测试，最后清理临时库。`-SkipTests` 只验证 SQL，不代表业务测试通过。

在 `sms-ui` 执行 `node --test scripts/check-chat-transport.cjs`、`node scripts/check-menu-seed.cjs` 和 `pnpm.cmd run build`。本轮分别通过 4 个传输测试、路由种子检查与包含类型检查的生产构建，变更文件 lint 通过。

真实 MySQL 已验证；Redis、上游流、embedding 和向量存储在定向测试中使用替身。真实多服务 Dubbo/Nacos、浏览器、模型/向量入库与召回、Redis 设备吊销、多实例故障及跨存储激活一致性尚未验收。完整 run/step 崩溃恢复、独立对话管理、知识成员 ACL、模型/工具/提示词治理和生产监控仍属后续阶段。
