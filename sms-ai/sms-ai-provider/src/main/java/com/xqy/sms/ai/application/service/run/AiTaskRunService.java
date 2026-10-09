package com.xqy.sms.ai.application.service.run;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
        return createOrReuse(requestId, tenantId, userId, sessionId, question, modelAlias, idempotencyKey, null, null);
    }

    public AiTaskRun createOrReuse(String requestId, String tenantId, String userId, String sessionId, String question,
                                  String modelAlias, String idempotencyKey, String streamKey, Long conversationId) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            AiTaskRun existing = findByIdempotencyKey(tenantId, idempotencyKey);
            if (existing != null) {
                if (java.util.Objects.equals(existing.getTenantId(), tenantId)
                        && java.util.Objects.equals(existing.getUserId(), userId)
                        && java.util.Objects.equals(existing.getSessionId(), sessionId)) {
                    validateRetry(existing, question, modelAlias, conversationId);
                    return existing;
                }
                throw new AiRunAccessDeniedException();
            }
        }
        AiTaskRun run = new AiTaskRun();
        run.setRunId(UUID.randomUUID().toString());
        run.setRequestId(requestId);
        run.setTenantId(requiredTenant(tenantId));
        run.setUserId(userId);
        run.setSessionId(sessionId);
        run.setStreamKey(streamKey);
        run.setConversationId(conversationId);
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
            if (existing != null) {
                if (!java.util.Objects.equals(existing.getUserId(), userId) || !java.util.Objects.equals(existing.getSessionId(), sessionId)) throw new AiRunAccessDeniedException();
                validateRetry(existing, question, modelAlias, conversationId);
                return existing;
            }
            throw exception;
        }
    }

    public void attachConversation(String runId, Long conversationId) {
        runMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AiTaskRun>()
                .eq(AiTaskRun::getRunId, runId).isNull(AiTaskRun::getConversationId).set(AiTaskRun::getConversationId, conversationId));
    }

    private void validateRetry(AiTaskRun existing, String question, String alias, Long conversationId) {
        if (!java.util.Objects.equals(existing.getQuestion(), question) || !java.util.Objects.equals(existing.getModelAlias(), alias)
                || (conversationId != null && !java.util.Objects.equals(existing.getConversationId(), conversationId)))
            throw new IllegalArgumentException("同一幂等键不能提交不同问题、模型或对话");
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
                .eq(AiTaskRun::getCancelRequest, false)
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

    public boolean completeRun(String runId) {
        return updateRun(runId, AiTaskRunStatus.SUCCEEDED, null, null, run -> { });
    }

    public void failRun(String runId, Throwable error) {
        String code = error == null ? "RUN_FAILED" : error.getClass().getSimpleName();
        String message = error == null ? null : error.getMessage();
        updateRun(runId, AiTaskRunStatus.FAILED, code, message, run -> { });
    }

    public boolean cancel(String runId, String tenantId, String userId) {
        AiTaskRun run = requireOwnedRun(runId, tenantId, userId);
        if (AiTaskRunStatus.isTerminal(run.getStatus())) return false;
        return runMapper.update(null, activeRun(runId)
                .set(AiTaskRun::getCancelRequest, true)
                .set(AiTaskRun::getCancelRequestTime, LocalDateTime.now())
                .set(AiTaskRun::getStatus, AiTaskRunStatus.CANCELLED)
                .set(AiTaskRun::getFinishTime, LocalDateTime.now())
                .set(AiTaskRun::getErrorCode, "CANCELLED")
                .set(AiTaskRun::getErrorMessage, "已停止生成")) > 0;
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
        runMapper.update(null, activeRun(runId).eq(AiTaskRun::getCancelRequest, false)
                .set(AiTaskRun::getCurrentStepNo, stepNo));
    }

    private AiTaskRun findByIdempotencyKey(String tenantId, String key) {
        return key == null || key.isBlank() ? null : runMapper.selectOne(
                new LambdaQueryWrapper<AiTaskRun>().eq(AiTaskRun::getTenantId, requiredTenant(tenantId))
                        .eq(AiTaskRun::getIdempotencyKey, key));
    }

    private boolean updateRun(String runId, String status, String errorCode, String errorMessage,
                           java.util.function.Consumer<AiTaskRun> customize) {
        AiTaskRun run = requireRun(runId);
        String oldPlan = run.getPlanJson();
        customize.accept(run);
        LambdaUpdateWrapper<AiTaskRun> update = activeRun(runId)
                .set(AiTaskRun::getStatus, status)
                .set(AiTaskRun::getErrorCode, errorCode)
                .set(AiTaskRun::getErrorMessage, errorMessage);
        if (AiTaskRunStatus.CANCELLED.equals(status)) update.set(AiTaskRun::getCancelRequest, true);
        else update.eq(AiTaskRun::getCancelRequest, false);
        if (AiTaskRunStatus.isTerminal(status)) update.set(AiTaskRun::getFinishTime, LocalDateTime.now());
        if (!java.util.Objects.equals(oldPlan, run.getPlanJson())) update.set(AiTaskRun::getPlanJson, run.getPlanJson());
        return runMapper.update(null, update) > 0;
    }

    private LambdaUpdateWrapper<AiTaskRun> activeRun(String runId) {
        return new LambdaUpdateWrapper<AiTaskRun>().eq(AiTaskRun::getRunId, runId)
                .notIn(AiTaskRun::getStatus, AiTaskRunStatus.SUCCEEDED, AiTaskRunStatus.FAILED, AiTaskRunStatus.CANCELLED);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
    private String requiredTenant(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("tenantId must not be blank");
        return value.trim();
    }

}
