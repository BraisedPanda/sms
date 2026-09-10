package com.xqy.sms.ai.service.model;

import com.xqy.sms.ai.mapper.AiModelDefinitionMapper;
import com.xqy.sms.common.entity.AiModelDefinition;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/** Loads model definitions from the database. */
@Service
public class AiModelDefinitionService {

    private final AiModelDefinitionMapper aiModelDefinitionMapper;

    public AiModelDefinitionService(AiModelDefinitionMapper aiModelDefinitionMapper) {
        this.aiModelDefinitionMapper = aiModelDefinitionMapper;
    }

    public List<AiModelDefinition> definitions() {
        List<AiModelDefinition> definitions = aiModelDefinitionMapper.selectList(null);
        return definitions == null ? Collections.emptyList() : definitions;
    }
}
