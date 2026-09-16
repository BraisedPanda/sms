package com.xqy.sms.ai.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xqy.sms.common.entity.AiRequestLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiRequestLogMapper extends BaseMapper<AiRequestLog> {
}
