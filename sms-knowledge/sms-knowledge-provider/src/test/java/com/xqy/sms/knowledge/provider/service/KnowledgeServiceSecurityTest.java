package com.xqy.sms.knowledge.provider.service;

import com.xqy.sms.common.security.rpc.InternalCallContext;
import com.xqy.sms.common.security.rpc.InternalCallSigner;
import com.xqy.sms.knowledge.api.model.KnowledgeVectorQuery;
import com.xqy.sms.knowledge.provider.mapper.AiKnowledgeBaseMapper;
import com.xqy.sms.knowledge.provider.mapper.AiKnowledgeDocumentMapper;
import com.xqy.sms.knowledge.provider.mapper.AiKnowledgeDocumentVersionMapper;
import com.xqy.sms.knowledge.provider.mapper.AiKnowledgeIngestionJobMapper;
import com.xqy.sms.knowledge.provider.milvus.MilvusVectorStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class KnowledgeServiceSecurityTest {
    private static final String SECRET = "01234567890123456789012345678901";

    @Test
    void vectorQueryRequiresSignedMatchingTenant() {
        MilvusVectorStore vectorStore = mock(MilvusVectorStore.class);
        KnowledgeServiceImpl service = new KnowledgeServiceImpl(mock(AiKnowledgeBaseMapper.class),
                mock(AiKnowledgeDocumentMapper.class), mock(AiKnowledgeDocumentVersionMapper.class),
                mock(AiKnowledgeIngestionJobMapper.class), vectorStore, SECRET);
        InternalCallSigner signer = new InternalCallSigner(SECRET);
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
}
