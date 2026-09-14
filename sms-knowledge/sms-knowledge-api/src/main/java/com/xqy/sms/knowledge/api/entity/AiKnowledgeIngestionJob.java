package com.xqy.sms.knowledge.api.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xqy.sms.common.entity.BaseEntity;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("ai_knowledge_ingestion_job")
public class AiKnowledgeIngestionJob extends BaseEntity implements Serializable {
    @TableField("document_version_id") private Long documentVersionId;
    @TableField("index_revision") private String indexRevision;
    @TableField("job_type") private String jobType;
    private String status;
    private Integer progress;
    @TableField("retry_count") private Integer retryCount;
    @TableField("error_message") private String errorMessage;
    @TableField("start_time") private LocalDateTime startTime;
    @TableField("finish_time") private LocalDateTime finishTime;
    public Long getDocumentVersionId() { return documentVersionId; }
    public void setDocumentVersionId(Long documentVersionId) { this.documentVersionId = documentVersionId; }
    public String getIndexRevision() { return indexRevision; }
    public void setIndexRevision(String indexRevision) { this.indexRevision = indexRevision; }
    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getFinishTime() { return finishTime; }
    public void setFinishTime(LocalDateTime finishTime) { this.finishTime = finishTime; }
}
