-- SMS MySQL 8.0+ bootstrap data.
-- New database: run after table_init.sql. Existing database: apply
-- 2026-10-09_chat_history_permissions.sql first. All statements use stable IDs and are idempotent.
-- Development login: Admin / 123456. Change this password immediately outside local development.
SET NAMES utf8mb4;
SET @bootstrap_tenant = 'default';
SET @bootstrap_actor = 'bootstrap';

-- Sample students keep the student query tool useful in a new local environment.
INSERT INTO student (
    id, student_no, name, age, gender, birthday, email, phone, enrolled_at, create_by, modify_by
) VALUES
    (10001, 'S20260001', '王芳', 18, '女', '2008-03-12', 'wangfang@example.com', '13800000001', '2026-09-01 08:00:00', @bootstrap_actor, @bootstrap_actor),
    (10002, 'S20260002', '李明', 19, '男', '2007-06-18', 'liming@example.com', '13800000002', '2026-09-01 08:00:00', @bootstrap_actor, @bootstrap_actor),
    (10003, 'S20260003', '王敏', 18, '女', '2008-01-25', 'wangmin@example.com', '13800000003', '2026-09-01 08:00:00', @bootstrap_actor, @bootstrap_actor)
ON DUPLICATE KEY UPDATE
    name = VALUES(name), age = VALUES(age), gender = VALUES(gender), birthday = VALUES(birthday),
    email = VALUES(email), phone = VALUES(phone), enrolled_at = VALUES(enrolled_at), modify_by = VALUES(modify_by);

-- Tool contracts must match the fields and operators accepted by the Java executors.
INSERT INTO ai_tool_definition (
    id, domain, tool_name, description, argument_specification, keywords,
    enable, version, create_by, modify_by
) VALUES
    (1000000000000000001, 'student', 'query_student',
     '按姓名、性别或学号查询学生信息。姓名支持 EQ/LIKE，性别和学号仅支持 EQ，不支持 OR/NOT。',
     'QueryCriteria: limit(integer,1..100); filter fields: name(EQ|LIKE,string), gender(EQ,string), studentNo(EQ,string)',
     '学生,查询,姓名,姓氏,性别,学号', 1, '1.0.0', @bootstrap_actor, @bootstrap_actor),
    (1000000000000000011, 'knowledge', 'query_knowledge',
     '在当前租户的启用知识库中执行语义检索，并返回受控的文档来源片段。queryText 必填，embedding 由服务端生成。',
     'QueryCriteria: queryText(string,required), knowledgeBaseId(long,optional), topK(integer,1..50), similarityThreshold(number,0..1), filter EQ fields: documentId,documentNo,documentVersionId,indexRevision,chunkNo',
     '知识库,RAG,文档,语义检索,向量,来源', 1, '1.0.0', @bootstrap_actor, @bootstrap_actor)
ON DUPLICATE KEY UPDATE
    description = VALUES(description), argument_specification = VALUES(argument_specification),
    keywords = VALUES(keywords), enable = VALUES(enable), version = VALUES(version), modify_by = VALUES(modify_by);

-- Official OpenAI endpoint definitions. api_key stores an environment-variable
-- name; no secret is persisted in MySQL.
INSERT INTO ai_model_definition (
    id, provider, model_name, base_url, api_key, alias, capabilities,
    fallback_alias, enabled, remark, create_by, modify_by
) VALUES
    (1000000000000000101, 'openai', 'gpt-4.1', 'https://api.openai.com/v1', 'OPENAI_API_KEY',
     'strong', 'chat,stream', 'balanced', '1',
     '复杂问题规划与回答模型；使用前必须配置 OPENAI_API_KEY', @bootstrap_actor, @bootstrap_actor),
    (1000000000000000102, 'openai', 'gpt-4.1-mini', 'https://api.openai.com/v1', 'OPENAI_API_KEY',
     'balanced', 'chat,stream', 'strong', '1',
     '默认平衡模型；使用前必须配置 OPENAI_API_KEY', @bootstrap_actor, @bootstrap_actor)
ON DUPLICATE KEY UPDATE
    provider = VALUES(provider), model_name = VALUES(model_name), base_url = VALUES(base_url),
    api_key = VALUES(api_key), capabilities = VALUES(capabilities), fallback_alias = VALUES(fallback_alias),
    enabled = VALUES(enabled), remark = VALUES(remark), modify_by = VALUES(modify_by);

-- Base prompt used by the planner. Task-specific tool definitions are appended
-- by AiPlanService at runtime.
INSERT INTO ai_prompt_template (
    id, prompt_code, prompt_name, prompt_content, version, enabled, remark, create_by, modify_by
) VALUES (
    1000000000000000002,
    'AI_PLAN',
    'AI planner base prompt',
    '你是一个 AI 规划助手，负责把用户问题拆分成一个或多个可执行任务。
只能输出 JSON 数组，不能输出 Markdown、代码块、解释或额外文字。
每个任务必须符合：{"domain":"chat 或工具定义中的 domain","toolName":"工具名或 null","reason":"执行理由","query":{"limit":100,"filter":{}}或 null,"missingArgs":[]}。
domain=chat 时 toolName 必须为 null，query 可为 null；domain 不是 chat 时必须从工具定义中选择 toolName。
必须严格使用工具定义列出的字段和操作符，不得臆造字段。相互独立的条件放入同一个 and 数组；未指定数量时 limit=100，没有条件时 filter=null。
student/query_student 仅支持 name、gender、studentNo。示例：“查询姓王的女生”使用 name LIKE “王” 与 gender EQ “女”。
knowledge/query_knowledge 必须填写 queryText；embedding 由服务端生成，不得在计划中生成向量。
无法满足工具契约时将缺失信息写入 missingArgs，或者退化为 chat 任务。',
    '2', '1', '与当前学生和知识检索执行器契约一致的基础规划提示词',
    @bootstrap_actor, @bootstrap_actor
)
ON DUPLICATE KEY UPDATE
    prompt_name = VALUES(prompt_name), prompt_content = VALUES(prompt_content), version = VALUES(version),
    enabled = VALUES(enabled), remark = VALUES(remark), modify_by = VALUES(modify_by);

-- Knowledge metadata and staging chunks. The configured embedding model and
-- pgvector schema both use 1536 dimensions; vector ingestion remains explicit.
INSERT INTO ai_knowledge_base (
    id, tenant_id, name, description, embedding_model_alias, chunk_size,
    chunk_overlap, topk, similarity_threshold, enabled, remark, create_by, modify_by
) VALUES (
    1000000000000000201, @bootstrap_tenant, 'langchain4j-guide',
    'LangChain4j concepts and practical usage sample knowledge base', 'text-embedding-3-small',
    800, 120, 5, 0.70, 1,
    'Run the knowledge ingestion endpoint after PostgreSQL and embedding are configured',
    @bootstrap_actor, @bootstrap_actor
)
ON DUPLICATE KEY UPDATE
    description = VALUES(description), embedding_model_alias = VALUES(embedding_model_alias),
    chunk_size = VALUES(chunk_size), chunk_overlap = VALUES(chunk_overlap), topk = VALUES(topk),
    similarity_threshold = VALUES(similarity_threshold), enabled = VALUES(enabled),
    remark = VALUES(remark), modify_by = VALUES(modify_by);

INSERT INTO ai_knowledge_document (
    id, tenant_id, knowledge_base_id, document_no, document_name, source_type,
    version, category, keywords, create_by, modify_by
) VALUES
    (1000000000000000211, @bootstrap_tenant, 1000000000000000201, 'LC4J-INTRO', 'LangChain4j Introduction', 'manual', '1.0', 'overview', 'LangChain4j,Java,LLM,AI Services', @bootstrap_actor, @bootstrap_actor),
    (1000000000000000212, @bootstrap_tenant, 1000000000000000201, 'LC4J-CHAT', 'Chat Models and AI Services', 'manual', '1.0', 'chat', 'LangChain4j,ChatLanguageModel,AI Services,prompt', @bootstrap_actor, @bootstrap_actor),
    (1000000000000000213, @bootstrap_tenant, 1000000000000000201, 'LC4J-RAG', 'Retrieval Augmented Generation', 'manual', '1.0', 'rag', 'LangChain4j,RAG,EmbeddingStore,ContentRetriever', @bootstrap_actor, @bootstrap_actor),
    (1000000000000000214, @bootstrap_tenant, 1000000000000000201, 'LC4J-TOOLS', 'Tools and Structured Outputs', 'manual', '1.0', 'tools', 'LangChain4j,tools,ToolSpecification,structured output', @bootstrap_actor, @bootstrap_actor)
ON DUPLICATE KEY UPDATE
    tenant_id = VALUES(tenant_id), knowledge_base_id = VALUES(knowledge_base_id),
    document_name = VALUES(document_name), source_type = VALUES(source_type), version = VALUES(version),
    category = VALUES(category), keywords = VALUES(keywords), modify_by = VALUES(modify_by);

INSERT INTO ai_knowledge_document_version (
    id, tenant_id, document_id, version, source_uri, source_version, mime_type,
    content_hash, active_index_revision, index_status, chunk_count, create_by, modify_by
) VALUES
    (1000000000000000221, @bootstrap_tenant, 1000000000000000211, '1.0', 'https://docs.langchain4j.dev/', '2026-09', 'text/markdown', 'lc4j-intro-v1', NULL, 'PENDING', 3, @bootstrap_actor, @bootstrap_actor),
    (1000000000000000222, @bootstrap_tenant, 1000000000000000212, '1.0', 'https://docs.langchain4j.dev/tutorials/ai-services/', '2026-09', 'text/markdown', 'lc4j-chat-v1', NULL, 'PENDING', 3, @bootstrap_actor, @bootstrap_actor),
    (1000000000000000223, @bootstrap_tenant, 1000000000000000213, '1.0', 'https://docs.langchain4j.dev/tutorials/rag/', '2026-09', 'text/markdown', 'lc4j-rag-v1', NULL, 'PENDING', 3, @bootstrap_actor, @bootstrap_actor),
    (1000000000000000224, @bootstrap_tenant, 1000000000000000214, '1.0', 'https://docs.langchain4j.dev/tutorials/tools/', '2026-09', 'text/markdown', 'lc4j-tools-v1', NULL, 'PENDING', 3, @bootstrap_actor, @bootstrap_actor)
ON DUPLICATE KEY UPDATE
    tenant_id = VALUES(tenant_id), document_id = VALUES(document_id), source_uri = VALUES(source_uri),
    source_version = VALUES(source_version), mime_type = VALUES(mime_type), content_hash = VALUES(content_hash),
    index_status = IF(index_status IN ('INDEXED', 'ACTIVE'), index_status, VALUES(index_status)),
    chunk_count = VALUES(chunk_count), modify_by = VALUES(modify_by);

INSERT INTO ai_knowledge_document_detail (
    id, tenant_id, document_id, document_version_id, chunk_no, content,
    content_type, metadata, create_by, modify_by
) VALUES
    (1000000000000000231, @bootstrap_tenant, 1000000000000000211, 1000000000000000221, 1, 'LangChain4j 是一个用于构建大语言模型应用的 Java 库。它提供 Java 优先的 API，覆盖聊天模型、向量嵌入、检索、工具调用和 AI Services。', 'text/plain', JSON_OBJECT('topic', 'overview', 'language', 'zh-CN'), @bootstrap_actor, @bootstrap_actor),
    (1000000000000000232, @bootstrap_tenant, 1000000000000000211, 1000000000000000221, 2, '一个典型的 LangChain4j 应用会配置模型供应商，通过 ChatLanguageModel 发送提示词，再将响应用于业务流程。不同供应商可以在统一接口后进行切换。', 'text/plain', JSON_OBJECT('topic', 'models', 'language', 'zh-CN'), @bootstrap_actor, @bootstrap_actor),
    (1000000000000000233, @bootstrap_tenant, 1000000000000000211, 1000000000000000221, 3, 'LangChain4j 面向 Java 设计，能够自然集成到 Spring Boot 应用中。模块化依赖让应用只引入实际需要的模型供应商和功能。', 'text/plain', JSON_OBJECT('topic', 'java-integration', 'language', 'zh-CN'), @bootstrap_actor, @bootstrap_actor),
    (1000000000000000234, @bootstrap_tenant, 1000000000000000212, 1000000000000000222, 1, 'ChatLanguageModel 是 LangChain4j 的底层聊天抽象，用于完成一次请求和一次响应。提示词可以包含系统、用户和助手消息，为模型提供指令与上下文。', 'text/plain', JSON_OBJECT('topic', 'chat-model', 'language', 'zh-CN'), @bootstrap_actor, @bootstrap_actor),
    (1000000000000000235, @bootstrap_tenant, 1000000000000000212, 1000000000000000222, 2, 'AI Services 能将 Java 接口转换为 AI 驱动的服务。使用 UserMessage 或 SystemMessage 标注方法，再通过 AiServices.builder 创建实现，方法参数会成为提示词变量。', 'text/plain', JSON_OBJECT('topic', 'ai-services', 'language', 'zh-CN'), @bootstrap_actor, @bootstrap_actor),
    (1000000000000000236, @bootstrap_tenant, 1000000000000000212, 1000000000000000222, 3, '当后续消息需要读取历史对话时，可以使用 ChatMemory。MessageWindowChatMemory 按消息数量保留窗口，TokenWindowChatMemory 则按 token 数量限制历史。', 'text/plain', JSON_OBJECT('topic', 'memory', 'language', 'zh-CN'), @bootstrap_actor, @bootstrap_actor),
    (1000000000000000237, @bootstrap_tenant, 1000000000000000213, 1000000000000000223, 1, '检索增强生成即 RAG，会在大语言模型请求中加入相关的外部知识。应用先检索源内容，再让聊天模型根据这些内容生成回答。', 'text/plain', JSON_OBJECT('topic', 'rag', 'language', 'zh-CN'), @bootstrap_actor, @bootstrap_actor),
    (1000000000000000238, @bootstrap_tenant, 1000000000000000213, 1000000000000000223, 2, 'EmbeddingModel 将文本转换为向量。将文档片段及其向量保存到 EmbeddingStore 后，可通过 EmbeddingStoreContentRetriever 为用户问题检索语义相近的片段。', 'text/plain', JSON_OBJECT('topic', 'embeddings', 'language', 'zh-CN'), @bootstrap_actor, @bootstrap_actor),
    (1000000000000000239, @bootstrap_tenant, 1000000000000000213, 1000000000000000223, 3, '文档切分会影响检索质量。应按语义将原始文档切成带重叠的片段，为每个片段保留文档元数据，并调节召回数量和最低相似度阈值。', 'text/plain', JSON_OBJECT('topic', 'document-splitting', 'language', 'zh-CN'), @bootstrap_actor, @bootstrap_actor),
    (1000000000000000240, @bootstrap_tenant, 1000000000000000214, 1000000000000000224, 1, 'LangChain4j 工具允许语言模型请求执行应用动作。Java 方法可通过 Tool 注解暴露，方法名称和描述会帮助模型判断何时调用工具。', 'text/plain', JSON_OBJECT('topic', 'tools', 'language', 'zh-CN'), @bootstrap_actor, @bootstrap_actor),
    (1000000000000000241, @bootstrap_tenant, 1000000000000000214, 1000000000000000224, 2, '工具执行始终由应用控制。应校验参数、执行权限检查、处理失败情况，并向模型返回简洁结果供下一轮响应使用。', 'text/plain', JSON_OBJECT('topic', 'tool-execution', 'language', 'zh-CN'), @bootstrap_actor, @bootstrap_actor),
    (1000000000000000242, @bootstrap_tenant, 1000000000000000214, 1000000000000000224, 3, '当模型供应商支持所需能力时，结构化输出可以将模型响应映射到 Java 类型。应定义清晰的字段和约束，并在结果影响业务逻辑前完成校验。', 'text/plain', JSON_OBJECT('topic', 'structured-output', 'language', 'zh-CN'), @bootstrap_actor, @bootstrap_actor)
ON DUPLICATE KEY UPDATE
    tenant_id = VALUES(tenant_id), document_id = VALUES(document_id),
    document_version_id = VALUES(document_version_id), content = VALUES(content),
    content_type = VALUES(content_type), metadata = VALUES(metadata), modify_by = VALUES(modify_by);

-- System administrator and the full RBAC/menu graph required by sms-ui.
INSERT INTO sys_user (
    id, tenant_id, username, password, nickname, user_type, status, create_by, modify_by
) VALUES (
    1001, @bootstrap_tenant, 'Admin',
    '$2a$10$OrVJdplQkNAnLWHiLKPxFeRZe95TFQ/ejKVBP0CVb2YivBwohC1SG',
    '系统管理员', 'SYSTEM', 'ENABLED', @bootstrap_actor, @bootstrap_actor
)
ON DUPLICATE KEY UPDATE
    tenant_id = VALUES(tenant_id), nickname = VALUES(nickname), user_type = VALUES(user_type),
    status = VALUES(status), modify_by = VALUES(modify_by);

INSERT INTO sys_role (
    id, role_code, role_name, description, role_type, status, create_by, modify_by
) VALUES (
    1101, 'R_SUPER', '超级管理员', '系统初始化超级管理员角色', 'SYSTEM', 'ENABLED',
    @bootstrap_actor, @bootstrap_actor
)
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name), description = VALUES(description), role_type = VALUES(role_type),
    status = VALUES(status), modify_by = VALUES(modify_by);

INSERT INTO sys_user_role (id, user_id, role_id, create_by, modify_by)
VALUES (1201, 1001, 1101, @bootstrap_actor, @bootstrap_actor)
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id), role_id = VALUES(role_id), modify_by = VALUES(modify_by);

-- Complete menu graph from sms-ui/src/router/modules, including hidden routes and external links.
INSERT INTO sys_menu (
    id, parent_id, path, route_name, component, redirect, title, icon, sort_no,
    keep_alive, visible, hide_tab, full_page, external_link, iframe_flag,
    active_path, status, create_by, modify_by
) VALUES
    (1305, NULL, '/dashboard', 'Dashboard', '/index/index', NULL, 'menus.dashboard.title', 'ri:pie-chart-line', 10, 0, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1306, 1305, 'console', 'Console', '/dashboard/console', NULL, 'menus.dashboard.console', 'ri:home-smile-2-line', 10, 0, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1307, 1305, 'analysis', 'Analysis', '/dashboard/analysis', NULL, 'menus.dashboard.analysis', 'ri:align-item-bottom-line', 20, 0, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1308, 1305, 'ecommerce', 'Ecommerce', '/dashboard/ecommerce', NULL, 'menus.dashboard.ecommerce', 'ri:bar-chart-box-line', 30, 0, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1309, NULL, '/template', 'Template', '/index/index', NULL, 'menus.template.title', 'ri:apps-2-line', 20, 0, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1310, 1309, 'cards', 'Cards', '/template/cards', NULL, 'menus.template.cards', 'ri:wallet-line', 10, 0, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1311, 1309, 'banners', 'Banners', '/template/banners', NULL, 'menus.template.banners', 'ri:rectangle-line', 20, 0, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1312, 1309, 'charts', 'Charts', '/template/charts', NULL, 'menus.template.charts', 'ri:bar-chart-box-line', 30, 0, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1313, 1309, 'map', 'Map', '/template/map', NULL, 'menus.template.map', 'ri:map-pin-line', 40, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1314, 1309, 'chat', 'Chat', '/template/chat', NULL, 'menus.template.chat', 'ri:message-3-line', 50, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1315, 1309, 'calendar', 'Calendar', '/template/calendar', NULL, 'menus.template.calendar', 'ri:calendar-2-line', 60, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1316, 1309, 'pricing', 'Pricing', '/template/pricing', NULL, 'menus.template.pricing', 'ri:money-cny-box-line', 70, 1, 1, 0, 1, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1317, NULL, '/widgets', 'Widgets', '/index/index', NULL, 'menus.widgets.title', 'ri:apps-2-add-line', 30, 0, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1318, 1317, 'icon', 'Icon', '/widgets/icon', NULL, 'menus.widgets.icon', 'ri:palette-line', 10, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1319, 1317, 'image-crop', 'ImageCrop', '/widgets/image-crop', NULL, 'menus.widgets.imageCrop', 'ri:screenshot-line', 20, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1320, 1317, 'excel', 'Excel', '/widgets/excel', NULL, 'menus.widgets.excel', 'ri:download-2-line', 30, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1321, 1317, 'video', 'Video', '/widgets/video', NULL, 'menus.widgets.video', 'ri:vidicon-line', 40, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1322, 1317, 'count-to', 'CountTo', '/widgets/count-to', NULL, 'menus.widgets.countTo', 'ri:anthropic-line', 50, 0, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1323, 1317, 'wang-editor', 'WangEditor', '/widgets/wang-editor', NULL, 'menus.widgets.wangEditor', 'ri:t-box-line', 60, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1324, 1317, 'watermark', 'Watermark', '/widgets/watermark', NULL, 'menus.widgets.watermark', 'ri:water-flash-line', 70, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1325, 1317, 'context-menu', 'ContextMenu', '/widgets/context-menu', NULL, 'menus.widgets.contextMenu', 'ri:menu-2-line', 80, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1326, 1317, 'qrcode', 'Qrcode', '/widgets/qrcode', NULL, 'menus.widgets.qrcode', 'ri:qr-code-line', 90, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1327, 1317, 'drag', 'Drag', '/widgets/drag', NULL, 'menus.widgets.drag', 'ri:drag-move-fill', 100, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1328, 1317, 'text-scroll', 'TextScroll', '/widgets/text-scroll', NULL, 'menus.widgets.textScroll', 'ri:input-method-line', 110, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1329, 1317, 'fireworks', 'Fireworks', '/widgets/fireworks', NULL, 'menus.widgets.fireworks', 'ri:magic-line', 120, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1330, 1317, '/outside/iframe/elementui', 'ElementUI', '', NULL, 'menus.widgets.elementUI', 'ri:apps-2-line', 130, 0, 1, 0, 0, 'https://element-plus.org/zh-CN/component/overview.html', 1, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1331, NULL, '/examples', 'Examples', '/index/index', NULL, 'menus.examples.title', 'ri:sparkling-line', 40, 0, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1332, 1331, 'permission', 'Permission', '', NULL, 'menus.examples.permission.title', 'ri:fingerprint-line', 10, 0, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1333, 1332, 'switch-role', 'PermissionSwitchRole', '/examples/permission/switch-role', NULL, 'menus.examples.permission.switchRole', 'ri:contacts-line', 10, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1334, 1332, 'button-auth', 'PermissionButtonAuth', '/examples/permission/button-auth', NULL, 'menus.examples.permission.buttonAuth', 'ri:mouse-line', 20, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1335, 1332, 'page-visibility', 'PermissionPageVisibility', '/examples/permission/page-visibility', NULL, 'menus.examples.permission.pageVisibility', 'ri:user-3-line', 30, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1336, 1331, 'tabs', 'Tabs', '/examples/tabs', NULL, 'menus.examples.tabs', 'ri:price-tag-line', 20, 0, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1337, 1331, 'tables/basic', 'TablesBasic', '/examples/tables/basic', NULL, 'menus.examples.tablesBasic', 'ri:layout-grid-line', 30, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1338, 1331, 'tables', 'Tables', '/examples/tables', NULL, 'menus.examples.tables', 'ri:table-3', 40, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1339, 1331, 'forms', 'Forms', '/examples/forms', NULL, 'menus.examples.forms', 'ri:table-view', 50, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1340, 1331, 'form/search-bar', 'SearchBar', '/examples/forms/search-bar', NULL, 'menus.examples.searchBar', 'ri:table-line', 60, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1341, 1331, 'tables/tree', 'TablesTree', '/examples/tables/tree', NULL, 'menus.examples.tablesTree', 'ri:layout-2-line', 70, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1342, 1331, 'socket-chat', 'SocketChat', '/examples/socket-chat', NULL, 'menus.examples.socketChat', 'ri:shake-hands-line', 80, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1301, NULL, '/system', 'System', '/index/index', NULL, 'menus.system.title', 'ri:user-3-line', 50, 0, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1302, 1301, 'user', 'User', '/system/user', NULL, 'menus.system.user', 'ri:user-line', 10, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1304, 1301, 'role', 'Role', '/system/role', NULL, 'menus.system.role', 'ri:user-settings-line', 20, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1366, 1301, 'user-center', 'UserCenter', '/system/user-center', NULL, 'menus.system.userCenter', 'ri:user-line', 30, 1, 0, 1, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1303, 1301, 'menu', 'Menus', '/system/menu', NULL, 'menus.system.menu', 'ri:menu-line', 40, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1357, 1301, 'nested', 'Nested', '', NULL, 'menus.system.nested', 'ri:menu-unfold-3-line', 50, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1358, 1357, 'menu1', 'NestedMenu1', '/system/nested/menu1', NULL, 'menus.system.menu1', 'ri:align-justify', 10, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1359, 1357, 'menu2', 'NestedMenu2', '', NULL, 'menus.system.menu2', 'ri:align-justify', 20, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1360, 1359, 'menu2-1', 'NestedMenu2-1', '/system/nested/menu2', NULL, 'menus.system.menu21', 'ri:align-justify', 10, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1361, 1357, 'menu3', 'NestedMenu3', '', NULL, 'menus.system.menu3', 'ri:align-justify', 30, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1362, 1361, 'menu3-1', 'NestedMenu3-1', '/system/nested/menu3', NULL, 'menus.system.menu31', NULL, 10, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1363, 1361, 'menu3-2', 'NestedMenu3-2', '', NULL, 'menus.system.menu32', NULL, 20, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1364, 1363, 'menu3-2-1', 'NestedMenu3-2-1', '/system/nested/menu3/menu3-2', NULL, 'menus.system.menu321', NULL, 10, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1343, NULL, '/article', 'Article', '/index/index', NULL, 'menus.article.title', 'ri:book-2-line', 60, 0, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1344, 1343, 'article-list', 'ArticleList', '/article/list', NULL, 'menus.article.articleList', 'ri:article-line', 10, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1345, 1343, 'detail/:id', 'ArticleDetail', '/article/detail', NULL, 'menus.article.articleDetail', NULL, 20, 1, 0, 0, 0, NULL, 0, '/article/article-list', 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1346, 1343, 'comment', 'ArticleComment', '/article/comment', NULL, 'menus.article.comment', 'ri:mail-line', 30, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1347, 1343, 'publish', 'ArticlePublish', '/article/publish', NULL, 'menus.article.articlePublish', 'ri:telegram-2-line', 40, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1348, NULL, '/result', 'Result', '/index/index', NULL, 'menus.result.title', 'ri:checkbox-circle-line', 70, 0, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1349, 1348, 'success', 'ResultSuccess', '/result/success', NULL, 'menus.result.success', 'ri:checkbox-circle-line', 10, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1350, 1348, 'fail', 'ResultFail', '/result/fail', NULL, 'menus.result.fail', 'ri:close-circle-line', 20, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1351, NULL, '/exception', 'Exception', '/index/index', NULL, 'menus.exception.title', 'ri:error-warning-line', 80, 0, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1352, 1351, '403', 'Exception403', '/exception/403', NULL, 'menus.exception.forbidden', NULL, 10, 1, 1, 1, 1, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1353, 1351, '404', 'Exception404', '/exception/404', NULL, 'menus.exception.notFound', NULL, 20, 1, 1, 1, 1, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1354, 1351, '500', 'Exception500', '/exception/500', NULL, 'menus.exception.serverError', NULL, 30, 1, 1, 1, 1, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1355, NULL, '/safeguard', 'Safeguard', '/index/index', NULL, 'menus.safeguard.title', 'ri:shield-check-line', 90, 0, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1356, 1355, 'server', 'SafeguardServer', '/safeguard/server', NULL, 'menus.safeguard.server', 'ri:hard-drive-3-line', 10, 1, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1367, NULL, '', 'Document', '', NULL, 'menus.help.document', 'ri:bill-line', 100, 0, 1, 0, 0, 'https://www.artd.pro/docs/zh/', 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1368, NULL, '', 'LiteVersion', '', NULL, 'menus.help.liteVersion', 'ri:bus-2-line', 110, 0, 1, 0, 0, 'https://www.artd.pro/docs/zh/guide/lite-version.html', 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1369, NULL, '', 'OldVersion', '', NULL, 'menus.help.oldVersion', 'ri:subway-line', 120, 0, 1, 0, 0, 'https://www.artd.pro/v2/', 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1365, NULL, '/change/log', 'ChangeLog', '/change/log', NULL, 'menus.plan.log', 'ri:gamepad-line', 130, 0, 1, 0, 0, NULL, 0, NULL, 'ENABLED', @bootstrap_actor, @bootstrap_actor)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id), path = VALUES(path), route_name = VALUES(route_name),
    component = VALUES(component), redirect = VALUES(redirect), title = VALUES(title), icon = VALUES(icon),
    sort_no = VALUES(sort_no), keep_alive = VALUES(keep_alive), visible = VALUES(visible),
    hide_tab = VALUES(hide_tab), full_page = VALUES(full_page), external_link = VALUES(external_link),
    iframe_flag = VALUES(iframe_flag), active_path = VALUES(active_path), status = VALUES(status),
    modify_by = VALUES(modify_by);

-- Existing routes and any additional enabled menus are also granted to R_SUPER.
INSERT INTO sys_role_menu (id, role_id, menu_id, create_by, modify_by)
SELECT 200000 + m.id, r.id, m.id, @bootstrap_actor, @bootstrap_actor
FROM sys_menu m CROSS JOIN sys_role r
WHERE r.role_code = 'R_SUPER' AND m.status = 'ENABLED'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id), menu_id = VALUES(menu_id), modify_by = VALUES(modify_by);

-- auth_remark is unique within a menu so reused frontend marks remain compatible.
INSERT INTO sys_button (
    id, menu_id, button_name, auth_remark, description, sort_no, status, create_by, modify_by
) VALUES
    (1501, 1302, '查询用户', 'user:read', '查询系统用户', 10, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1508, 1334, '新增', 'add', 'PermissionButtonAuth 新增', 10, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1509, 1334, '编辑', 'edit', 'PermissionButtonAuth 编辑', 20, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1510, 1334, '删除', 'delete', 'PermissionButtonAuth 删除', 30, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1511, 1334, '导出', 'export', 'PermissionButtonAuth 导出', 40, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1512, 1334, '查看', 'view', 'PermissionButtonAuth 查看', 50, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1513, 1334, '发布', 'publish', 'PermissionButtonAuth 发布', 60, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1514, 1334, '配置', 'config', 'PermissionButtonAuth 配置', 70, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1515, 1334, '管理', 'manage', 'PermissionButtonAuth 管理', 80, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1502, 1303, '新增', 'add', 'Menus 新增', 10, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1503, 1303, '编辑', 'edit', 'Menus 编辑', 20, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1504, 1303, '删除', 'delete', 'Menus 删除', 30, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1505, 1344, '新增', 'add', 'ArticleList 新增', 10, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1506, 1344, '编辑', 'edit', 'ArticleList 编辑', 20, 'ENABLED', @bootstrap_actor, @bootstrap_actor),
    (1507, 1347, '发布', 'add', 'ArticlePublish 发布', 10, 'ENABLED', @bootstrap_actor, @bootstrap_actor)
ON DUPLICATE KEY UPDATE
    menu_id = VALUES(menu_id), button_name = VALUES(button_name), auth_remark = VALUES(auth_remark),
    description = VALUES(description), sort_no = VALUES(sort_no), status = VALUES(status), modify_by = VALUES(modify_by);

INSERT INTO sys_role_button (id, role_id, button_id, create_by, modify_by)
SELECT 300000 + b.id, r.id, b.id, @bootstrap_actor, @bootstrap_actor
FROM sys_button b CROSS JOIN sys_role r
WHERE r.role_code = 'R_SUPER' AND b.status = 'ENABLED'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id), button_id = VALUES(button_id), modify_by = VALUES(modify_by);
