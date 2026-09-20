package com.xqy.sms.ai.application.service.run;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xqy.sms.ai.infrastructure.persistence.mapper.AiTaskRunMapper;
import com.xqy.sms.ai.infrastructure.persistence.mapper.AiTaskStepMapper;
import com.xqy.sms.ai.domain.model.AiTaskRunStatus;
import com.xqy.sms.ai.api.service.AiRunAccessDeniedException;
import com.xqy.sms.ai.api.service.AiRunNotFoundException;
import com.xqy.sms.common.entity.AiTaskRun;
import com.xqy.sms.common.entity.AiTaskStep;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Owns durable run/step creation, transitions, cancellation, and recovery lookup. */
@Service
public class AiTaskRunService {
    private final AiTaskRunMapper runMapper;
    private final AiTaskStepMapper stepMapper;

    public AiTaskRunService(AiTaskRunMapper runMapper, AiTaskStepMapper stepMapper) {
        this.runMapper = runMapper;
        this.stepMapper = stepMapper;
    }

    public AiTaskRun createOrReuse(String requestId, String tenantId, String userId, String sessionId, String question,
                                   String modelAlias, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            AiTaskRun existing = findByIdempotencyKey(tenantId, idempotencyKey);
            if (existing != null) {
                if (java.util.Objects.equals(existing.getTenantId(), tenantId)
                        && java.util.Objects.equals(existing.getUserId(), userId)
                        && java.util.Objects.equals(existing.getSessionId(), sessionId)) return existing;
                throw new AiRunAccessDeniedException();
            }
        }
        AiTaskRun run = new AiTaskRun();
        run.setRunId(UUID.randomUUID().toString());
        run.setRequestId(requestId);
        run.setTenantId(requiredTenant(tenantId));
        run.setUserId(userId);
        run.setSessionId(sessionId);
        run.setRunType("CHAT");
        run.setStatus(AiTaskRunStatus.PENDING);
        run.setQuestion(question);
        run.setModelAlias(modelAlias);
        run.setIdempotencyKey(blankToNull(idempotencyKey));
        run.setCurrentStepNo(0);
        run.setCancelRequest(false);
        run.setStartTime(LocalDateTime.now());
        try {
            runMapper.insert(run);
            return run;
        } catch (DuplicateKeyException exception) {
            AiTaskRun existing = findByIdempotencyKey(tenantId, idempotencyKey);
            if (existing != null) return existing;
            throw exception;
        }
    }

    public AiTaskRun requireRun(String runId) {
        AiTaskRun run = findByRunId(runId);
        if (run == null) throw new AiRunNotFoundException(runId);
        return run;
    }

    public AiTaskRun findByRunId(String runId) {
        return runId == null || runId.isBlank() ? null : runMapper.selectOne(
                new LambdaQueryWrapper<AiTaskRun>().eq(AiTaskRun::getRunId, runId));
    }

    public void savePlan(String runId, String planJson) {
        updateRun(runId, AiTaskRunStatus.RUNNING, null, null, run -> run.setPlanJson(planJson));
    }

    /** Claims a newly created run so only one request starts its workflow. */
    public boolean claimPendingRun(String runId) {
        return runMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AiTaskRun>()
                .eq(AiTaskRun::getRunId, runId)
                .eq(AiTaskRun::getStatus, AiTaskRunStatus.PENDING)
                .set(AiTaskRun::getStatus, AiTaskRunStatus.PLANNING)) > 0;
    }

    public void setRunStatus(String runId, String status) {
        updateRun(runId, status, null, null, run -> { });
    }

    public AiTaskStep createStep(String runId, int stepNo, String stepType, String domain,
                                 String toolName, String inputJson, int maxAttempt, long timeoutMs) {
        AiTaskStep step = new AiTaskStep();
        step.setStepId(UUID.randomUUID().toString());
        step.setTaskRunId(runId);
        step.setStepNo(stepNo);
        step.setStepType(stepType);
        step.setDomain(domain);
        step.setToolName(toolName);
        step.setStatus(AiTaskRunStatus.PENDING);
        step.setInputJson(inputJson);
        step.setAttempt(0);
        step.setMaxAttempt(maxAttempt);
        step.setTimeoutMs(timeoutMs);
        step.setIdempotencyKey(runId + ":" + stepNo);
        stepMapper.insert(step);
        setCurrentStep(runId, stepNo);
        return step;
    }

    public void startStep(AiTaskStep step, int attempt) {
        LocalDateTime now = LocalDateTime.now();
        step.setStatus(AiTaskRunStatus.RUNNING);
        step.setAttempt(attempt);
        step.setStartTime(now);
        step.setLeaseExpireTime(now.plusNanos(step.getTimeoutMs() * 1_000_000L));
        step.setNextRetryTime(null);
        step.setErrorCode(null);
        step.setErrorMessage(null);
        stepMapper.updateById(step);
    }

    public void succeedStep(AiTaskStep step, String outputJson) {
        step.setStatus(AiTaskRunStatus.SUCCEEDED);
        step.setOutputJson(outputJson);
        step.setFinishTime(LocalDateTime.now());
        step.setLeaseExpireTime(null);
        step.setNextRetryTime(null);
        stepMapper.updateById(step);
    }

    public void retryStep(AiTaskStep step, String errorCode, String errorMessage, LocalDateTime retryTime) {
        step.setStatus(AiTaskRunStatus.RETRY_WAIT);
        step.setErrorCode(errorCode);
        step.setErrorMessage(errorMessage);
        step.setNextRetryTime(retryTime);
        step.setLeaseExpireTime(null);
        stepMapper.updateById(step);
    }

    public void failStep(AiTaskStep step, String errorCode, String errorMessage) {
        step.setStatus(AiTaskRunStatus.FAILED);
        step.setErrorCode(errorCode);
        step.setErrorMessage(errorMessage);
        step.setFinishTime(LocalDateTime.now());
        step.setLeaseExpireTime(null);
        stepMapper.updateById(step);
    }

    public void completeRun(String runId) {
        updateRun(runId, AiTaskRunStatus.SUCCEEDED, null, null, run -> { });
    }

    public void failRun(String runId, Throwable error) {
        String code = error == null ? "RUN_FAILED" : error.getClass().getSimpleName();
        String message = error == null ? null : error.getMessage();
        updateRun(runId, AiTaskRunStatus.FAILED, code, message, run -> { });
    }

    public void cancel(String runId, String tenantId, String userId) {
        AiTaskRun run = requireOwnedRun(runId, tenantId, userId);
        if (AiTaskRunStatus.isTerminal(run.getStatus())) return;
        run.setCancelRequest(true);
        run.setCancelRequestTime(LocalDateTime.now());
        run.setStatus(AiTaskRunStatus.CANCEL_REQUESTED);
        runMapper.updateById(run);
    }

    public AiTaskRun requireOwnedRun(String runId, String tenantId, String userId) {
        AiTaskRun run = requireRun(runId);
        if (!java.util.Objects.equals(run.getTenantId(), tenantId) || !java.util.Objects.equals(run.getUserId(), userId)) {
            throw new AiRunAccessDeniedException();
        }
        return run;
    }

    public boolean isCancellationRequested(String runId) {
        AiTaskRun run = requireRun(runId);
        return Boolean.TRUE.equals(run.getCancelRequest())
                || AiTaskRunStatus.CANCEL_REQUESTED.equals(run.getStatus())
                || AiTaskRunStatus.CANCELLED.equals(run.getStatus());
    }

    public void markCancelled(String runId) {
        updateRun(runId, AiTaskRunStatus.CANCELLED, "CANCELLED", "AI task run was cancelled", run -> { });
    }

    public List<AiTaskStep> listRecoverableSteps() {
        LocalDateTime now = LocalDateTime.now();
        return stepMapper.selectList(new LambdaQueryWrapper<AiTaskStep>()
                .and(wrapper -> wrapper.eq(AiTaskStep::getStatus, AiTaskRunStatus.RETRY_WAIT)
                        .le(AiTaskStep::getNextRetryTime, now)
                        .or()
                        .eq(AiTaskStep::getStatus, AiTaskRunStatus.RUNNING)
                        .le(AiTaskStep::getLeaseExpireTime, now)));
    }

    public void markRecoverableStepPending(AiTaskStep step) {
        step.setStatus(AiTaskRunStatus.PENDING);
        step.setLeaseExpireTime(null);
        stepMapper.updateById(step);
    }

    private void setCurrentStep(String runId, int stepNo) {
        AiTaskRun run = requireRun(runId);
        run.setCurrentStepNo(stepNo);
        runMapper.updateById(run);
    }

    private AiTaskRun findByIdempotencyKey(String tenantId, String key) {
        return key == null || key.isBlank() ? null : runMapper.selectOne(
                new LambdaQueryWrapper<AiTaskRun>().eq(AiTaskRun::getTenantId, requiredTenant(tenantId))
                        .eq(AiTaskRun::getIdempotencyKey, key));
    }

    private void updateRun(String runId, String status, String errorCode, String errorMessage,
                           java.util.function.Consumer<AiTaskRun> customize) {
        AiTaskRun run = requireRun(runId);
        customize.accept(run);
        run.setStatus(status);
        if (AiTaskRunStatus.isTerminal(status)) run.setFinishTime(LocalDateTime.now());
        run.setErrorCode(errorCode);
        run.setErrorMessage(errorMessage);
        runMapper.updateById(run);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
    private String requiredTenant(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("tenantId must not be blank");
        return value.trim();
    }

}
