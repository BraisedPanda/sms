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
