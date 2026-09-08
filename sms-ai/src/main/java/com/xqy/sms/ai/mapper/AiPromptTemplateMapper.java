package com.xqy.sms.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xqy.sms.common.entity.AiPromptTemplate;
import org.apache.ibatis.annotations.Mapper;

/** Persistence mapper for database-managed AI prompt templates. */
@Mapper
public interface AiPromptTemplateMapper extends BaseMapper<AiPromptTemplate> {
}
