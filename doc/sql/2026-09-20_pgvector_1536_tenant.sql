-- pgvector cannot alter vector dimensions in place. Rebuild the index table before switching traffic.
CREATE TABLE ai_knowledge_chunk_v2 (LIKE ai_knowledge_chunk INCLUDING ALL);
ALTER TABLE ai_knowledge_chunk_v2 ADD COLUMN tenant_id VARCHAR(64) NOT NULL DEFAULT 'default';
ALTER TABLE ai_knowledge_chunk_v2 ALTER COLUMN embedding TYPE VECTOR(1536);
CREATE UNIQUE INDEX uk_ai_knowledge_chunk_v2_document ON ai_knowledge_chunk_v2
    (tenant_id, knowledge_base_id, document_id, document_version_id, index_revision, chunk_no);
CREATE INDEX idx_ai_knowledge_chunk_v2_tenant_base_status
    ON ai_knowledge_chunk_v2 (tenant_id, knowledge_base_id, status);
-- Re-embed into ai_knowledge_chunk_v2; validate counts and retrieval before renaming tables.
