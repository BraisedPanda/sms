-- student 表 DDL
CREATE TABLE IF NOT EXISTS student (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_no VARCHAR(64) NOT NULL COMMENT '学号',
    name VARCHAR(128) NOT NULL COMMENT '姓名',
    age INT DEFAULT NULL COMMENT '年龄',
    gender VARCHAR(16) DEFAULT NULL COMMENT '性别',
    birthday DATE DEFAULT NULL COMMENT '出生日期',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    phone VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
    enrolled_at DATETIME DEFAULT NULL COMMENT '入学时间',
    sys_creator VARCHAR(64) DEFAULT NULL COMMENT '记录创建者',
    sys_modifier VARCHAR(64) DEFAULT NULL COMMENT '记录修改者',
    sys_create_time DATETIME DEFAULT NULL COMMENT '记录创建时间',
    sys_update_time DATETIME DEFAULT NULL COMMENT '记录更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uq_student_no (student_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生表';

-- AI 工具定义表
CREATE TABLE IF NOT EXISTS ai_tool_definition (
    id BIGINT NOT NULL COMMENT '主键（雪花算法）',
    domain VARCHAR(128) NOT NULL COMMENT '工具所属领域',
    tool_name VARCHAR(128) NOT NULL COMMENT '工具名称',
    description TEXT DEFAULT NULL COMMENT '工具描述',
    argument_specification TEXT DEFAULT NULL COMMENT '参数定义',
    keywords TEXT DEFAULT NULL COMMENT '关键词',
    enable TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用',
    version VARCHAR(20) DEFAULT NULL COMMENT '版本号',
    sys_creator VARCHAR(64) DEFAULT NULL COMMENT '记录创建者',
    sys_modifier VARCHAR(64) DEFAULT NULL COMMENT '记录修改者',
    sys_create_time DATETIME DEFAULT NULL COMMENT '记录创建时间',
    sys_update_time DATETIME DEFAULT NULL COMMENT '记录更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uq_ai_tool_definition_domain_name (domain, tool_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 工具定义表';

-- AI request invocation log
CREATE TABLE IF NOT EXISTS ai_request_log (
    id BIGINT NOT NULL COMMENT 'primary key',
    request_id VARCHAR(64) NOT NULL COMMENT 'request trace id',
    user_id VARCHAR(64) DEFAULT NULL,
    session_id VARCHAR(128) DEFAULT NULL,
    request_type VARCHAR(32) DEFAULT NULL,
    question TEXT DEFAULT NULL,
    model_name VARCHAR(128) DEFAULT NULL,
    start_time DATETIME(3) NOT NULL,
    finish_time DATETIME(3) DEFAULT NULL,
    duration_time BIGINT DEFAULT NULL COMMENT 'duration in milliseconds',
    input_token_count INT DEFAULT NULL,
    output_token_count INT DEFAULT NULL,
    total_token_count INT DEFAULT NULL,
    success TINYINT(1) NOT NULL DEFAULT 0,
    error_code VARCHAR(64) DEFAULT NULL,
    error_message TEXT DEFAULT NULL,
    sys_creator VARCHAR(64) DEFAULT NULL,
    sys_modifier VARCHAR(64) DEFAULT NULL,
    sys_create_time DATETIME DEFAULT NULL,
    sys_update_time DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ai_request_log_request_id (request_id),
    KEY idx_ai_request_log_session (session_id),
    KEY idx_ai_request_log_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI request invocation log';

-- AI tool execution log
CREATE TABLE IF NOT EXISTS ai_tool_execute_log (
    id BIGINT NOT NULL COMMENT 'primary key',
    tool_execute_id VARCHAR(64) NOT NULL COMMENT 'tool execution trace id',
    request_id VARCHAR(64) DEFAULT NULL,
    domain VARCHAR(128) NOT NULL,
    tool_name VARCHAR(128) NOT NULL,
    question TEXT DEFAULT NULL,
    model_name VARCHAR(128) DEFAULT NULL,
    start_time DATETIME(3) NOT NULL,
    finish_time DATETIME(3) DEFAULT NULL,
    duration_time BIGINT DEFAULT NULL COMMENT 'duration in milliseconds',
    input_token_count INT DEFAULT NULL,
    output_token_count INT DEFAULT NULL,
    total_token_count INT DEFAULT NULL,
    result_count INT DEFAULT NULL,
    success TINYINT(1) NOT NULL DEFAULT 0,
    error_code VARCHAR(64) DEFAULT NULL,
    error_message TEXT DEFAULT NULL,
    sys_creator VARCHAR(64) DEFAULT NULL,
    sys_modifier VARCHAR(64) DEFAULT NULL,
    sys_create_time DATETIME DEFAULT NULL,
    sys_update_time DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ai_tool_execute_log_execute_id (tool_execute_id),
    KEY idx_ai_tool_execute_log_request_id (request_id),
    KEY idx_ai_tool_execute_log_start_time (start_time),
    KEY idx_ai_tool_execute_log_tool (domain, tool_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI tool execution log';
