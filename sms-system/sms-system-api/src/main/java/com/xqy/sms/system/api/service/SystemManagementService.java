package com.xqy.sms.system.api.service;

import com.xqy.sms.common.dto.PageResult;
import com.xqy.sms.common.security.rpc.InternalCallContext;
import java.util.List;
import java.util.Map;

public interface SystemManagementService {
    void authorize(String authority, InternalCallContext caller);
    PageResult list(String resource, Map<String, String> filters, int current, int size, InternalCallContext caller);
    Map<String, Object> save(String resource, Long id, Map<String, Object> values, InternalCallContext caller);
    void delete(String resource, Long id, InternalCallContext caller);
    Map<String, Object> assignments(String resource, Long id, InternalCallContext caller);
    void assignUserRoles(Long userId, List<Long> roleIds, InternalCallContext caller);
    void grantRole(Long roleId, List<Long> menuIds, List<Long> buttonIds, InternalCallContext caller);
    void revokeSession(Long sessionId, InternalCallContext caller);
}
