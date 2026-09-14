package com.xqy.sms.knowledge.provider.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xqy.sms.knowledge.api.entity.*;
import com.xqy.sms.knowledge.api.model.KnowledgeVectorQuery;
import com.xqy.sms.knowledge.api.service.KnowledgeService;
import com.xqy.sms.knowledge.provider.mapper.AiKnowledgeBaseMapper;
import com.xqy.sms.knowledge.provider.mapper.AiKnowledgeDocumentMapper;
import com.xqy.sms.knowledge.provider.mapper.AiKnowledgeDocumentVersionMapper;
import com.xqy.sms.knowledge.provider.mapper.AiKnowledgeIngestionJobMapper;
import com.xqy.sms.knowledge.provider.milvus.MilvusVectorStore;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@DubboService
@Transactional(rollbackFor = Exception.class)
public class KnowledgeServiceImpl implements KnowledgeService {
    private final AiKnowledgeBaseMapper baseMapper;
    private final AiKnowledgeDocumentMapper documentMapper;
    private final AiKnowledgeDocumentVersionMapper versionMapper;
    private final AiKnowledgeIngestionJobMapper jobMapper;
    private final MilvusVectorStore vectorStore;

    public KnowledgeServiceImpl(AiKnowledgeBaseMapper baseMapper, AiKnowledgeDocumentMapper documentMapper,
                                AiKnowledgeDocumentVersionMapper versionMapper, AiKnowledgeIngestionJobMapper jobMapper,
                                MilvusVectorStore vectorStore) {
        this.baseMapper = baseMapper;
        this.documentMapper = documentMapper;
        this.versionMapper = versionMapper;
        this.jobMapper = jobMapper;
        this.vectorStore = vectorStore;
    }

    public boolean saveKnowledgeBase(AiKnowledgeBase value) {
        return value != null && baseMapper.insert(value) > 0;
    }

    public boolean updateKnowledgeBase(AiKnowledgeBase value) {
        return value != null && baseMapper.updateById(value) > 0;
    }

    public boolean deleteKnowledgeBase(Long id) {
        return id != null && baseMapper.deleteById(id) > 0;
    }

    public AiKnowledgeBase getKnowledgeBaseById(Long id) {
        return id == null ? null : baseMapper.selectById(id);
    }

    public List<AiKnowledgeBase> listKnowledgeBases() {
        return baseMapper.selectList(null);
    }

    public boolean saveDocument(AiKnowledgeDocument value) {
        return value != null && documentMapper.insert(value) > 0;
    }

    public boolean updateDocument(AiKnowledgeDocument value) {
        return value != null && documentMapper.updateById(value) > 0;
    }

    public boolean deleteDocument(Long id) {
        return id != null && documentMapper.deleteById(id) > 0;
    }

    public AiKnowledgeDocument getDocumentById(Long id) {
        return id == null ? null : documentMapper.selectById(id);
    }

    public List<AiKnowledgeDocument> listDocuments(Long knowledgeBaseId) {
        return documentMapper.selectList(new LambdaQueryWrapper<AiKnowledgeDocument>().eq(knowledgeBaseId != null, AiKnowledgeDocument::getKnowledgeBaseId, knowledgeBaseId));
    }

    public boolean saveDocumentVersion(AiKnowledgeDocumentVersion value) {
        return value != null && versionMapper.insert(value) > 0;
    }

    public boolean updateDocumentVersion(AiKnowledgeDocumentVersion value) {
        return value != null && versionMapper.updateById(value) > 0;
    }

    public AiKnowledgeDocumentVersion getDocumentVersionById(Long id) {
        return id == null ? null : versionMapper.selectById(id);
    }

    public List<AiKnowledgeDocumentVersion> listDocumentVersions(Long documentId) {
        return versionMapper.selectList(new LambdaQueryWrapper<AiKnowledgeDocumentVersion>().eq(documentId != null, AiKnowledgeDocumentVersion::getDocumentId, documentId));
    }

    public boolean saveIngestionJob(AiKnowledgeIngestionJob value) {
        return value != null && jobMapper.insert(value) > 0;
    }

    public boolean updateIngestionJob(AiKnowledgeIngestionJob value) {
        return value != null && jobMapper.updateById(value) > 0;
    }

    public AiKnowledgeIngestionJob getIngestionJobById(Long id) {
        return id == null ? null : jobMapper.selectById(id);
    }

    public List<AiKnowledgeIngestionJob> listIngestionJobs(Long documentVersionId) {
        return jobMapper.selectList(new LambdaQueryWrapper<AiKnowledgeIngestionJob>().eq(documentVersionId != null, AiKnowledgeIngestionJob::getDocumentVersionId, documentVersionId));
    }

    @Override
    public List<AiknowledgeChunk> queryVector(KnowledgeVectorQuery query) {
        if (query == null) return Collections.emptyList();
        KnowledgeVectorQuery effective = query;
        if (query.getKnowledgeBaseId() != null) {
            AiKnowledgeBase base = baseMapper.selectById(query.getKnowledgeBaseId());
            if (base != null) {
                if (Boolean.FALSE.equals(base.getEnabled())) return Collections.emptyList();
                if (effective.getTopK() == null || effective.getTopK() <= 0) effective.setTopK(base.getTopk());
                if (effective.getSimilarityThreshold() == null)
                    effective.setSimilarityThreshold(base.getSimilarityThreshold());
            }
        }
        List<AiknowledgeChunk> chunks = vectorStore.search(effective);
        if (effective.getSimilarityThreshold() == null || chunks == null) return chunks;
        return chunks.stream()
                .filter(chunk -> chunk.getScore() == null || chunk.getScore() >= effective.getSimilarityThreshold())
                .toList();
    }
}
