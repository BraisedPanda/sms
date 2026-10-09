package com.xqy.sms.knowledge.api.service;

import com.xqy.sms.common.dto.PageResult;
import com.xqy.sms.common.security.rpc.InternalCallContext;
import java.util.Map;

public interface KnowledgeManagementService {
    PageResult list(String resource, Map<String, String> filters, int current, int size, InternalCallContext caller);
    Map<String, Object> save(String resource, Long id, Map<String, Object> values, InternalCallContext caller);
    void delete(String resource, Long id, InternalCallContext caller);
    Map<String, Object> upload(Long knowledgeBaseId, Long documentId, String filename, byte[] bytes, InternalCallContext caller);
    void retryJob(Long jobId, InternalCallContext caller);
    void cancelJob(Long jobId, InternalCallContext caller);
}
