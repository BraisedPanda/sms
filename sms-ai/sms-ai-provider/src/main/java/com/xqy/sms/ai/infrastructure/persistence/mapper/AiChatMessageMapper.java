package com.xqy.sms.ai.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xqy.sms.common.entity.AiChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {
    @Select("SELECT COALESCE(MAX(sequence_no), 0) FROM ai_chat_message WHERE conversation_id = #{conversationId}")
    int lastSequenceNo(@Param("conversationId") Long conversationId);
}
