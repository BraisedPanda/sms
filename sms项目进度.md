# SMS 项目进度

## 2026-09-18

- `sms-web` 已完成 BFF 化：认证和系统数据通过 Dubbo 系统服务访问，不再保留数据源、JDBC、MyBatis 或本地 Mapper。
- `sms-ai-provider` 已完成流式传输解耦：应用服务仅发布 AI 运行事件到 Redis Stream，不依赖 Spring Web 或 `SseEmitter`。
- `sms-web` 的 `AiChatController` 是唯一的 SSE 适配层，负责将 Redis Stream 事件转发给 HTTP 客户端。
- 已执行 `mvn -pl sms-web,sms-ai/sms-ai-provider -am -DskipTests compile`，构建通过。
