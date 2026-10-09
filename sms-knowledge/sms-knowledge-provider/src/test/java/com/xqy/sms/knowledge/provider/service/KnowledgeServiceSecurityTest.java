package com.xqy.sms.knowledge.provider.service;

import com.xqy.sms.common.security.rpc.InternalCallContext;
import com.xqy.sms.common.security.rpc.InternalCallSigner;
import com.xqy.sms.knowledge.api.model.KnowledgeVectorQuery;
import com.xqy.sms.knowledge.api.entity.AiknowledgeChunk;
import com.xqy.sms.knowledge.provider.mapper.AiKnowledgeBaseMapper;
import com.xqy.sms.knowledge.provider.mapper.AiKnowledgeDocumentMapper;
import com.xqy.sms.knowledge.provider.mapper.AiKnowledgeDocumentVersionMapper;
import com.xqy.sms.knowledge.provider.mapper.AiKnowledgeIngestionJobMapper;
import com.xqy.sms.knowledge.provider.milvus.MilvusVectorStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;

class KnowledgeServiceSecurityTest {
    private static final String SECRET = "01234567890123456789012345678901";

    @Test
    void vectorQueryRequiresSignedMatchingTenant() {
        MilvusVectorStore vectorStore = mock(MilvusVectorStore.class);
        KnowledgeServiceImpl service = new KnowledgeServiceImpl(mock(AiKnowledgeBaseMapper.class),
                mock(AiKnowledgeDocumentMapper.class), mock(AiKnowledgeDocumentVersionMapper.class),
                mock(AiKnowledgeIngestionJobMapper.class), vectorStore, SECRET);
        InternalCallSigner signer = new InternalCallSigner(SECRET);
        JdbcTemplate metadata = mock(JdbcTemplate.class);
        when(metadata.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(Map.of("id", 10L, "active_index_revision", "1")));
        ReflectionTestUtils.setField(service, "metadata", metadata);
        KnowledgeVectorQuery valid = new KnowledgeVectorQuery();
        valid.setTenantId("tenant-1");
        valid.setEmbedding(List.of(1.0f));
        valid.setCallerContext(signer.sign("sms-ai-provider", "tenant-1", 1L, 2L, "request-1"));
        service.queryVector(valid);
        verify(vectorStore).search(valid);

        KnowledgeVectorQuery forged = new KnowledgeVectorQuery();
        forged.setTenantId("tenant-2");
        forged.setCallerContext(valid.getCallerContext());
        assertThrows(InternalCallSigner.InternalCallAuthenticationException.class,
                () -> service.queryVector(forged));
    }

    @Test
    void retrievalRejectsStaleUnscopedAndInactiveVectorRecords() {
        MilvusVectorStore vectorStore = mock(MilvusVectorStore.class);
        KnowledgeServiceImpl service = new KnowledgeServiceImpl(mock(AiKnowledgeBaseMapper.class),
                mock(AiKnowledgeDocumentMapper.class), mock(AiKnowledgeDocumentVersionMapper.class),
                mock(AiKnowledgeIngestionJobMapper.class), vectorStore, SECRET);
        JdbcTemplate metadata = mock(JdbcTemplate.class);
        when(metadata.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of(Map.of("id", 10L, "active_index_revision", "2")));
        ReflectionTestUtils.setField(service, "metadata", metadata);
        KnowledgeVectorQuery query = new KnowledgeVectorQuery();
        query.setTenantId("tenant-1");
        query.setCallerContext(new InternalCallSigner(SECRET).sign("sms-ai-provider", "tenant-1", 1L, 2L, "request-1"));
        query.setActiveVersions(Map.of(99L, "1")); // A caller cannot override authoritative metadata.
        query.setSimilarityThreshold(0.5);
        AiknowledgeChunk valid = chunk("tenant-1", 10L, "2", "ACTIVE", 0.8);
        when(vectorStore.search(query)).thenReturn(java.util.Arrays.asList(valid,
                chunk("tenant-2", 10L, "2", "ACTIVE", 0.9),
                chunk("tenant-1", 10L, "1", "ACTIVE", 0.9),
                chunk("tenant-1", 99L, "1", "ACTIVE", 0.9),
                chunk("tenant-1", 99L, null, "ACTIVE", 0.9),
                chunk("tenant-1", 10L, "2", "INACTIVE", 0.9),
                chunk("tenant-1", 10L, "2", "ACTIVE", 0.1), null));
        assertEquals(List.of(valid), service.queryVector(query));
        assertEquals(Map.of(10L, "2"), query.getActiveVersions());
    }

    private static AiknowledgeChunk chunk(String tenant, Long version, String revision, String status, double score) {
        AiknowledgeChunk chunk = new AiknowledgeChunk();
        chunk.setTenantId(tenant); chunk.setDocumentVersionId(version); chunk.setIndexRevision(revision);
        chunk.setStatus(status); chunk.setScore(score);
        return chunk;
    }
}
