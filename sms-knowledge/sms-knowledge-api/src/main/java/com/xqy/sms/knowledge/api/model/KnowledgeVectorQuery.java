package com.xqy.sms.knowledge.api.model;

import com.xqy.sms.common.security.rpc.InternalCallContext;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Query sent from sms-ai to the knowledge provider.
 */
public class KnowledgeVectorQuery implements Serializable {
    private Long knowledgeBaseId;
    private String queryText;
    private List<Float> embedding = new ArrayList<>();
    private Integer topK;
    private Double similarityThreshold;
    private Map<String, Object> filter = new HashMap<>();
    private String collectionName;
    private String tenantId;
    private InternalCallContext callerContext;

    public Long getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public void setKnowledgeBaseId(Long knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId;
    }

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public List<Float> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<Float> embedding) {
        this.embedding = embedding;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    public Double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(Double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public Map<String, Object> getFilter() {
        return filter;
    }

    public void setFilter(Map<String, Object> filter) {
        this.filter = filter;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public InternalCallContext getCallerContext() { return callerContext; }
    public void setCallerContext(InternalCallContext callerContext) { this.callerContext = callerContext; }
}
