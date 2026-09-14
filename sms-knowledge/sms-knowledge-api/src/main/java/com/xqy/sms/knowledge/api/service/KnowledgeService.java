package com.xqy.sms.knowledge.api.service;

import com.xqy.sms.knowledge.api.entity.AiKnowledgeBase;
import com.xqy.sms.knowledge.api.entity.AiKnowledgeDocument;
import com.xqy.sms.knowledge.api.entity.AiKnowledgeDocumentVersion;
import com.xqy.sms.knowledge.api.entity.AiKnowledgeIngestionJob;
import com.xqy.sms.knowledge.api.entity.AiknowledgeChunk;
import com.xqy.sms.knowledge.api.model.KnowledgeVectorQuery;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface KnowledgeService {
    boolean saveKnowledgeBase(AiKnowledgeBase value);
    boolean updateKnowledgeBase(AiKnowledgeBase value);
    boolean deleteKnowledgeBase(Long id);
    AiKnowledgeBase getKnowledgeBaseById(Long id);
    List<AiKnowledgeBase> listKnowledgeBases();

    boolean saveDocument(AiKnowledgeDocument value);
    boolean updateDocument(AiKnowledgeDocument value);
    boolean deleteDocument(Long id);
    AiKnowledgeDocument getDocumentById(Long id);
    List<AiKnowledgeDocument> listDocuments(Long knowledgeBaseId);

    boolean saveDocumentVersion(AiKnowledgeDocumentVersion value);
    boolean updateDocumentVersion(AiKnowledgeDocumentVersion value);
    AiKnowledgeDocumentVersion getDocumentVersionById(Long id);
    List<AiKnowledgeDocumentVersion> listDocumentVersions(Long documentId);

    boolean saveIngestionJob(AiKnowledgeIngestionJob value);
    boolean updateIngestionJob(AiKnowledgeIngestionJob value);
    AiKnowledgeIngestionJob getIngestionJobById(Long id);
    List<AiKnowledgeIngestionJob> listIngestionJobs(Long documentVersionId);

    /** Performs a top-K vector search in the active Milvus index. */
    List<AiknowledgeChunk> queryVector(KnowledgeVectorQuery query);

    default List<AiknowledgeChunk> queryKnowledge(KnowledgeVectorQuery query) {
        return queryVector(query);
    }
}
