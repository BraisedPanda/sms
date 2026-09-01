package com.xqy.sms.ai.service.tool;

import com.xqy.sms.ai.mapper.AiToolDefinitionMapper;
import com.xqy.sms.ai.model.AiToolDefinitionProvider;
import com.xqy.sms.common.entity.AiToolDefinition;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/** Loads the tool definitions visible to the planner from the database. */
@Service
public class AiToolDefinitionService implements AiToolDefinitionProvider {

    private final AiToolDefinitionMapper aiToolDefinitionMapper;

    public AiToolDefinitionService(AiToolDefinitionMapper aiToolDefinitionMapper) {
        this.aiToolDefinitionMapper = aiToolDefinitionMapper;
    }

    @Override
    public List<AiToolDefinition> definitions() {
        List<AiToolDefinition> definitions = aiToolDefinitionMapper.selectList(null);
        return definitions == null ? Collections.emptyList() : definitions;
    }
}
