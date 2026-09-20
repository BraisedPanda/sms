package com.xqy.sms.ai.api.service;

import com.xqy.sms.ai.api.model.AiChatCommand;
import com.xqy.sms.ai.api.model.AiRunView;
import com.xqy.sms.common.security.rpc.InternalCallContext;

public interface AiChatCommandService {
    /** Accepts a chat command; provider events are appended to the command's Redis Stream key. */
    void submit(AiChatCommand command);
    void cancel(String runId, InternalCallContext callerContext);
    AiRunView getRun(String runId, InternalCallContext callerContext);
}
