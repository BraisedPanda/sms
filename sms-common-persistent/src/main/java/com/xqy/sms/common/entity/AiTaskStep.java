package com.xqy.sms.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Persistent lifecycle record for one plan or tool step. */
@TableName("ai_task_step")
public class AiTaskStep extends BaseEntity implements Serializable {
    @TableField("step_id")
    private String stepId;
    @TableField("task_run_id")
    private String taskRunId;
    @TableField("step_no")
    private Integer stepNo;
    @TableField("step_type")
    private String stepType;
    private String domain;
    @TableField("tool_name")
    private String toolName;
    private String status;
    @TableField("input_json")
    private String inputJson;
    @TableField("output_json")
    private String outputJson;
    private Integer attempt;
    @TableField("max_attempt")
    private Integer maxAttempt;
    @TableField("timeout_ms")
    private Long timeoutMs;
    @TableField("next_retry_time")
    private LocalDateTime nextRetryTime;
    @TableField("idempotency_key")
    private String idempotencyKey;
    @TableField("lease_expire_time")
    private LocalDateTime leaseExpireTime;
    @TableField("start_time")
    private LocalDateTime startTime;
    @TableField("finish_time")
    private LocalDateTime finishTime;
    @TableField("error_code")
    private String errorCode;
    @TableField("error_message")
    private String errorMessage;

    public String getStepId() { return stepId; }
    public void setStepId(String stepId) { this.stepId = stepId; }
    public String getTaskRunId() { return taskRunId; }
    public void setTaskRunId(String taskRunId) { this.taskRunId = taskRunId; }
    public Integer getStepNo() { return stepNo; }
    public void setStepNo(Integer stepNo) { this.stepNo = stepNo; }
    public String getStepType() { return stepType; }
    public void setStepType(String stepType) { this.stepType = stepType; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getInputJson() { return inputJson; }
    public void setInputJson(String inputJson) { this.inputJson = inputJson; }
    public String getOutputJson() { return outputJson; }
    public void setOutputJson(String outputJson) { this.outputJson = outputJson; }
    public Integer getAttempt() { return attempt; }
    public void setAttempt(Integer attempt) { this.attempt = attempt; }
    public Integer getMaxAttempt() { return maxAttempt; }
    public void setMaxAttempt(Integer maxAttempt) { this.maxAttempt = maxAttempt; }
    public Long getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Long timeoutMs) { this.timeoutMs = timeoutMs; }
    public LocalDateTime getNextRetryTime() { return nextRetryTime; }
    public void setNextRetryTime(LocalDateTime nextRetryTime) { this.nextRetryTime = nextRetryTime; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getIdpotencyKey() { return idempotencyKey; }
    public void setIdpotencyKey(String idpotencyKey) { this.idempotencyKey = idpotencyKey; }
    public LocalDateTime getLeaseExpireTime() { return leaseExpireTime; }
    public void setLeaseExpireTime(LocalDateTime leaseExpireTime) { this.leaseExpireTime = leaseExpireTime; }
    public LocalDateTime getLeasExpireTime() { return leaseExpireTime; }
    public void setLeasExpireTime(LocalDateTime leasExpireTime) { this.leaseExpireTime = leasExpireTime; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getFinishTime() { return finishTime; }
    public void setFinishTime(LocalDateTime finishTime) { this.finishTime = finishTime; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
