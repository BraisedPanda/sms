package com.xqy.sms.ai.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xqy.sms.common.entity.BaseEntity;

import java.time.LocalDateTime;

/** Persistent trace record for one registered AI tool execution. */
@TableName("ai_tool_execute_log")
public class AiToolExecuteLog extends BaseEntity {

    private String toolExecuteId;
    private String requestId;
    private String domain;
    private String toolName;
    private String question;
    private String modelName;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private Long durationTime;
    private Integer inputTokenCount;
    private Integer outputTokenCount;
    private Integer totalTokenCount;
    private Integer resultCount;
    private boolean success;
    private String errorCode;
    private String errorMessage;

    @TableField(exist = false)
    private transient String traceContext;

    public String getToolExecuteId() { return toolExecuteId; }
    public void setToolExecuteId(String toolExecuteId) { this.toolExecuteId = toolExecuteId; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
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
    public Integer getResultCount() { return resultCount; }
    public void setResultCount(Integer resultCount) { this.resultCount = resultCount; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getTraceContext() { return traceContext; }
    public void setTraceContext(String traceContext) { this.traceContext = traceContext; }
}
