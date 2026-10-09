package com.xqy.sms.ai.infrastructure.service.history;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.xqy.sms.ai.infrastructure.persistence.mapper.AiChatConversationMapper;
import com.xqy.sms.ai.infrastructure.persistence.mapper.AiChatMessageMapper;
import com.xqy.sms.common.entity.AiChatConversation;
import com.xqy.sms.common.entity.AiChatMessage;
import com.xqy.sms.common.entity.AiTaskRun;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Persists the user and assistant turns of an AI chat. */
@Service
public class AiChatHistoryService {
    private final AiChatConversationMapper conversationMapper;
    private final AiChatMessageMapper messageMapper;

    public AiChatHistoryService(AiChatConversationMapper conversationMapper, AiChatMessageMapper messageMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

    @Transactional
    public void recordUserMessage(AiTaskRun run) {
        validateOwner(run);
        if (blank(run.getQuestion())) throw new IllegalArgumentException("chat question must not be blank");
        persist(run, "user", run.getQuestion());
    }

    @Transactional
    public void recordAssistantMessage(AiTaskRun run, String content) {
        validateOwner(run);
        if (blank(content)) throw new IllegalArgumentException("assistant response must not be blank");
        persist(run, "assistant", content);
    }

    private void persist(AiTaskRun run, String role, String content) {
        AiChatConversation conversation = findOrCreate(run);
        if (messageMapper.exists(new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getConversationId, conversation.getId())
                .eq(AiChatMessage::getRunId, run.getRunId())
                .eq(AiChatMessage::getRole, role))) return;
        AiChatMessage message = new AiChatMessage();
        message.setConversationId(conversation.getId());
        message.setTenantId(run.getTenantId());
        message.setUserId(run.getUserId());
        message.setSessionId(run.getSessionId());
        message.setRole(role);
        message.setContent(content);
        message.setRunId(run.getRunId());
        message.setSequenceNo(Math.addExact(messageMapper.lastSequenceNo(conversation.getId()), 1));
        messageMapper.insert(message);
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);
    }

    private AiChatConversation findOrCreate(AiTaskRun run) {
        AiChatConversation conversation = new AiChatConversation();
        conversation.setId(IdWorker.getId());
        conversation.setTenantId(run.getTenantId());
        conversation.setUserId(run.getUserId());
        conversation.setSessionId(run.getSessionId());
        String question = run.getQuestion() == null ? "AI chat" : run.getQuestion();
        conversation.setTitle(question.substring(0, question.offsetByCodePoints(0,
                Math.min(512, question.codePointCount(0, question.length())))));
        conversation.setStatus("ACTIVE");
        conversationMapper.createIfAbsent(conversation);
        conversation = conversationMapper.lockByOwner(run.getTenantId(), run.getUserId(), run.getSessionId());
        if (conversation == null) throw new IllegalStateException("chat conversation was not persisted");
        return conversation;
    }

    private void validateOwner(AiTaskRun run) {
        if (run == null || blank(run.getTenantId()) || blank(run.getUserId())
                || blank(run.getSessionId()) || blank(run.getRunId())) {
            throw new IllegalArgumentException("chat history requires tenant, user, session and run identifiers");
        }
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
