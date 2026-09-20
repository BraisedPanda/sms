package com.xqy.sms.common.security.rpc;

import java.io.Serializable;

/** Signed caller and end-user context propagated across internal RPC boundaries. */
public record InternalCallContext(String callerService, String tenantId, Long userId, Long sessionId,
                                  String requestId, long issuedAtEpochSecond, String signature)
        implements Serializable { }
