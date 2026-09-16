package com.xqy.sms.knowledge.provider.milvus;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xqy.sms.knowledge.api.entity.AiknowledgeChunk;
import com.xqy.sms.knowledge.api.model.KnowledgeVectorQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** pgvector implementation for installations that keep chunks in PostgreSQL. */
@Component
@ConditionalOnProperty(name = "sms.knowledge.vector-store", havingValue = "postgres")
public class PostgresVectorStore implements MilvusVectorStore {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final String tableName;

    public PostgresVectorStore(ObjectMapper objectMapper,
                               @Value("${sms.knowledge.postgres.url}") String url,
                               @Value("${sms.knowledge.postgres.username}") String username,
                               @Value("${sms.knowledge.postgres.password}") String password,
                               @Value("${sms.knowledge.postgres.table}") String tableName) {
        this.objectMapper = objectMapper;
        this.tableName = safeTableName(tableName);
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, username, password);
        dataSource.setDriverClassName("org.postgresql.Driver");
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public List<AiknowledgeChunk> search(KnowledgeVectorQuery query) {
        if (query == null || query.getEmbedding() == null || query.getEmbedding().isEmpty()) {
            return Collections.emptyList();
        }
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT id, knowledge_base_id, document_id, document_no, "
                + "document_version_id, index_revision, chunk_no, content, metadata, status, "
                + "1 - (embedding <=> CAST(? AS vector)) AS score FROM ")
                .append(tableName).append(" WHERE status = 'ACTIVE'");
        args.add(vectorLiteral(query.getEmbedding()));
        if (query.getKnowledgeBaseId() != null) {
            sql.append(" AND knowledge_base_id = ?");
            args.add(query.getKnowledgeBaseId());
        }
        appendFilters(sql, args, query.getFilter());
        sql.append(" ORDER BY embedding <=> CAST(? AS vector) LIMIT ?");
        args.add(vectorLiteral(query.getEmbedding()));
        args.add(safeLimit(query.getTopK()));
        return jdbcTemplate.query(sql.toString(), this::mapChunk, args.toArray());
    }

    private void appendFilters(StringBuilder sql, List<Object> args, Map<String, Object> filters) {
        if (filters == null) return;
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String column = switch (entry.getKey()) {
                case "documentId", "document_id" -> "document_id";
                case "documentNo", "document_no" -> "document_no";
                case "documentVersionId", "document_version_id" -> "document_version_id";
                case "indexRevision", "index_revision" -> "index_revision";
                case "chunkNo", "chunk_no" -> "chunk_no";
                default -> null;
            };
            if (column != null && entry.getValue() != null) {
                sql.append(" AND ").append(column).append(" = ?");
                args.add(entry.getValue());
            }
        }
    }

    private AiknowledgeChunk mapChunk(ResultSet rs, int rowNum) throws java.sql.SQLException {
        AiknowledgeChunk chunk = new AiknowledgeChunk();
        chunk.setId(rs.getLong("id"));
        chunk.setKnowledgeBaseId(nullableLong(rs, "knowledge_base_id"));
        chunk.setDocumentId(nullableLong(rs, "document_id"));
        chunk.setDocumentNo(rs.getString("document_no"));
        chunk.setDocumentVersionId(nullableLong(rs, "document_version_id"));
        chunk.setIndexRevision(rs.getString("index_revision"));
        chunk.setChunkNo((Integer) rs.getObject("chunk_no"));
        chunk.setContent(rs.getString("content"));
        chunk.setStatus(rs.getString("status"));
        double score = rs.getDouble("score");
        if (!rs.wasNull()) chunk.setScore(score);
        String metadata = rs.getString("metadata");
        if (metadata != null && !metadata.isBlank()) {
            try {
                chunk.setMetadata(objectMapper.readValue(metadata, new TypeReference<>() { }));
            } catch (Exception ignored) {
                // Keep retrieval usable when an old row contains malformed metadata.
            }
        }
        return chunk;
    }

    private Long nullableLong(ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private String vectorLiteral(List<Float> embedding) {
        StringBuilder value = new StringBuilder("[");
        for (int i = 0; i < embedding.size(); i++) {
            if (i > 0) value.append(',');
            value.append(embedding.get(i));
        }
        return value.append(']').toString();
    }

    private int safeLimit(Integer limit) { return limit == null || limit <= 0 ? 10 : Math.min(limit, 1000); }

    private String safeTableName(String value) {
        String table = value == null || value.isBlank() ? "ai_knowledge_chunk" : value.trim();
        if (!table.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid PostgreSQL vector table name");
        }
        return table;
    }
}
