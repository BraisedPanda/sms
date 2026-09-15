package com.xqy.sms.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Persistent lifecycle record for one planned AI conversation run. */
@TableName("ai_task_run")
public class AiTaskRun extends BaseEntity implements Serializable {
    @TableField("run_id")
    private String runId;
    @TableField("request_id")
    private String requestId;
    @TableField("user_id")
    private String userId;
    @TableField("session_id")
    private String sessionId;
    @TableField("run_type")
    private String runType;
    private String status;
    private String question;
    @TableField("model_alias")
    private String modelAlias;
    @TableField("plan_json")
    private String planJson;
    @TableField("idempotency_key")
    private String idempotencyKey;
    @TableField("current_step_no")
    private Integer currentStepNo;
    @TableField("cancel_request")
    private Boolean cancelRequest;
    @TableField("cancel_request_time")
    private LocalDateTime cancelRequestTime;
    @TableField("start_time")
    private LocalDateTime startTime;
    @TableField("finish_time")
    private LocalDateTime finishTime;
    @TableField("error_code")
    private String errorCode;
    @TableField("error_message")
    private String errorMessage;

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    /** Compatibility alias for callers using the database-style runID spelling. */
    public String getRunID() { return runId; }
    public void setRunID(String runID) { this.runId = runID; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getRunType() { return runType; }
    public void setRunType(String runType) { this.runType = runType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getModelAlias() { return modelAlias; }
    public void setModelAlias(String modelAlias) { this.modelAlias = modelAlias; }
    public String getPlanJson() { return planJson; }
    public void setPlanJson(String planJson) { this.planJson = planJson; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getIdpotencyKey() { return idempotencyKey; }
    public void setIdpotencyKey(String idpotencyKey) { this.idempotencyKey = idpotencyKey; }
    public Integer getCurrentStepNo() { return currentStepNo; }
    public void setCurrentStepNo(Integer currentStepNo) { this.currentStepNo = currentStepNo; }
    public Boolean getCancelRequest() { return cancelRequest; }
    public void setCancelRequest(Boolean cancelRequest) { this.cancelRequest = cancelRequest; }
    public LocalDateTime getCancelRequestTime() { return cancelRequestTime; }
    public void setCancelRequestTime(LocalDateTime cancelRequestTime) { this.cancelRequestTime = cancelRequestTime; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getFinishTime() { return finishTime; }
    public void setFinishTime(LocalDateTime finishTime) { this.finishTime = finishTime; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
