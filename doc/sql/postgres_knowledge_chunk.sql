-- PostgreSQL/pgvector schema for the optional sms-knowledge vector backend.
-- Enable it with SMS_KNOWLEDGE_VECTOR_STORE=postgres.
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS ai_knowledge_chunk (
    id BIGINT PRIMARY KEY,
    knowledge_base_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    document_no VARCHAR(128) NOT NULL,
    document_version_id BIGINT NOT NULL,
    index_revision VARCHAR(64) NOT NULL,
    chunk_no INTEGER NOT NULL,
    content TEXT NOT NULL,
    metadata JSONB,
    embedding VECTOR NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    sys_creator VARCHAR(64),
    sys_modifier VARCHAR(64),
    sys_create_time TIMESTAMP,
    sys_update_time TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_knowledge_chunk_base_status
    ON ai_knowledge_chunk (knowledge_base_id, status);

-- Add an ivfflat index after data has been loaded and the embedding dimension is known.
-- CREATE INDEX idx_ai_knowledge_chunk_embedding ON ai_knowledge_chunk
-- USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
