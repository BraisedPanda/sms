package com.xqy.sms.ai.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xqy.sms.common.entity.AiChatConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiChatConversationMapper extends BaseMapper<AiChatConversation> {
    @Insert("""
            INSERT INTO ai_chat_conversation (id, tenant_id, user_id, session_id, title, status)
            VALUES (#{id}, #{tenantId}, #{userId}, #{sessionId}, #{title}, #{status})
            ON DUPLICATE KEY UPDATE id = id
            """)
    int createIfAbsent(AiChatConversation conversation);

    @Select("""
            SELECT * FROM ai_chat_conversation
            WHERE tenant_id = #{tenantId} AND user_id = #{userId} AND session_id = #{sessionId}
            FOR UPDATE
            """)
    AiChatConversation lockByOwner(@Param("tenantId") String tenantId, @Param("userId") String userId,
                                   @Param("sessionId") String sessionId);

    @Select("SELECT * FROM ai_chat_conversation WHERE id=#{id} AND tenant_id=#{tenantId} AND user_id=#{userId} FOR UPDATE")
    AiChatConversation lockByIdAndOwner(@Param("id") Long id, @Param("tenantId") String tenantId, @Param("userId") String userId);
}
