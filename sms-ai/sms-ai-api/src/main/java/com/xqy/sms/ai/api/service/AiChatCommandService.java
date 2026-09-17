package com.xqy.sms.ai.api.service;

import com.xqy.sms.ai.api.model.AiChatCommand;

public interface AiChatCommandService {
    /** Accepts a chat command; provider events are appended to the command's Redis Stream key. */
    void submit(AiChatCommand command);
    void cancel(String runId, Long userId);
}
