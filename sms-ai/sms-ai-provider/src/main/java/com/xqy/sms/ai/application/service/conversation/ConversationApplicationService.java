package com.xqy.sms.ai.application.service.conversation;

import cn.hutool.json.JSONUtil;
import com.xqy.sms.ai.domain.model.AiConstants;
import com.xqy.sms.ai.domain.model.AiTask;
import com.xqy.sms.ai.domain.model.AiTaskRequest;
import com.xqy.sms.ai.domain.model.AiTaskResult;
import com.xqy.sms.ai.application.event.AiStreamEventPublisher;
import com.xqy.sms.ai.application.service.chat.AiChatService;
import com.xqy.sms.ai.application.service.execution.TaskExecutionService;
import com.xqy.sms.ai.infrastructure.service.log.AiRequestLogService;
import com.xqy.sms.ai.infrastructure.security.AiSafetyPolicy;
import com.xqy.sms.common.security.jwt.JwtUserContext;
import com.xqy.sms.ai.application.service.plan.TaskPlannerService;
import com.xqy.sms.ai.application.service.run.AiTaskRunService;
import com.xqy.sms.common.entity.AiTaskRun;
import com.xqy.sms.common.entity.AiTaskStep;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.time.Duration;
import java.util.UUID;

/** Orchestrates the PLAN, TOOL, and COMPOSE lifecycle of a durable conversation run. */
@Service
public class ConversationApplicationService {
    private final AiTaskRunService runService;
    private final TaskPlannerService plannerService;
    private final TaskExecutionService executionService;
    private final AiChatService chatService;
    private final AiRequestLogService requestLogService;
    private final StringRedisTemplate redisTemplate;
    private final AiStreamEventPublisher eventPublisher;
    private final TaskExecutor workflowExecutor;
    private final int maxAttempts;
    private final long stepTimeoutMs;
    private final long businessResultTtlMinutes;

    public ConversationApplicationService(AiTaskRunService runService, TaskPlannerService plannerService,
                                          TaskExecutionService executionService, AiChatService chatService,
                                          AiRequestLogService requestLogService, StringRedisTemplate redisTemplate,
                                          AiStreamEventPublisher eventPublisher,
                                          @Qualifier("aiWorkflowExecutor") TaskExecutor workflowExecutor,
                                          @Value("${sms.ai.workflow.max-attempts}") int maxAttempts,
                                          @Value("${sms.ai.workflow.step-timeout-ms}") long stepTimeoutMs,
                                          @Value("${sms.ai.workflow.business-result-ttl-minutes}") long businessResultTtlMinutes) {
        this.runService = runService;
        this.plannerService = plannerService;
        this.executionService = executionService;
        this.chatService = chatService;
        this.requestLogService = requestLogService;
        this.redisTemplate = redisTemplate;
        this.eventPublisher = eventPublisher;
        this.workflowExecutor = workflowExecutor;
        this.maxAttempts = maxAttempts;
        this.stepTimeoutMs = stepTimeoutMs;
        this.businessResultTtlMinutes = businessResultTtlMinutes;
    }

    public void start(AiTaskRequest request, JwtUserContext userContext, String streamKey) {
        validate(request);
        if (userContext == null) throw new IllegalArgumentException("authenticated user context must not be null");
        if (streamKey == null || streamKey.isBlank()) throw new IllegalArgumentException("streamKey must not be blank");
        String alias = request.getAlias() == null || request.getAlias().isBlank()
                ? AiConstants.MODEL_ALIAS.BALANCED : request.getAlias().trim();
        AiTaskRun run = runService.createOrReuse(UUID.randomUUID().toString(), userContext.tenantId(), String.valueOf(userContext.userId()),
                String.valueOf(userContext.sessionId()),
                request.getQuestion(), alias, request.getIdempotencyKey());
        publish(streamKey, AiConstants.STREAM_EVENT.START, java.util.Map.of("runId", run.getRunId()));
        if (runService.claimPendingRun(run.getRunId())) {
            requestLogService.start(run.getRequestId(), run.getTenantId(), run.getUserId(), run.getSessionId(), run.getQuestion(),
                    "chat", run.getModelAlias());
            workflowExecutor.execute(() -> execute(run.getRunId(), streamKey));
        } else {
            publish(streamKey, AiConstants.STREAM_EVENT.START,
                    java.util.Map.of("runId", run.getRunId(), "status", run.getStatus()));
            if (com.xqy.sms.ai.domain.model.AiTaskRunStatus.isTerminal(run.getStatus())) {
                publish(streamKey,
                        com.xqy.sms.ai.domain.model.AiTaskRunStatus.SUCCEEDED.equals(run.getStatus())
                                ? AiConstants.STREAM_EVENT.DONE : AiConstants.STREAM_EVENT.ERROR,
                        run.getStatus());
            }
        }
    }

    public void cancel(String runId, JwtUserContext userContext) {
        if (userContext == null) throw new IllegalArgumentException("authenticated user context must not be null");
        runService.cancel(runId, userContext.tenantId(), String.valueOf(userContext.userId()));
        executionService.cancel(runId);
    }

    private void execute(String runId, String streamKey) {
        try {
            AiTaskRun run = runService.requireRun(runId);
            publish(streamKey, AiConstants.STREAM_EVENT.PLANNING, "分析问题中");
            AiTaskStep planStep = runService.createStep(runId, 1, "PLAN", "planner", null,
                    run.getQuestion(), 1, stepTimeoutMs);
            runService.startStep(planStep, 1);
            List<AiTask> tasks = plannerService.plan(run.getQuestion(), businessContext(run), run.getModelAlias());
            String planJson = JSONUtil.toJsonStr(tasks);
            runService.succeedStep(planStep, planJson);
            runService.savePlan(runId, planJson);
            checkCancelled(runId);

            if (isChatPlan(tasks)) {
                compose(runId, streamKey, run, List.of());
                return;
            }
            publish(streamKey, AiConstants.STREAM_EVENT.EXECUTING, "查询相关数据");
            List<AiTaskResult> results = new ArrayList<>();
            for (int index = 0; index < tasks.size(); index++) {
                checkCancelled(runId);
                AiTask task = tasks.get(index);
                task.setRequestId(run.getRequestId());
                task.setTenantId(run.getTenantId());
                task.setUserId(run.getUserId());
                task.setSessionId(run.getSessionId());
                AiTaskStep step = runService.createStep(runId, index + 2, "TOOL", task.getDomain(), task.getToolName(),
                        JSONUtil.toJsonStr(task), maxAttempts, stepTimeoutMs);
                results.add(executionService.execute(runId, step, task));
            }
            String resultJson = JSONUtil.toJsonStr(results);
            redisTemplate.opsForValue().set(businessKey(run), resultJson,
                    Duration.ofMinutes(businessResultTtlMinutes));
            compose(runId, streamKey, run, results);
        } catch (TaskExecutionService.TaskCancelledException cancelled) {
            runService.markCancelled(runId);
            requestLogService.fail(runService.requireRun(runId).getRequestId(), "CANCELLED", cancelled);
            publish(streamKey, AiConstants.STREAM_EVENT.ERROR, "AI 任务已取消");
        } catch (Exception error) {
            runService.failRun(runId, error);
            requestLogService.fail(runService.requireRun(runId).getRequestId(), error.getClass().getSimpleName(), error);
            publish(streamKey, AiConstants.STREAM_EVENT.ERROR,
                    error.getMessage() == null ? "AI task failed" : error.getMessage());
        }
    }

    private void compose(String runId, String streamKey, AiTaskRun run, List<AiTaskResult> results) {
        checkCancelled(runId);
        int composeStepNo = results.isEmpty() ? 2 : results.size() + 2;
        AiTaskStep composeStep = runService.createStep(runId, composeStepNo, "COMPOSE", "chat", null,
                JSONUtil.toJsonStr(results), 1, stepTimeoutMs);
        runService.startStep(composeStep, 1);
        runService.setRunStatus(runId, com.xqy.sms.ai.domain.model.AiTaskRunStatus.COMPOSING);
        if (!results.isEmpty()) {
            publish(streamKey, AiConstants.STREAM_EVENT.SOURCES, AiSafetyPolicy.publicSources(results));
        }
        String prompt = results.isEmpty() ? run.getQuestion()
                : "User question:\n" + AiSafetyPolicy.redact(run.getQuestion()) + "\n\n"
                + AiSafetyPolicy.promptResults(results);
        chatService.answer(streamKey, prompt, memoryKey(run), run.getRequestId(), run.getModelAlias(),
                () -> {
                    if (runService.isCancellationRequested(runId)) {
                        runService.markCancelled(runId);
                        return;
                    }
                    runService.succeedStep(composeStep, "STREAM_COMPLETED");
                    runService.completeRun(runId);
                }, error -> {
                    runService.failStep(composeStep, error.getClass().getSimpleName(), error.getMessage());
                    runService.failRun(runId, error);
                });
    }

    private void checkCancelled(String runId) {
        if (runService.isCancellationRequested(runId)) throw new TaskExecutionService.TaskCancelledException();
    }

    private void publish(String streamKey, String type, Object data) {
        eventPublisher.publish(streamKey, type, data);
    }

    private boolean isChatPlan(List<AiTask> tasks) {
        return tasks.stream().allMatch(task -> AiConstants.TASK_DOMAIN.CHAT.equals(task.getDomain()));
    }

    private String businessContext(AiTaskRun run) { return redisTemplate.opsForValue().get(businessKey(run)); }
    private String businessKey(AiTaskRun run) { return AiConstants.CACHE_KEY.BUSINESS_PREFIX + run.getUserId() + "_" + run.getSessionId(); }
    private String memoryKey(AiTaskRun run) { return AiConstants.CACHE_KEY.CHAT_PREFIX + run.getUserId() + "_" + run.getSessionId(); }

    private void validate(AiTaskRequest request) {
        if (request == null || request.getQuestion() == null || request.getQuestion().isBlank()) {
            throw new IllegalArgumentException("question must not be blank");
        }
    }
}
