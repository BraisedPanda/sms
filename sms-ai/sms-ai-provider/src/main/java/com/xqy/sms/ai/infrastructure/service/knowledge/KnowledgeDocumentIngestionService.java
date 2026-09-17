package com.xqy.sms.ai.infrastructure.service.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Reads MySQL staging details, creates embeddings, and upserts them into pgvector. */
@Service
public class KnowledgeDocumentIngestionService {
    private final JdbcTemplate sourceJdbcTemplate;
    private final JdbcTemplate vectorJdbcTemplate;
    private final KnowledgeEmbeddingService embeddingService;
    private final ObjectMapper objectMapper;
    private final String vectorTable;
    private final int defaultLimit;
    private final int maxLimit;
    private final int defaultIndexRevision;

    public KnowledgeDocumentIngestionService(
            JdbcTemplate sourceJdbcTemplate,
            KnowledgeEmbeddingService embeddingService,
            ObjectMapper objectMapper,
            @Value("${sms.ai.knowledge-ingestion.postgres.url}") String vectorUrl,
            @Value("${sms.ai.knowledge-ingestion.postgres.username}") String vectorUsername,
            @Value("${sms.ai.knowledge-ingestion.postgres.password}") String vectorPassword,
            @Value("${sms.ai.knowledge-ingestion.postgres.table}") String vectorTable,
            @Value("${sms.ai.knowledge-ingestion.default-limit}") int defaultLimit,
            @Value("${sms.ai.knowledge-ingestion.max-limit}") int maxLimit,
            @Value("${sms.ai.knowledge-ingestion.default-index-revision}") int defaultIndexRevision) {
        this.sourceJdbcTemplate = sourceJdbcTemplate;
        this.embeddingService = embeddingService;
        this.objectMapper = objectMapper;
        this.vectorTable = safeIdentifier(vectorTable);
        this.defaultLimit = defaultLimit;
        this.maxLimit = maxLimit;
        this.defaultIndexRevision = defaultIndexRevision;
        DriverManagerDataSource dataSource = new DriverManagerDataSource(vectorUrl, vectorUsername, vectorPassword);
        dataSource.setDriverClassName("org.postgresql.Driver");
        this.vectorJdbcTemplate = new JdbcTemplate(dataSource);
    }

    public IngestionResult ingest(IngestionRequest request) {
        int limit = request == null || request.limit() == null ? defaultLimit : request.limit();
        if (limit <= 0) throw new IllegalArgumentException("limit must be positive");
        limit = Math.min(limit, maxLimit);
        int indexRevision = request == null || request.indexRevision() == null ? defaultIndexRevision : request.indexRevision();
        if (indexRevision <= 0) throw new IllegalArgumentException("indexRevision must be positive");

        List<StagingDetail> details = loadDetails(request == null ? null : request.knowledgeBaseId(),
                request == null ? null : request.documentVersionId(), limit);
        if (details.isEmpty()) return new IngestionResult(0, 0, indexRevision);

        List<List<Float>> embeddings = embeddingService.embedAll(
                details.stream().map(StagingDetail::content).toList());
        int imported = 0;
        for (int i = 0; i < details.size(); i++) {
            StagingDetail detail = details.get(i);
            upsertChunk(detail, embeddings.get(i), indexRevision);
            imported++;
        }
        return new IngestionResult(details.size(), imported, indexRevision);
    }

    private List<StagingDetail> loadDetails(Long knowledgeBaseId, Long documentVersionId, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT d.knowledge_base_id, d.id AS document_id, d.document_no,
                       v.id AS document_version_id, x.chunk_no, x.content, x.metadata
                  FROM ai_knowledge_document_detail x
                  JOIN ai_knowledge_document d ON d.id = x.document_id
                  JOIN ai_knowledge_document_version v
                    ON v.id = x.document_version_id AND v.document_id = d.id
                 WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>();
        if (knowledgeBaseId != null) {
            sql.append(" AND d.knowledge_base_id = ?");
            args.add(knowledgeBaseId);
        }
        if (documentVersionId != null) {
            sql.append(" AND x.document_version_id = ?");
            args.add(documentVersionId);
        }
        sql.append(" ORDER BY x.document_version_id, x.chunk_no LIMIT ?");
        args.add(limit);
        return sourceJdbcTemplate.query(sql.toString(), (rs, rowNum) -> new StagingDetail(
                rs.getLong("knowledge_base_id"),
                rs.getLong("document_id"),
                rs.getString("document_no"),
                rs.getLong("document_version_id"),
                rs.getInt("chunk_no"),
                rs.getString("content"),
                rs.getString("metadata")
        ), args.toArray());
    }

    private void upsertChunk(StagingDetail detail, List<Float> embedding, int indexRevision) {
        if (embedding == null || embedding.isEmpty()) {
            throw new IllegalStateException("Embedding response is empty for chunk " + detail.chunkNo());
        }
        String sql = "INSERT INTO " + vectorTable + " "
                + "(knowledge_base_id, document_id, document_no, document_version_id, index_revision, "
                + "chunk_no, content, metadata, embedding, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS vector), 'ACTIVE') "
                + "ON CONFLICT (knowledge_base_id, document_id, document_version_id, index_revision, chunk_no) "
                + "DO UPDATE SET document_no = EXCLUDED.document_no, content = EXCLUDED.content, "
                + "metadata = EXCLUDED.metadata, embedding = EXCLUDED.embedding, status = 'ACTIVE', "
                + "sys_update_time = CURRENT_TIMESTAMP";
        vectorJdbcTemplate.update(sql,
                detail.knowledgeBaseId(), detail.documentId(), detail.documentNo(), detail.documentVersionId(),
                indexRevision, detail.chunkNo(), detail.content(), metadataJson(detail), vectorLiteral(embedding));
    }

    private String metadataJson(StagingDetail detail) {
        Map<String, Object> metadata = new HashMap<>();
        if (detail.metadata() != null && !detail.metadata().isBlank()) {
            try {
                metadata.putAll(objectMapper.readValue(detail.metadata(), new TypeReference<Map<String, Object>>() { }));
            } catch (Exception exception) {
                throw new IllegalArgumentException("Invalid metadata for document detail " + detail.chunkNo(), exception);
            }
        }
        metadata.putIfAbsent("documentId", detail.documentId());
        metadata.putIfAbsent("documentNo", detail.documentNo());
        metadata.putIfAbsent("documentVersionId", detail.documentVersionId());
        metadata.putIfAbsent("chunkNo", detail.chunkNo());
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize document metadata", exception);
        }
    }

    private String vectorLiteral(List<Float> embedding) {
        StringBuilder value = new StringBuilder("[");
        for (int i = 0; i < embedding.size(); i++) {
            if (i > 0) value.append(',');
            value.append(embedding.get(i));
        }
        return value.append(']').toString();
    }

    private String safeIdentifier(String value) {
        String identifier = value == null ? "" : value.trim();
        if (!identifier.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid PostgreSQL vector table name");
        }
        return identifier;
    }

    public record IngestionRequest(Long knowledgeBaseId, Long documentVersionId,
                                   Integer limit, Integer indexRevision) { }

    public record IngestionResult(int selected, int imported, int indexRevision) { }

    private record StagingDetail(Long knowledgeBaseId, Long documentId, String documentNo,
                                 Long documentVersionId, Integer chunkNo, String content, String metadata) { }
}
