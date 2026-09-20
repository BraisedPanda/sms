-- Rename shared audit columns in every table of the current MySQL schema.
-- MySQL 8.0+ is required for RENAME COLUMN. The procedure is idempotent:
-- a second execution finds no legacy columns and performs no changes.
DELIMITER //
DROP PROCEDURE IF EXISTS sms_rename_audit_columns//
CREATE PROCEDURE sms_rename_audit_columns()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE table_name_value VARCHAR(64);
    DECLARE old_column_value VARCHAR(64);
    DECLARE new_column_value VARCHAR(64);
    DECLARE error_message_value VARCHAR(255);

    DECLARE audit_columns CURSOR FOR
        SELECT table_name,
               column_name,
               CASE column_name
                   WHEN 'sys_creator' THEN 'create_by'
                   WHEN 'sys_modifier' THEN 'modify_by'
                   WHEN 'sys_create_time' THEN 'create_time'
                   WHEN 'sys_update_time' THEN 'update_time'
               END
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND column_name IN ('sys_creator', 'sys_modifier', 'sys_create_time', 'sys_update_time')
         ORDER BY table_name, ordinal_position;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN audit_columns;
    rename_loop: LOOP
        FETCH audit_columns INTO table_name_value, old_column_value, new_column_value;
        IF done = 1 THEN
            LEAVE rename_loop;
        END IF;

        IF EXISTS (
            SELECT 1
              FROM information_schema.columns
             WHERE table_schema = DATABASE()
               AND table_name = table_name_value
               AND column_name = new_column_value
        ) THEN
            SET error_message_value = CONCAT('Both audit columns exist on ', table_name_value,
                                             ': ', old_column_value, ' and ', new_column_value);
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = error_message_value;
        END IF;

        SET @audit_column_ddl = CONCAT(
            'ALTER TABLE `', REPLACE(table_name_value, '`', '``'),
            '` RENAME COLUMN `', old_column_value, '` TO `', new_column_value, '`'
        );
        PREPARE audit_column_statement FROM @audit_column_ddl;
        EXECUTE audit_column_statement;
        DEALLOCATE PREPARE audit_column_statement;
    END LOOP;
    CLOSE audit_columns;
END//
DELIMITER ;

CALL sms_rename_audit_columns();
DROP PROCEDURE IF EXISTS sms_rename_audit_columns;

