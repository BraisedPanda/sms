package com.xqy.sms.ai.model;

import com.xqy.sms.common.entity.AiToolDefinition;

import java.util.List;

/** Supplies the tool definitions visible to an AI model. */
public interface AiToolDefinitionProvider {

    List<AiToolDefinition> definitions();

    default List<AiToolDefinition> getDefinitions() {
        return definitions();
    }
}
