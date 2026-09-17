package com.xqy.sms.ai.domain.model;

import java.util.ArrayList;
import java.util.List;

/** Paging and filtering criteria supplied to an AI tool. */
public class QueryCriteria {

    private int limit;
    private QueryFilterNode filter;
    private String queryText;
    private List<Float> embedding = new ArrayList<>();
    private Integer topK;
    private Double similarityThreshold;
    private Long knowledgeBaseId;

    public QueryCriteria() {
    }

    public QueryCriteria(int limit, QueryFilterNode filter) {
        this.limit = limit;
        this.filter = filter;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public QueryFilterNode getFilter() {
        return filter;
    }

    public void setFilter(QueryFilterNode filter) {
        this.filter = filter;
    }

    public String getQueryText() { return queryText; }
    public void setQueryText(String queryText) { this.queryText = queryText; }
    public List<Float> getEmbedding() { return embedding; }
    public void setEmbedding(List<Float> embedding) { this.embedding = embedding; }
    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }
    public Double getSimilarityThreshold() { return similarityThreshold; }
    public void setSimilarityThreshold(Double similarityThreshold) { this.similarityThreshold = similarityThreshold; }
    public Long getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(Long knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
}
