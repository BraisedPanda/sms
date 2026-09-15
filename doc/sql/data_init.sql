-- AI 工具定义初始化数据
-- 固定 ID 便于重复执行时通过唯一键幂等更新；生产环境请按实际 ID 规划策略调整。
INSERT INTO ai_tool_definition (
    id,
    domain,
    tool_name,
    description,
    argument_specification,
    keywords,
    enable
) VALUES (
    1000000000000000001,
    'student',
    'query_student',
    '按学生姓名、年级、班级、性别等条件查询学生信息',
    'QueryCriteria: name(string), grade(string), className(string), gender(string), studentNo(string), limit(int)',
    '学生,查询,年级,班级,姓名,姓氏,性别',
    1
)
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    argument_specification = VALUES(argument_specification),
    keywords = VALUES(keywords),
    enable = VALUES(enable);

-- LangChain4j staging data. ai_knowledge_document_detail is intentionally
-- persisted so a later vector ingestion job can read it from another session.
INSERT INTO ai_knowledge_base (
    id, name, description, embedding_model_alias, chunk_size, chunk_overlap,
    topk, similarity_threshold, enabled, remark
) VALUES (
    1000000000000000201,
    'langchain4j-guide',
    'LangChain4j concepts and practical usage test knowledge base',
    'bge-m3',
    800,
    120,
    5,
    0.70,
    1,
    'Vector database ingestion test data'
)
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    embedding_model_alias = VALUES(embedding_model_alias),
    chunk_size = VALUES(chunk_size),
    chunk_overlap = VALUES(chunk_overlap),
    topk = VALUES(topk),
    similarity_threshold = VALUES(similarity_threshold),
    enabled = VALUES(enabled),
    remark = VALUES(remark);

INSERT INTO ai_knowledge_document (
    id, knowledge_base_id, document_no, document_name, source_type, version, category, keywords
) VALUES
    (1000000000000000211, 1000000000000000201, 'LC4J-INTRO', 'LangChain4j Introduction', 'manual', '1.0', 'overview', 'LangChain4j,Java,LLM,AI Services'),
    (1000000000000000212, 1000000000000000201, 'LC4J-CHAT', 'Chat Models and AI Services', 'manual', '1.0', 'chat', 'LangChain4j,ChatLanguageModel,AI Services,prompt'),
    (1000000000000000213, 1000000000000000201, 'LC4J-RAG', 'Retrieval Augmented Generation', 'manual', '1.0', 'rag', 'LangChain4j,RAG,EmbeddingStore,ContentRetriever'),
    (1000000000000000214, 1000000000000000201, 'LC4J-TOOLS', 'Tools and Structured Outputs', 'manual', '1.0', 'tools', 'LangChain4j,tools,ToolSpecification,structured output')
ON DUPLICATE KEY UPDATE
    document_name = VALUES(document_name),
    source_type = VALUES(source_type),
    version = VALUES(version),
    category = VALUES(category),
    keywords = VALUES(keywords);

INSERT INTO ai_knowledge_document_version (
    id, document_id, version, source_uri, source_version, mime_type, content_hash,
    active_index_revision, index_status, chunk_count
) VALUES
    (1000000000000000221, 1000000000000000211, '1.0', 'https://docs.langchain4j.dev/', '2026-09', 'text/markdown', 'lc4j-intro-v1', '1', 'PENDING', 3),
    (1000000000000000222, 1000000000000000212, '1.0', 'https://docs.langchain4j.dev/tutorials/ai-services/', '2026-09', 'text/markdown', 'lc4j-chat-v1', '1', 'PENDING', 3),
    (1000000000000000223, 1000000000000000213, '1.0', 'https://docs.langchain4j.dev/tutorials/rag/', '2026-09', 'text/markdown', 'lc4j-rag-v1', '1', 'PENDING', 3),
    (1000000000000000224, 1000000000000000214, '1.0', 'https://docs.langchain4j.dev/tutorials/tools/', '2026-09', 'text/markdown', 'lc4j-tools-v1', '1', 'PENDING', 3)
ON DUPLICATE KEY UPDATE
    source_uri = VALUES(source_uri),
    source_version = VALUES(source_version),
    mime_type = VALUES(mime_type),
    content_hash = VALUES(content_hash),
    active_index_revision = VALUES(active_index_revision),
    index_status = VALUES(index_status),
    chunk_count = VALUES(chunk_count);

INSERT INTO ai_knowledge_document_detail (
    id, document_id, document_version_id, chunk_no, content, content_type, metadata
) VALUES
    (1000000000000000231, 1000000000000000211, 1000000000000000221, 1, 'LangChain4j 是一个用于构建大语言模型应用的 Java 库。它提供 Java 优先的 API，覆盖聊天模型、向量嵌入、检索、工具调用和 AI Services。', 'text/plain', JSON_OBJECT('topic', 'overview', 'language', 'zh-CN')),
    (1000000000000000232, 1000000000000000211, 1000000000000000221, 2, '一个典型的 LangChain4j 应用会配置模型供应商，通过 ChatLanguageModel 发送提示词，再将响应用于业务流程。不同供应商可以在统一接口后进行切换。', 'text/plain', JSON_OBJECT('topic', 'models', 'language', 'zh-CN')),
    (1000000000000000233, 1000000000000000211, 1000000000000000221, 3, 'LangChain4j 面向 Java 设计，能够自然集成到 Spring Boot 应用中。模块化依赖让应用只引入实际需要的模型供应商和功能。', 'text/plain', JSON_OBJECT('topic', 'java-integration', 'language', 'zh-CN')),
    (1000000000000000234, 1000000000000000212, 1000000000000000222, 1, 'ChatLanguageModel 是 LangChain4j 的底层聊天抽象，用于完成一次请求和一次响应。提示词可以包含系统、用户和助手消息，为模型提供指令与上下文。', 'text/plain', JSON_OBJECT('topic', 'chat-model', 'language', 'zh-CN')),
    (1000000000000000235, 1000000000000000212, 1000000000000000222, 2, 'AI Services 能将 Java 接口转换为 AI 驱动的服务。使用 UserMessage 或 SystemMessage 标注方法，再通过 AiServices.builder 创建实现，方法参数会成为提示词变量。', 'text/plain', JSON_OBJECT('topic', 'ai-services', 'language', 'zh-CN')),
    (1000000000000000236, 1000000000000000212, 1000000000000000222, 3, '当后续消息需要读取历史对话时，可以使用 ChatMemory。MessageWindowChatMemory 按消息数量保留窗口，TokenWindowChatMemory 则按 token 数量限制历史。', 'text/plain', JSON_OBJECT('topic', 'memory', 'language', 'zh-CN')),
    (1000000000000000237, 1000000000000000213, 1000000000000000223, 1, '检索增强生成即 RAG，会在大语言模型请求中加入相关的外部知识。应用先检索源内容，再让聊天模型根据这些内容生成回答。', 'text/plain', JSON_OBJECT('topic', 'rag', 'language', 'zh-CN')),
    (1000000000000000238, 1000000000000000213, 1000000000000000223, 2, 'EmbeddingModel 将文本转换为向量。将文档片段及其向量保存到 EmbeddingStore 后，可通过 EmbeddingStoreContentRetriever 为用户问题检索语义相近的片段。', 'text/plain', JSON_OBJECT('topic', 'embeddings', 'language', 'zh-CN')),
    (1000000000000000239, 1000000000000000213, 1000000000000000223, 3, '文档切分会影响检索质量。应按语义将原始文档切成带重叠的片段，为每个片段保留文档元数据，并调节召回数量和最低相似度阈值。', 'text/plain', JSON_OBJECT('topic', 'document-splitting', 'language', 'zh-CN')),
    (1000000000000000240, 1000000000000000214, 1000000000000000224, 1, 'LangChain4j 工具允许语言模型请求执行应用动作。Java 方法可通过 Tool 注解暴露，方法名称和描述会帮助模型判断何时调用工具。', 'text/plain', JSON_OBJECT('topic', 'tools', 'language', 'zh-CN')),
    (1000000000000000241, 1000000000000000214, 1000000000000000224, 2, '工具执行始终由应用控制。应校验参数、执行权限检查、处理失败情况，并向模型返回简洁结果供下一轮响应使用。', 'text/plain', JSON_OBJECT('topic', 'tool-execution', 'language', 'zh-CN')),
    (1000000000000000242, 1000000000000000214, 1000000000000000224, 3, '当模型供应商支持所需能力时，结构化输出可以将模型响应映射到 Java 类型。应定义清晰的字段和约束，并在结果影响业务逻辑前完成校验。', 'text/plain', JSON_OBJECT('topic', 'structured-output', 'language', 'zh-CN'))
ON DUPLICATE KEY UPDATE
    document_id = VALUES(document_id),
    content = VALUES(content),
    content_type = VALUES(content_type),
    metadata = VALUES(metadata),
    sys_update_time = CURRENT_TIMESTAMP;

-- AI 模型定义初始化数据。api_key 保存环境变量名，不直接保存密钥。
INSERT INTO ai_model_definition (
    id,
    provider,
    model_name,
    base_url,
    api_key,
    alias,
    capabilities,
    fallback_alias,
    enabled,
    remark
) VALUES (
    1000000000000000101,
    'openai',
    'gpt-5.6-terra',
    'https://blankapi.com/v1',
    'OPENAI_API_KEY',
    'strong',
    'chat,stream',
    'balanced',
    '1',
    'OpenAI 兼容接口的强模型，优先用于复杂问题规划和回答'
), (
    1000000000000000102,
    'deepseek',
    'deepseek-chat',
    'https://api.deepseek.com/v1',
    'DEEPSEEK_API_KEY',
    'balanced',
    'chat,stream',
    'strong',
    '0',
    'DeepSeek 通用对话模型，配置密钥后可启用为默认模型'
)
ON DUPLICATE KEY UPDATE
    base_url = VALUES(base_url),
    api_key = VALUES(api_key),
    capabilities = VALUES(capabilities),
    fallback_alias = VALUES(fallback_alias),
    enabled = VALUES(enabled),
    remark = VALUES(remark);

-- Base prompt used by the AI planner. Keep the task-specific context and tool
-- definitions outside this template; AiPlanService appends them at runtime.
INSERT INTO ai_prompt_template (
    id,
    prompt_code,
    prompt_name,
    prompt_content,
    version,
    enabled,
    remark
) VALUES (
    1000000000000000002,
    'AI_PLAN',
    'AI planner base prompt',
    '你是一个 AI 规划助手，负责把用户问题拆分成一个或多个可执行任务。\n只能输出 JSON 数组，不能输出 Markdown、代码块、解释或额外文字。\n每个任务必须符合：{"domain":"chat 或工具定义中的 domain","toolName":"工具名或 null",\n"reason":"执行理由","query":{"limit":100,"filter":{}},"missingArgs":[]}。\ndomain=chat 时 toolName 必须为 null，query 可为 null；domain 不是 chat 时必须从工具定义中选择 toolName。\nQueryCriteria 用于转换为 MyBatis-Plus QueryWrapper：\nfilter 叶子节点使用 field、operator、value；逻辑节点使用 and、or、not。\noperator 只能使用 EQ、NE、LIKE、NOT_LIKE、GT、GE、LT、LE、IN、NOT_IN、BETWEEN、IS_NULL、IS_NOT_NULL；IN/NOT_IN/BETWEEN 使用 values。\n提取问题中的所有明确条件（例如年级、班级、姓氏、性别、时间和数量），相互独立的条件放入同一个 and 数组；不确定的参数放入 missingArgs。\nfield 必须使用工具参数说明中的实体属性或数据库字段名；未指定数量时 limit=100，没有条件时 filter=null。\n\n解析示例：‘查询一年级1班所有姓王的女生信息’应生成 student/query_student，并在 query.filter.and 中放入 grade EQ ‘一年级’、className EQ ‘1班’、name LIKE ‘王’、gender EQ ‘女’ 四个条件。\n\n',
    '1',
    '1',
    'Base instructions for task planning'
)
ON DUPLICATE KEY UPDATE
    prompt_name = VALUES(prompt_name),
    prompt_content = VALUES(prompt_content),
    version = VALUES(version),
    enabled = VALUES(enabled),
    remark = VALUES(remark);

INSERT INTO ai_tool_definition (
    id, domain, tool_name, description, argument_specification, keywords, enable
) VALUES (
    1000000000000000011,
    'knowledge',
    'query_knowledge',
    'Search enabled knowledge bases using semantic vector retrieval and return source chunks.',
    'QueryCriteria: queryText(string), embedding(float[]), knowledgeBaseId(long), topK(int), similarityThreshold(double), filter(object)',
    'knowledge,RAG,document,semantic search,vector,milvus',
    1
)
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    argument_specification = VALUES(argument_specification),
    keywords = VALUES(keywords),
    enable = VALUES(enable);
