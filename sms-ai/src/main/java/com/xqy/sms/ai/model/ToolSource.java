package com.xqy.sms.ai.model;

import java.util.Map;

/**
 * Source trace for content retrieved by a RAG-backed tool.
 *
 * <p>The document and chunk numbers identify the exact source fragment. The
 * remaining fields are optional display and retrieval metadata and can be
 * extended through {@link #metadata} when a source backend has extra fields.</p>
 */
public class ToolSource {

    private String documentNo;
    private String chunkNo;
    private String documentName;
    private String chunkTitle;
    private String content;
    private String sourceType;
    private String sourceUrl;
    private Double score;
    private Integer rank;
    private Map<String, Object> metadata;

    public ToolSource() {
    }

    public ToolSource(String documentNo, String chunkNo) {
        this.documentNo = documentNo;
        this.chunkNo = chunkNo;
    }

    public String getDocumentNo() {
        return documentNo;
    }

    public void setDocumentNo(String documentNo) {
        this.documentNo = documentNo;
    }

    public String getChunkNo() {
        return chunkNo;
    }

    public void setChunkNo(String chunkNo) {
        this.chunkNo = chunkNo;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getChunkTitle() {
        return chunkTitle;
    }

    public void setChunkTitle(String chunkTitle) {
        this.chunkTitle = chunkTitle;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
