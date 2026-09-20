-- Rename shared audit columns in every table of the current PostgreSQL schema.
-- The block is idempotent and also covers temporary pgvector rebuild tables.
DO $$
DECLARE
    audit_column RECORD;
BEGIN
    FOR audit_column IN
        SELECT table_schema,
               table_name,
               column_name,
               CASE column_name
                   WHEN 'sys_creator' THEN 'create_by'
                   WHEN 'sys_modifier' THEN 'modify_by'
                   WHEN 'sys_create_time' THEN 'create_time'
                   WHEN 'sys_update_time' THEN 'update_time'
               END AS new_column_name
          FROM information_schema.columns
         WHERE table_schema = current_schema()
           AND column_name IN ('sys_creator', 'sys_modifier', 'sys_create_time', 'sys_update_time')
         ORDER BY table_name, ordinal_position
    LOOP
        IF EXISTS (
            SELECT 1
              FROM information_schema.columns
             WHERE table_schema = audit_column.table_schema
               AND table_name = audit_column.table_name
               AND column_name = audit_column.new_column_name
        ) THEN
            RAISE EXCEPTION 'Both audit columns exist on %.%: % and %',
                audit_column.table_schema,
                audit_column.table_name,
                audit_column.column_name,
                audit_column.new_column_name;
        END IF;

        EXECUTE format(
            'ALTER TABLE %I.%I RENAME COLUMN %I TO %I',
            audit_column.table_schema,
            audit_column.table_name,
            audit_column.column_name,
            audit_column.new_column_name
        );
    END LOOP;
END $$;

