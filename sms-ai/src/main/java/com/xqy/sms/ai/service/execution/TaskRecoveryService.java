package com.xqy.sms.ai.service.execution;

import cn.hutool.json.JSONUtil;
import com.xqy.sms.ai.model.AiTask;
import com.xqy.sms.ai.service.run.AiTaskRunService;
import com.xqy.sms.common.entity.AiTaskStep;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Reclaims expired leases and due retries after a process failure. */
@Service
public class TaskRecoveryService {
    private static final Logger log = LoggerFactory.getLogger(TaskRecoveryService.class);
    private final AiTaskRunService runService;
    private final TaskExecutionService executionService;

    public TaskRecoveryService(AiTaskRunService runService, TaskExecutionService executionService) {
        this.runService = runService;
        this.executionService = executionService;
    }

    @Scheduled(fixedDelayString = "${sms.ai.task-recovery-delay-ms:10000}")
    public void recover() {
        for (AiTaskStep step : runService.listRecoverableSteps()) {
            try {
                runService.markRecoverableStepPending(step);
                if (!"TOOL".equals(step.getStepType())) continue;
                AiTask task = JSONUtil.toBean(step.getInputJson(), AiTask.class);
                executionService.execute(step.getTaskRunId(), step, task);
            } catch (Exception error) {
                // The execution service persists retry/failure details; the next sweep can claim due work.
                log.warn("Unable to recover AI task step {}", step.getStepId(), error);
            }
        }
    }
}
