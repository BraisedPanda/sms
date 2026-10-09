package com.xqy.sms.ai.api.service;

import com.xqy.sms.common.dto.PageResult;
import com.xqy.sms.common.security.rpc.InternalCallContext;
import java.util.Map;

public interface AiOperationsService {
    void requireCaller(InternalCallContext caller);
    PageResult list(String resource, Map<String, String> filters, int current, int size, InternalCallContext caller);
    PageResult messages(Long conversationId, int current, int size, InternalCallContext caller);
    Map<String, Object> delivery(String runId, InternalCallContext caller);
    void requireConversation(Long id, InternalCallContext caller);
}
