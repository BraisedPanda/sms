package com.xqy.sms.knowledge.api.service;

import com.xqy.sms.knowledge.api.entity.AiKnowledgeBase;
import com.xqy.sms.knowledge.api.entity.AiKnowledgeDocument;
import com.xqy.sms.knowledge.api.entity.AiKnowledgeDocumentVersion;
import com.xqy.sms.knowledge.api.entity.AiKnowledgeIngestionJob;
import com.xqy.sms.knowledge.api.entity.AiknowledgeChunk;
import com.xqy.sms.knowledge.api.model.KnowledgeVectorQuery;
import org.springframework.stereotype.Service;
import com.xqy.sms.common.security.rpc.InternalCallContext;

import java.util.List;

@Service
public interface KnowledgeService {
    boolean saveKnowledgeBase(AiKnowledgeBase value, InternalCallContext context);
    boolean updateKnowledgeBase(AiKnowledgeBase value, InternalCallContext context);
    boolean deleteKnowledgeBase(Long id, InternalCallContext context);
    AiKnowledgeBase getKnowledgeBaseById(Long id, InternalCallContext context);
    List<AiKnowledgeBase> listKnowledgeBases(InternalCallContext context);

    boolean saveDocument(AiKnowledgeDocument value, InternalCallContext context);
    boolean updateDocument(AiKnowledgeDocument value, InternalCallContext context);
    boolean deleteDocument(Long id, InternalCallContext context);
    AiKnowledgeDocument getDocumentById(Long id, InternalCallContext context);
    List<AiKnowledgeDocument> listDocuments(Long knowledgeBaseId, InternalCallContext context);

    boolean saveDocumentVersion(AiKnowledgeDocumentVersion value, InternalCallContext context);
    boolean updateDocumentVersion(AiKnowledgeDocumentVersion value, InternalCallContext context);
    AiKnowledgeDocumentVersion getDocumentVersionById(Long id, InternalCallContext context);
    List<AiKnowledgeDocumentVersion> listDocumentVersions(Long documentId, InternalCallContext context);

    boolean saveIngestionJob(AiKnowledgeIngestionJob value, InternalCallContext context);
    boolean updateIngestionJob(AiKnowledgeIngestionJob value, InternalCallContext context);
    AiKnowledgeIngestionJob getIngestionJobById(Long id, InternalCallContext context);
    List<AiKnowledgeIngestionJob> listIngestionJobs(Long documentVersionId, InternalCallContext context);

    /** Performs a top-K vector search in the active Milvus index. */
    List<AiknowledgeChunk> queryVector(KnowledgeVectorQuery query);

    default List<AiknowledgeChunk> queryKnowledge(KnowledgeVectorQuery query) {
        return queryVector(query);
    }
}
