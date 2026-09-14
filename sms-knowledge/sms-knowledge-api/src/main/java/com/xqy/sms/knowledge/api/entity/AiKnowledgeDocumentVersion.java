package com.xqy.sms.knowledge.api.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xqy.sms.common.entity.BaseEntity;
import java.io.Serializable;

@TableName("ai_knowledge_document_version")
public class AiKnowledgeDocumentVersion extends BaseEntity implements Serializable {
    @TableField("document_id") private Long documentId;
    private String version;
    @TableField("source_uri") private String sourceUri;
    @TableField("source_version") private String sourceVersion;
    @TableField("mime_type") private String mimeType;
    @TableField("content_hash") private String contentHash;
    @TableField("active_index_revision") private String activeIndexRevision;
    @TableField("index_status") private String indexStatus;
    private Integer chunkCount;
    @TableField("error_message") private String errorMessage;
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getSourceUri() { return sourceUri; }
    public void setSourceUri(String sourceUri) { this.sourceUri = sourceUri; }
    public String getSourceVersion() { return sourceVersion; }
    public void setSourceVersion(String sourceVersion) { this.sourceVersion = sourceVersion; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public String getActiveIndexRevision() { return activeIndexRevision; }
    public void setActiveIndexRevision(String activeIndexRevision) { this.activeIndexRevision = activeIndexRevision; }
    public String getIndexStatus() { return indexStatus; }
    public void setIndexStatus(String indexStatus) { this.indexStatus = indexStatus; }
    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
