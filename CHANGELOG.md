# 更新日志

本文档记录 SMS 项目的重要变更。

## 2026-09-16

### 新增

- `sms-web` 新增基于 JWT 的登录、刷新令牌、持久化用户会话、BCrypt 密码校验及角色/按钮 RBAC 支持。
- 新增用户、角色、菜单、按钮、关联关系及用户会话的持久化对象和 DDL。
- 新增由 Spring 托管的 AI 工作流线程池配置及 JWT 令牌单元测试。
- 新增 `.env.example` 与 `sms-ui/.env.example` 配置模板。
- `sms-common-core` 新增基于 Auth0 `java-jwt` 的共享 JWT 签发与校验服务，以及用户、会话、租户、角色、权限和数据范围上下文对象。
- `sms-ai` 新增 Spring Security 配置与 JWT 认证过滤器，保护全部 `/api/ai/**` 接口。

### 调整

- 将 `sms-ai` 重组为应用层、领域层、基础设施层和接口层。
- 将 AI 请求日志和工具执行日志实体迁入 `sms-common-persistent`。
- 优化会话幂等处理、请求日志归属、工作流执行和 Redis 业务结果过期策略。
- 修复知识入库忽略知识库、文档版本、数量限制和索引版本请求参数的问题。
- 运行配置统一由 `.env` 提供，YAML 仅保留配置结构和环境变量绑定。
- 环境变量移除 `SMS_` 前缀，例如 `SMS_AI_APPLICATION_NAME` 改为 `AI_APPLICATION_NAME`。
- 前端开发代理调整为将认证请求转发到 `sms-web:9090`。
- 更新项目进度文档，反映当前实现状态、阻塞项和验收标准。
- `sms-web` 登录和刷新令牌改用共享 JWT 服务签发；刷新时重新加载角色和按钮权限。
- `sms-ai` 聊天任务不再从请求体接收可信的用户和会话标识，改为从认证 JWT 上下文取得。
- AI 任务取消和幂等键复用增加用户及会话归属校验，防止跨用户操作。
- JWT 配置环境变量统一为 `JWT_SECRET`、`JWT_ACCESS_TOKEN_TTL_SECONDS` 与 `JWT_REFRESH_TOKEN_TTL_SECONDS`。

### 移除

- 移除 `sms-ai` 中的临时内存认证 Controller。
- 移除前端按环境拆分的配置文件，统一使用 `.env`。
- 移除 `sms-web` 对 JJWT 的依赖及模块内重复 JWT 服务实现。

