package com.xqy.sms.ai.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xqy.sms.common.entity.BaseEntity;

import java.time.LocalDateTime;

/** Persistent trace record for one user-facing AI request. */
@TableName("ai_request_log")
public class AiRequestLog extends BaseEntity {

    private String requestId;
    private String userId;
    private String sessionId;
    private String requestType;
    private String question;
    private String modelName;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private Long durationTime;
    private Integer inputTokenCount;
    private Integer outputTokenCount;
    private Integer totalTokenCount;
    private boolean success;
    private String errorCode;
    private String errorMessage;

    @TableField(exist = false)
    private transient String traceContext;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getFinishTime() { return finishTime; }
    public void setFinishTime(LocalDateTime finishTime) { this.finishTime = finishTime; }
    public Long getDurationTime() { return durationTime; }
    public void setDurationTime(Long durationTime) { this.durationTime = durationTime; }
    public Integer getInputTokenCount() { return inputTokenCount; }
    public void setInputTokenCount(Integer inputTokenCount) { this.inputTokenCount = inputTokenCount; }
    public Integer getOutputTokenCount() { return outputTokenCount; }
    public void setOutputTokenCount(Integer outputTokenCount) { this.outputTokenCount = outputTokenCount; }
    public Integer getTotalTokenCount() { return totalTokenCount; }
    public void setTotalTokenCount(Integer totalTokenCount) { this.totalTokenCount = totalTokenCount; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getTraceContext() { return traceContext; }
    public void setTraceContext(String traceContext) { this.traceContext = traceContext; }
}
