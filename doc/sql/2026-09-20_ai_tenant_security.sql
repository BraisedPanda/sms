-- Apply to the MySQL sms schema before deploying tenant-aware services.
-- Existing rows are assigned to the explicit legacy tenant `default`.
DELIMITER //
DROP PROCEDURE IF EXISTS sms_add_column_if_missing//
CREATE PROCEDURE sms_add_column_if_missing(IN table_name_value VARCHAR(64), IN column_name_value VARCHAR(64))
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = DATABASE() AND table_name = table_name_value AND column_name = column_name_value
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', table_name_value,
                          '` ADD COLUMN `', column_name_value,
                          '` VARCHAR(64) NOT NULL DEFAULT ''default'' AFTER `id`');
        PREPARE ddl_statement FROM @ddl;
        EXECUTE ddl_statement;
        DEALLOCATE PREPARE ddl_statement;
    END IF;
END//

DROP PROCEDURE IF EXISTS sms_drop_index_if_exists//
CREATE PROCEDURE sms_drop_index_if_exists(IN table_name_value VARCHAR(64), IN index_name_value VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.statistics
         WHERE table_schema = DATABASE() AND table_name = table_name_value AND index_name = index_name_value
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', table_name_value, '` DROP INDEX `', index_name_value, '`');
        PREPARE ddl_statement FROM @ddl;
        EXECUTE ddl_statement;
        DEALLOCATE PREPARE ddl_statement;
    END IF;
END//

DROP PROCEDURE IF EXISTS sms_add_index_if_missing//
CREATE PROCEDURE sms_add_index_if_missing(IN table_name_value VARCHAR(64), IN index_name_value VARCHAR(64), IN index_ddl TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
         WHERE table_schema = DATABASE() AND table_name = table_name_value AND index_name = index_name_value
    ) THEN
        SET @ddl = index_ddl;
        PREPARE ddl_statement FROM @ddl;
        EXECUTE ddl_statement;
        DEALLOCATE PREPARE ddl_statement;
    END IF;
END//
DELIMITER ;

CALL sms_add_column_if_missing('ai_request_log', 'tenant_id');
CALL sms_add_column_if_missing('ai_task_run', 'tenant_id');
CALL sms_add_column_if_missing('ai_tool_execute_log', 'tenant_id');
CALL sms_add_column_if_missing('ai_knowledge_base', 'tenant_id');
CALL sms_add_column_if_missing('ai_knowledge_document', 'tenant_id');
CALL sms_add_column_if_missing('ai_knowledge_document_detail', 'tenant_id');
CALL sms_add_column_if_missing('ai_knowledge_document_version', 'tenant_id');
CALL sms_add_column_if_missing('ai_knowledge_ingestion_job', 'tenant_id');
CALL sms_add_column_if_missing('sys_user', 'tenant_id');
CALL sms_add_column_if_missing('sys_user_session', 'tenant_id');

CALL sms_drop_index_if_exists('ai_task_run', 'uq_ai_task_run_idempotency');
CALL sms_drop_index_if_exists('ai_knowledge_base', 'uq_ai_knowledge_base_name');
CALL sms_drop_index_if_exists('ai_knowledge_document', 'uq_ai_knowledge_document_no');
CALL sms_drop_index_if_exists('ai_knowledge_document_detail', 'uq_ai_knowledge_document_detail_chunk');
CALL sms_drop_index_if_exists('ai_knowledge_document_version', 'uq_ai_knowledge_document_version');

CALL sms_add_index_if_missing('ai_task_run', 'uq_ai_task_run_tenant_idempotency',
    'CREATE UNIQUE INDEX uq_ai_task_run_tenant_idempotency ON ai_task_run (tenant_id, idempotency_key)');
CALL sms_add_index_if_missing('ai_task_run', 'idx_ai_task_run_tenant_user',
    'CREATE INDEX idx_ai_task_run_tenant_user ON ai_task_run (tenant_id, user_id)');
CALL sms_add_index_if_missing('ai_knowledge_base', 'uq_ai_knowledge_base_tenant_name',
    'CREATE UNIQUE INDEX uq_ai_knowledge_base_tenant_name ON ai_knowledge_base (tenant_id, name)');
CALL sms_add_index_if_missing('ai_knowledge_document', 'uq_ai_knowledge_document_tenant_no',
    'CREATE UNIQUE INDEX uq_ai_knowledge_document_tenant_no ON ai_knowledge_document (tenant_id, knowledge_base_id, document_no)');
CALL sms_add_index_if_missing('ai_knowledge_document', 'idx_ai_knowledge_document_tenant_base',
    'CREATE INDEX idx_ai_knowledge_document_tenant_base ON ai_knowledge_document (tenant_id, knowledge_base_id)');
CALL sms_add_index_if_missing('ai_knowledge_document_detail', 'uq_ai_knowledge_detail_tenant_chunk',
    'CREATE UNIQUE INDEX uq_ai_knowledge_detail_tenant_chunk ON ai_knowledge_document_detail (tenant_id, document_version_id, chunk_no)');
CALL sms_add_index_if_missing('ai_knowledge_document_version', 'uq_ai_knowledge_version_tenant',
    'CREATE UNIQUE INDEX uq_ai_knowledge_version_tenant ON ai_knowledge_document_version (tenant_id, document_id, version)');
CALL sms_add_index_if_missing('ai_knowledge_document_version', 'idx_ai_knowledge_version_tenant_document',
    'CREATE INDEX idx_ai_knowledge_version_tenant_document ON ai_knowledge_document_version (tenant_id, document_id)');
CALL sms_add_index_if_missing('ai_knowledge_ingestion_job', 'idx_ai_knowledge_job_tenant_version',
    'CREATE INDEX idx_ai_knowledge_job_tenant_version ON ai_knowledge_ingestion_job (tenant_id, document_version_id)');
CALL sms_add_index_if_missing('sys_user_session', 'idx_sys_user_session_tenant_user',
    'CREATE INDEX idx_sys_user_session_tenant_user ON sys_user_session (tenant_id, user_id)');

DROP PROCEDURE sms_add_column_if_missing;
DROP PROCEDURE sms_drop_index_if_exists;
DROP PROCEDURE sms_add_index_if_missing;

SOURCE system_data_init.sql;
