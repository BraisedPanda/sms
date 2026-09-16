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

-- AI 模型定义表
CREATE TABLE IF NOT EXISTS ai_model_definition (
    id BIGINT NOT NULL COMMENT '主键（雪花算法）',
    provider VARCHAR(64) NOT NULL COMMENT '模型厂商，例如 openai、deepseek',
    model_name VARCHAR(128) NOT NULL COMMENT '厂商模型名称',
    base_url VARCHAR(500) DEFAULT NULL COMMENT '模型服务基础地址',
    api_key VARCHAR(128) NOT NULL COMMENT 'API key 环境变量名，例如 OPENAI_API_KEY',
    alias VARCHAR(64) NOT NULL COMMENT '模型别名，例如 balanced、strong、fast',
    capabilities VARCHAR(255) DEFAULT NULL COMMENT '模型能力，逗号分隔，例如 chat,stream',
    fallback_alias VARCHAR(64) DEFAULT NULL COMMENT '当前模型不可用时的回退模型别名',
    enabled VARCHAR(16) NOT NULL DEFAULT '1' COMMENT '是否启用，支持 1、true、yes',
    remark VARCHAR(500) DEFAULT NULL COMMENT '中文备注',
    sys_creator VARCHAR(64) DEFAULT NULL COMMENT '记录创建者',
    sys_modifier VARCHAR(64) DEFAULT NULL COMMENT '记录修改者',
    sys_create_time DATETIME DEFAULT NULL COMMENT '记录创建时间',
    sys_update_time DATETIME DEFAULT NULL COMMENT '记录更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uq_ai_model_definition_alias (alias),
    UNIQUE KEY uq_ai_model_definition_provider_name (provider, model_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 模型定义表';

-- AI prompt template table
CREATE TABLE IF NOT EXISTS ai_prompt_template (
    id BIGINT NOT NULL COMMENT 'primary key',
    prompt_code VARCHAR(128) NOT NULL COMMENT 'prompt template code',
    prompt_name VARCHAR(128) NOT NULL COMMENT 'prompt template name',
    prompt_content TEXT NOT NULL COMMENT 'prompt template content',
    version VARCHAR(32) DEFAULT NULL COMMENT 'prompt template version',
    enabled VARCHAR(16) NOT NULL DEFAULT '1' COMMENT 'whether the template is enabled',
    remark VARCHAR(500) DEFAULT NULL COMMENT 'remark',
    sys_creator VARCHAR(64) DEFAULT NULL COMMENT 'creator',
    sys_modifier VARCHAR(64) DEFAULT NULL COMMENT 'modifier',
    sys_create_time DATETIME DEFAULT NULL COMMENT 'create time',
    sys_update_time DATETIME DEFAULT NULL COMMENT 'update time',
    PRIMARY KEY (id),
    UNIQUE KEY uq_ai_prompt_template_code (prompt_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI prompt templates';

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

-- AI task run lifecycle. A run groups planning, tool execution and compose steps.
CREATE TABLE IF NOT EXISTS ai_task_run (
    id BIGINT NOT NULL COMMENT 'primary key',
    run_id VARCHAR(64) NOT NULL COMMENT 'run trace id',
    request_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) DEFAULT NULL,
    session_id VARCHAR(128) DEFAULT NULL,
    run_type VARCHAR(32) NOT NULL DEFAULT 'CHAT',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    question TEXT NOT NULL,
    model_alias VARCHAR(64) DEFAULT NULL,
    plan_json LONGTEXT DEFAULT NULL,
    idempotency_key VARCHAR(128) DEFAULT NULL,
    current_step_no INT NOT NULL DEFAULT 0,
    cancel_request TINYINT(1) NOT NULL DEFAULT 0,
    cancel_request_time DATETIME(3) DEFAULT NULL,
    start_time DATETIME(3) NOT NULL,
    finish_time DATETIME(3) DEFAULT NULL,
    error_code VARCHAR(64) DEFAULT NULL,
    error_message TEXT DEFAULT NULL,
    sys_creator VARCHAR(64) DEFAULT NULL,
    sys_modifier VARCHAR(64) DEFAULT NULL,
    sys_create_time DATETIME DEFAULT NULL,
    sys_update_time DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ai_task_run_run_id (run_id),
    UNIQUE KEY uq_ai_task_run_idempotency (idempotency_key),
    KEY idx_ai_task_run_request (request_id),
    KEY idx_ai_task_run_status (status),
    KEY idx_ai_task_run_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI task run lifecycle';

-- AI task step lifecycle. A step is a plan, tool or compose operation.
CREATE TABLE IF NOT EXISTS ai_task_step (
    id BIGINT NOT NULL COMMENT 'primary key',
    step_id VARCHAR(64) NOT NULL COMMENT 'step trace id',
    task_run_id VARCHAR(64) NOT NULL,
    step_no INT NOT NULL,
    step_type VARCHAR(32) NOT NULL,
    domain VARCHAR(128) DEFAULT NULL,
    tool_name VARCHAR(128) DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    input_json LONGTEXT DEFAULT NULL,
    output_json LONGTEXT DEFAULT NULL,
    attempt INT NOT NULL DEFAULT 0,
    max_attempt INT NOT NULL DEFAULT 3,
    timeout_ms BIGINT NOT NULL DEFAULT 30000,
    next_retry_time DATETIME(3) DEFAULT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    lease_expire_time DATETIME(3) DEFAULT NULL,
    start_time DATETIME(3) DEFAULT NULL,
    finish_time DATETIME(3) DEFAULT NULL,
    error_code VARCHAR(64) DEFAULT NULL,
    error_message TEXT DEFAULT NULL,
    sys_creator VARCHAR(64) DEFAULT NULL,
    sys_modifier VARCHAR(64) DEFAULT NULL,
    sys_create_time DATETIME DEFAULT NULL,
    sys_update_time DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ai_task_step_step_id (step_id),
    UNIQUE KEY uq_ai_task_step_run_no (task_run_id, step_no),
    UNIQUE KEY uq_ai_task_step_idempotency (idempotency_key),
    KEY idx_ai_task_step_status_lease (status, lease_expire_time),
    KEY idx_ai_task_step_run (task_run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI task step lifecycle';

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

-- Knowledge base metadata used by the RAG ingestion and retrieval services.
CREATE TABLE IF NOT EXISTS ai_knowledge_base (
    id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    embedding_model_alias VARCHAR(128) DEFAULT NULL,
    chunk_size INT NOT NULL DEFAULT 800,
    chunk_overlap INT NOT NULL DEFAULT 120,
    topk INT NOT NULL DEFAULT 5,
    similarity_threshold DOUBLE DEFAULT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    remark VARCHAR(500) DEFAULT NULL,
    sys_creator VARCHAR(64) DEFAULT NULL,
    sys_modifier VARCHAR(64) DEFAULT NULL,
    sys_create_time DATETIME DEFAULT NULL,
    sys_update_time DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ai_knowledge_base_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG knowledge bases';

CREATE TABLE IF NOT EXISTS ai_knowledge_document (
    id BIGINT NOT NULL,
    knowledge_base_id BIGINT NOT NULL,
    document_no VARCHAR(128) NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    source_type VARCHAR(64) DEFAULT NULL,
    version VARCHAR(64) DEFAULT NULL,
    category VARCHAR(128) DEFAULT NULL,
    keywords TEXT DEFAULT NULL,
    sys_creator VARCHAR(64) DEFAULT NULL,
    sys_modifier VARCHAR(64) DEFAULT NULL,
    sys_create_time DATETIME DEFAULT NULL,
    sys_update_time DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ai_knowledge_document_no (knowledge_base_id, document_no),
    KEY idx_ai_knowledge_document_base (knowledge_base_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG documents';

-- Persistent staging details used as the source for vector database ingestion.
CREATE TABLE IF NOT EXISTS ai_knowledge_document_detail (
    id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    document_version_id BIGINT NOT NULL,
    chunk_no INT NOT NULL,
    content TEXT NOT NULL,
    content_type VARCHAR(64) DEFAULT 'text/plain',
    metadata JSON DEFAULT NULL,
    sys_creator VARCHAR(64) DEFAULT NULL,
    sys_modifier VARCHAR(64) DEFAULT NULL,
    sys_create_time DATETIME DEFAULT NULL,
    sys_update_time DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ai_knowledge_document_detail_chunk (document_version_id, chunk_no),
    KEY idx_ai_knowledge_document_detail_document (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG document content staging details';

CREATE TABLE IF NOT EXISTS ai_knowledge_document_version (
    id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    version VARCHAR(64) NOT NULL,
    source_uri VARCHAR(1000) DEFAULT NULL,
    source_version VARCHAR(128) DEFAULT NULL,
    mime_type VARCHAR(128) DEFAULT NULL,
    content_hash VARCHAR(128) DEFAULT NULL,
    active_index_revision VARCHAR(64) DEFAULT NULL,
    index_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    chunk_count INT DEFAULT NULL,
    error_message TEXT DEFAULT NULL,
    sys_creator VARCHAR(64) DEFAULT NULL,
    sys_modifier VARCHAR(64) DEFAULT NULL,
    sys_create_time DATETIME DEFAULT NULL,
    sys_update_time DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ai_knowledge_document_version (document_id, version),
    KEY idx_ai_knowledge_document_version_status (document_id, index_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG document versions';

CREATE TABLE IF NOT EXISTS ai_knowledge_ingestion_job (
    id BIGINT NOT NULL,
    document_version_id BIGINT NOT NULL,
    index_revision VARCHAR(64) NOT NULL,
    job_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    progress INT NOT NULL DEFAULT 0,
    retry_count INT NOT NULL DEFAULT 0,
    error_message TEXT DEFAULT NULL,
    start_time DATETIME(3) DEFAULT NULL,
    finish_time DATETIME(3) DEFAULT NULL,
    sys_creator VARCHAR(64) DEFAULT NULL,
    sys_modifier VARCHAR(64) DEFAULT NULL,
    sys_create_time DATETIME DEFAULT NULL,
    sys_update_time DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_ai_knowledge_ingestion_job_version (document_version_id),
    KEY idx_ai_knowledge_ingestion_job_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG ingestion jobs';




-- pgsql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS ai_knowledge_chunk
(
    id                  BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,

    knowledge_base_id   BIGINT NOT NULL,
    document_id         BIGINT NOT NULL,
    document_no         VARCHAR(100) NOT NULL,
    document_version_id BIGINT NOT NULL,

    index_revision      INTEGER NOT NULL DEFAULT 1,
    chunk_no            INTEGER NOT NULL,

    content             TEXT NOT NULL,

    metadata            JSONB,

    embedding           VECTOR(1024),

    status              VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',

    sys_creator         BIGINT,
    sys_modifier        BIGINT,

    sys_create_time     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sys_update_time     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_knowledge_chunk_document ON ai_knowledge_chunk
    ( knowledge_base_id, document_id, document_version_id, index_revision, chunk_no );
-- System identity and authorization (RBAC)
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(64) DEFAULT NULL,
    real_name VARCHAR(64) DEFAULT NULL,
    email VARCHAR(128) DEFAULT NULL,
    phone VARCHAR(32) DEFAULT NULL,
    avatar VARCHAR(512) DEFAULT NULL,
    user_type VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    last_login_time DATETIME DEFAULT NULL,
    last_login_ip VARCHAR(64) DEFAULT NULL,
    sys_creator VARCHAR(64) DEFAULT NULL,
    sys_modifier VARCHAR(64) DEFAULT NULL,
    sys_create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sys_update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    KEY idx_sys_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System user';

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(64) NOT NULL,
    description VARCHAR(512) DEFAULT NULL,
    role_type VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    sys_creator VARCHAR(64) DEFAULT NULL,
    sys_modifier VARCHAR(64) DEFAULT NULL,
    sys_create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sys_update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_code (role_code),
    KEY idx_sys_role_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System role';

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    sys_creator VARCHAR(64) DEFAULT NULL,
    sys_modifier VARCHAR(64) DEFAULT NULL,
    sys_create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sys_update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_role (user_id, role_id),
    KEY idx_sys_user_role_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User-role relationship';

CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT NOT NULL,
    parent_id BIGINT DEFAULT NULL,
    path VARCHAR(256) DEFAULT NULL,
    route_name VARCHAR(128) DEFAULT NULL,
    component VARCHAR(256) DEFAULT NULL,
    redirect VARCHAR(256) DEFAULT NULL,
    title VARCHAR(128) NOT NULL,
    icon VARCHAR(128) DEFAULT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    keep_alive TINYINT(1) NOT NULL DEFAULT 0,
    visible TINYINT(1) NOT NULL DEFAULT 1,
    hide_tab TINYINT(1) NOT NULL DEFAULT 0,
    full_page TINYINT(1) NOT NULL DEFAULT 0,
    external_link VARCHAR(512) DEFAULT NULL,
    iframe_flag TINYINT(1) NOT NULL DEFAULT 0,
    active_path VARCHAR(256) DEFAULT NULL,
    status VARCHAR(16) NOT NULL,
    sys_creator VARCHAR(64) DEFAULT NULL,
    sys_modifier VARCHAR(64) DEFAULT NULL,
    sys_create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sys_update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_sys_menu_parent_id (parent_id),
    KEY idx_sys_menu_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System menu';

CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    sys_creator VARCHAR(64) DEFAULT NULL,
    sys_modifier VARCHAR(64) DEFAULT NULL,
    sys_create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sys_update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_menu (role_id, menu_id),
    KEY idx_sys_role_menu_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Role-menu relationship';

CREATE TABLE IF NOT EXISTS sys_button (
    id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    button_name VARCHAR(64) NOT NULL,
    auth_remark VARCHAR(128) NOT NULL,
    description VARCHAR(512) DEFAULT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL,
    sys_creator VARCHAR(64) DEFAULT NULL,
    sys_modifier VARCHAR(64) DEFAULT NULL,
    sys_create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sys_update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_button_auth_remark (auth_remark),
    KEY idx_sys_button_menu_id (menu_id),
    KEY idx_sys_button_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System button permission';

CREATE TABLE IF NOT EXISTS sys_role_button (
    id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    button_id BIGINT NOT NULL,
    sys_creator VARCHAR(64) DEFAULT NULL,
    sys_modifier VARCHAR(64) DEFAULT NULL,
    sys_create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sys_update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_button (role_id, button_id),
    KEY idx_sys_role_button_button_id (button_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Role-button relationship';

CREATE TABLE IF NOT EXISTS sys_user_session (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    access_token TEXT NOT NULL,
    refresh_token TEXT NOT NULL,
    login_ip VARCHAR(64) DEFAULT NULL,
    user_agent VARCHAR(1024) DEFAULT NULL,
    device_type VARCHAR(32) NOT NULL,
    login_time DATETIME NOT NULL,
    expire_time DATETIME NOT NULL,
    refresh_expire_time DATETIME NOT NULL,
    status VARCHAR(16) NOT NULL,
    logout_time DATETIME DEFAULT NULL,
    sys_creator VARCHAR(64) DEFAULT NULL,
    sys_modifier VARCHAR(64) DEFAULT NULL,
    sys_create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sys_update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_sys_user_session_user_id (user_id),
    KEY idx_sys_user_session_status_expire (status, expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User login session';
