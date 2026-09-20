package com.xqy.sms.knowledge.api.entity;

import com.xqy.sms.common.entity.BaseEntity;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** Vector record stored in Milvus; its metadata is denormalized for filtering. */
public class AiknowledgeChunk extends BaseEntity implements Serializable {
    private String tenantId;
    private Long knowledgeBaseId;
    private Long documentId;
    private String documentNo;
    private Long documentVersionId;
    private String indexRevision;
    private Integer chunkNo;
    private String content;
    private Map<String, Object> metadata;
    private List<Float> embedding;
    private String status;
    private Double score;
    public Long getKnowledgeBaseId() { return knowledgeBaseId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public void setKnowledgeBaseId(Long knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getDocumentNo() { return documentNo; }
    public void setDocumentNo(String documentNo) { this.documentNo = documentNo; }
    public Long getDocumentVersionId() { return documentVersionId; }
    public void setDocumentVersionId(Long documentVersionId) { this.documentVersionId = documentVersionId; }
    public String getIndexRevision() { return indexRevision; }
    public void setIndexRevision(String indexRevision) { this.indexRevision = indexRevision; }
    public Integer getChunkNo() { return chunkNo; }
    public void setChunkNo(Integer chunkNo) { this.chunkNo = chunkNo; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public List<Float> getEmbedding() { return embedding; }
    public void setEmbedding(List<Float> embedding) { this.embedding = embedding; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
}
