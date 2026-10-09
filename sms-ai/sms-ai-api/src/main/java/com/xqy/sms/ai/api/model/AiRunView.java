package com.xqy.sms.ai.api.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class AiRunView implements Serializable {
    private static final long serialVersionUID = 1L;

    private String runId;
    private String status;
    private Integer currentStepNo;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;

    public AiRunView() { }

    public AiRunView(String runId, String status, Integer currentStepNo, LocalDateTime startTime, LocalDateTime finishTime) {
        this.runId = runId;
        this.status = status;
        this.currentStepNo = currentStepNo;
        this.startTime = startTime;
        this.finishTime = finishTime;
    }

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getCurrentStepNo() { return currentStepNo; }
    public void setCurrentStepNo(Integer currentStepNo) { this.currentStepNo = currentStepNo; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getFinishTime() { return finishTime; }
    public void setFinishTime(LocalDateTime finishTime) { this.finishTime = finishTime; }

    /** Compatibility accessors retained for existing RPC callers. */
    public String runId() { return runId; }
    public String status() { return status; }
    public Integer currentStepNo() { return currentStepNo; }
    public LocalDateTime startTime() { return startTime; }
    public LocalDateTime finishTime() { return finishTime; }
}
