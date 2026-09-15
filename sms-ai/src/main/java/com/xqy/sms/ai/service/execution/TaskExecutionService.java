package com.xqy.sms.ai.service.execution;

import cn.hutool.json.JSONUtil;
import com.xqy.sms.ai.model.AiTask;
import com.xqy.sms.ai.model.AiTaskResult;
import com.xqy.sms.ai.model.AiToolRegistry;
import com.xqy.sms.ai.service.run.AiTaskRunService;
import com.xqy.sms.common.entity.AiTaskStep;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Executes one tool step with cancellation, timeout, retries, leases, and durable outcomes. */
@Service
public class TaskExecutionService {
    private final AiToolRegistry toolRegistry;
    private final AiTaskRunService runService;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ConcurrentMap<String, Future<?>> runningTasks = new ConcurrentHashMap<>();

    public TaskExecutionService(AiToolRegistry toolRegistry, AiTaskRunService runService) {
        this.toolRegistry = toolRegistry;
        this.runService = runService;
    }

    public AiTaskResult execute(String runId, AiTaskStep step, AiTask task) {
        validate(task);
        int maxAttempt = Math.max(1, step.getMaxAttempt());
        for (int attempt = Math.max(1, step.getAttempt() + 1); attempt <= maxAttempt; attempt++) {
            if (runService.isCancellationRequested(runId)) {
                runService.markCancelled(runId);
                throw new TaskCancelledException();
            }
            runService.startStep(step, attempt);
            Future<AiTaskResult> future = executor.submit(() -> toolRegistry.execute(task));
            runningTasks.put(runId, future);
            try {
                AiTaskResult result = future.get(step.getTimeoutMs(), TimeUnit.MILLISECONDS);
                runService.succeedStep(step, JSONUtil.toJsonStr(result));
                return result;
            } catch (TimeoutException exception) {
                future.cancel(true);
                handleFailure(step, attempt, maxAttempt, exception);
            } catch (TaskCancelledException exception) {
                future.cancel(true);
                runService.markCancelled(runId);
                throw exception;
            } catch (Exception exception) {
                handleFailure(step, attempt, maxAttempt, unwrap(exception));
            } finally {
                runningTasks.remove(runId, future);
            }
        }
        throw new IllegalStateException("Task step exhausted retry attempts");
    }

    /** Interrupts the active tool invocation, if its executor honors interruption. */
    public void cancel(String runId) {
        Future<?> future = runningTasks.get(runId);
        if (future != null) future.cancel(true);
    }

    private void handleFailure(AiTaskStep step, int attempt, int maxAttempt, Exception error) {
        String code = error.getClass().getSimpleName();
        String message = error.getMessage();
        if (attempt >= maxAttempt) {
            runService.failStep(step, code, message);
            throw new IllegalStateException("Task step failed after " + attempt + " attempts", error);
        }
        LocalDateTime retryAt = LocalDateTime.now().plusSeconds(Math.min(30, 1L << Math.min(attempt, 5)));
        runService.retryStep(step, code, message, retryAt);
        try {
            Thread.sleep(Math.max(1, java.time.Duration.between(LocalDateTime.now(), retryAt).toMillis()));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new TaskCancelledException();
        }
    }

    private void validate(AiTask task) {
        if (task == null || task.getMissingArgs() != null && !task.getMissingArgs().isEmpty()) {
            throw new IllegalArgumentException("Task is missing required arguments");
        }
    }

    private Exception unwrap(Exception error) {
        return error.getCause() instanceof Exception cause ? cause : error;
    }

    @PreDestroy
    public void destroy() { executor.shutdownNow(); }

    public static class TaskCancelledException extends RuntimeException {
        public TaskCancelledException() { super("AI task run was cancelled"); }
    }
}
