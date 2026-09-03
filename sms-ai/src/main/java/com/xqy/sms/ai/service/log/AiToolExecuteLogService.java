package com.xqy.sms.ai.service.log;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xqy.sms.ai.mapper.AiToolExecuteLogMapper;
import com.xqy.sms.ai.model.AiTask;
import com.xqy.sms.ai.model.AiTaskResult;
import com.xqy.sms.ai.model.AiToolExecuteLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/** Persists the lifecycle of each domain-tool execution. */
@Service
public class AiToolExecuteLogService {

    private static final Logger log = LoggerFactory.getLogger(AiToolExecuteLogService.class);
    private final AiToolExecuteLogMapper mapper;

    public AiToolExecuteLogService(AiToolExecuteLogMapper mapper) {
        this.mapper = mapper;
    }

    public AiToolExecuteLog start(AiTask task, String modelName) {
        AiToolExecuteLog record = new AiToolExecuteLog();
        record.setToolExecuteId(UUID.randomUUID().toString());
        record.setRequestId(task == null ? null : task.getRequestId());
        record.setDomain(task == null ? null : task.getDomain());
        record.setToolName(task == null ? null : task.getToolName());
        record.setQuestion(task == null ? null : task.getSubQuestion());
        record.setModelName(modelName);
        record.setStartTime(LocalDateTime.now());
        record.setSuccess(false);
        persist(record);
        return record;
    }

    public void success(String toolExecuteId, AiTaskResult result) {
        Integer resultCount = result == null || result.getItems() == null ? null : result.getItems().size();
        finish(toolExecuteId, true, null, null, resultCount);
    }

    public void fail(String toolExecuteId, Throwable error) {
        finish(toolExecuteId, false, error == null ? null : error.getClass().getSimpleName(),
                error == null ? null : error.getMessage(), null);
    }

    private void finish(String toolExecuteId, boolean success, String errorCode,
                        String errorMessage, Integer resultCount) {
        if (toolExecuteId == null || toolExecuteId.isBlank()) return;
        try {
            AiToolExecuteLog record = mapper.selectOne(new LambdaQueryWrapper<AiToolExecuteLog>()
                    .eq(AiToolExecuteLog::getToolExecuteId, toolExecuteId));
            if (record == null) {
                log.warn("toolExecuteId={} tool log not found while finishing", toolExecuteId);
                return;
            }
            LocalDateTime finishTime = LocalDateTime.now();
            record.setFinishTime(finishTime);
            record.setDurationTime(Duration.between(record.getStartTime(), finishTime).toMillis());
            record.setSuccess(success);
            record.setResultCount(resultCount);
            record.setErrorCode(errorCode);
            record.setErrorMessage(errorMessage);
            mapper.updateById(record);
            log.info("requestId={} toolExecuteId={} domain={} tool={} finished success={} durationMs={}",
                    record.getRequestId(), toolExecuteId, record.getDomain(), record.getToolName(),
                    success, record.getDurationTime());
        } catch (Exception persistenceError) {
            log.error("toolExecuteId={} failed to persist tool completion log", toolExecuteId, persistenceError);
        }
    }

    private void persist(AiToolExecuteLog record) {
        try {
            mapper.insert(record);
            log.info("requestId={} toolExecuteId={} domain={} tool={} started",
                    record.getRequestId(), record.getToolExecuteId(), record.getDomain(), record.getToolName());
        } catch (Exception persistenceError) {
            log.error("requestId={} toolExecuteId={} failed to persist tool start log",
                    record.getRequestId(), record.getToolExecuteId(), persistenceError);
        }
    }
}
