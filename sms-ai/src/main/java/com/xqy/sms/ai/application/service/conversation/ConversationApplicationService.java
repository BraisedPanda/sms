package com.xqy.sms.ai.application.service.conversation;

import cn.hutool.json.JSONUtil;
import com.xqy.sms.ai.domain.model.AiConstants;
import com.xqy.sms.ai.domain.model.AiTask;
import com.xqy.sms.ai.domain.model.AiTaskRequest;
import com.xqy.sms.ai.domain.model.AiTaskResult;
import com.xqy.sms.ai.application.service.chat.AiChatService;
import com.xqy.sms.ai.application.service.execution.TaskExecutionService;
import com.xqy.sms.ai.infrastructure.service.log.AiRequestLogService;
import com.xqy.sms.ai.application.service.plan.TaskPlannerService;
import com.xqy.sms.ai.application.service.run.AiTaskRunService;
import com.xqy.sms.ai.application.service.transport.SseTransportService;
import com.xqy.sms.common.entity.AiTaskRun;
import com.xqy.sms.common.entity.AiTaskStep;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
    private final SseTransportService transport;
    private final TaskExecutor workflowExecutor;
    private final int maxAttempts;
    private final long stepTimeoutMs;
    private final long businessResultTtlMinutes;

    public ConversationApplicationService(AiTaskRunService runService, TaskPlannerService plannerService,
                                          TaskExecutionService executionService, AiChatService chatService,
                                          AiRequestLogService requestLogService, StringRedisTemplate redisTemplate,
                                          SseTransportService transport,
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
        this.transport = transport;
        this.workflowExecutor = workflowExecutor;
        this.maxAttempts = maxAttempts;
        this.stepTimeoutMs = stepTimeoutMs;
        this.businessResultTtlMinutes = businessResultTtlMinutes;
    }

    public SseEmitter start(AiTaskRequest request) {
        validate(request);
        String alias = request.getAlias() == null || request.getAlias().isBlank()
                ? AiConstants.MODEL_ALIAS.BALANCED : request.getAlias().trim();
        AiTaskRun run = runService.createOrReuse(UUID.randomUUID().toString(), request.getUserId(), request.getSessionId(),
                request.getQuestion(), alias, request.getIdempotencyKey());
        SseEmitter emitter = transport.createEmitter();
        transport.send(emitter, AiConstants.SSE_EVENT.START, java.util.Map.of("runId", run.getRunId()));
        if (runService.claimPendingRun(run.getRunId())) {
            requestLogService.start(run.getRequestId(), run.getUserId(), run.getSessionId(), run.getQuestion(),
                    "chat", run.getModelAlias());
            workflowExecutor.execute(() -> execute(run.getRunId(), emitter));
        } else {
            transport.send(emitter, AiConstants.SSE_EVENT.START,
                    java.util.Map.of("runId", run.getRunId(), "status", run.getStatus()));
            if (com.xqy.sms.ai.domain.model.AiTaskRunStatus.isTerminal(run.getStatus())) emitter.complete();
        }
        return emitter;
    }

    public void cancel(String runId) {
        runService.cancel(runId);
        executionService.cancel(runId);
    }

    private void execute(String runId, SseEmitter emitter) {
        try {
            AiTaskRun run = runService.requireRun(runId);
            transport.send(emitter, AiConstants.SSE_EVENT.PLANNING, "分析问题中");
            AiTaskStep planStep = runService.createStep(runId, 1, "PLAN", "planner", null,
                    run.getQuestion(), 1, stepTimeoutMs);
            runService.startStep(planStep, 1);
            List<AiTask> tasks = plannerService.plan(run.getQuestion(), businessContext(run), run.getModelAlias());
            String planJson = JSONUtil.toJsonStr(tasks);
            runService.succeedStep(planStep, planJson);
            runService.savePlan(runId, planJson);
            checkCancelled(runId);

            if (isChatPlan(tasks)) {
                compose(runId, emitter, run, List.of());
                return;
            }
            transport.send(emitter, AiConstants.SSE_EVENT.EXECUTING, "查询相关数据");
            List<AiTaskResult> results = new ArrayList<>();
            for (int index = 0; index < tasks.size(); index++) {
                checkCancelled(runId);
                AiTask task = tasks.get(index);
                task.setRequestId(run.getRequestId());
                AiTaskStep step = runService.createStep(runId, index + 2, "TOOL", task.getDomain(), task.getToolName(),
                        JSONUtil.toJsonStr(task), maxAttempts, stepTimeoutMs);
                results.add(executionService.execute(runId, step, task));
            }
            String resultJson = JSONUtil.toJsonStr(results);
            redisTemplate.opsForValue().set(businessKey(run), resultJson,
                    Duration.ofMinutes(businessResultTtlMinutes));
            compose(runId, emitter, run, results);
        } catch (TaskExecutionService.TaskCancelledException cancelled) {
            runService.markCancelled(runId);
            requestLogService.fail(runService.requireRun(runId).getRequestId(), "CANCELLED", cancelled);
            transport.send(emitter, AiConstants.SSE_EVENT.ERROR, "AI 任务已取消");
            emitter.complete();
        } catch (Exception error) {
            runService.failRun(runId, error);
            requestLogService.fail(runService.requireRun(runId).getRequestId(), error.getClass().getSimpleName(), error);
            transport.error(emitter, error);
        }
    }

    private void compose(String runId, SseEmitter emitter, AiTaskRun run, List<AiTaskResult> results) {
        checkCancelled(runId);
        int composeStepNo = results.isEmpty() ? 2 : results.size() + 2;
        AiTaskStep composeStep = runService.createStep(runId, composeStepNo, "COMPOSE", "chat", null,
                JSONUtil.toJsonStr(results), 1, stepTimeoutMs);
        runService.startStep(composeStep, 1);
        runService.setRunStatus(runId, com.xqy.sms.ai.domain.model.AiTaskRunStatus.COMPOSING);
        String prompt = results.isEmpty() ? run.getQuestion()
                : "用户问题：\n" + run.getQuestion() + "\n\n业务查询结果（只能依据此结果回答，不要编造）：\n"
                + JSONUtil.toJsonStr(results);
        chatService.answer(emitter, prompt, memoryKey(run), run.getRequestId(), run.getModelAlias(),
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
