package com.xqy.sms.ai.service.log;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xqy.sms.ai.mapper.AiRequestLogMapper;
import com.xqy.sms.ai.model.AiRequestLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/** Persists the lifecycle of one user-facing AI request. */
@Service
public class AiRequestLogService {

    private static final Logger log = LoggerFactory.getLogger(AiRequestLogService.class);
    private final AiRequestLogMapper mapper;

    public AiRequestLogService(AiRequestLogMapper mapper) {
        this.mapper = mapper;
    }

    public AiRequestLog start(String userId, String sessionId, String question,
                              String requestType, String modelName) {
        AiRequestLog record = new AiRequestLog();
        record.setRequestId(UUID.randomUUID().toString());
        record.setUserId(userId);
        record.setSessionId(sessionId);
        record.setQuestion(question);
        record.setRequestType(requestType);
        record.setModelName(modelName);
        record.setStartTime(LocalDateTime.now());
        record.setSuccess(false);
        persist(record, "start request");
        return record;
    }

    public void success(String requestId, String modelName, Integer inputTokens,
                        Integer outputTokens, Integer totalTokens) {
        finish(requestId, true, null, null, modelName, inputTokens, outputTokens, totalTokens);
    }

    public void fail(String requestId, String errorCode, Throwable error) {
        finish(requestId, false, errorCode, message(error), null, null, null, null);
    }

    public void finish(String requestId, boolean success, String errorCode, String errorMessage,
                       String modelName, Integer inputTokens, Integer outputTokens, Integer totalTokens) {
        if (requestId == null || requestId.isBlank()) {
            return;
        }
        try {
            AiRequestLog record = mapper.selectOne(new LambdaQueryWrapper<AiRequestLog>()
                    .eq(AiRequestLog::getRequestId, requestId));
            if (record == null) {
                log.warn("requestId={} request log not found while finishing", requestId);
                return;
            }
            LocalDateTime finishTime = LocalDateTime.now();
            record.setFinishTime(finishTime);
            record.setDurationTime(Duration.between(record.getStartTime(), finishTime).toMillis());
            record.setSuccess(success);
            if (modelName != null && !modelName.isBlank()) record.setModelName(modelName);
            record.setInputTokenCount(inputTokens);
            record.setOutputTokenCount(outputTokens);
            record.setTotalTokenCount(totalTokens);
            record.setErrorCode(errorCode);
            record.setErrorMessage(errorMessage);
            mapper.updateById(record);
            log.info("requestId={} request finished success={} durationMs={} tokens={}",
                    requestId, success, record.getDurationTime(), totalTokens);
        } catch (Exception persistenceError) {
            log.error("requestId={} failed to persist request completion log", requestId, persistenceError);
        }
    }

    private void persist(AiRequestLog record, String operation) {
        try {
            mapper.insert(record);
            log.info("requestId={} request started type={} model={}",
                    record.getRequestId(), record.getRequestType(), record.getModelName());
        } catch (Exception persistenceError) {
            log.error("requestId={} failed to persist {} log", record.getRequestId(), operation, persistenceError);
        }
    }

    private static String message(Throwable error) {
        return error == null || error.getMessage() == null ? null : error.getMessage();
    }
}
