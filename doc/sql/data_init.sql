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
