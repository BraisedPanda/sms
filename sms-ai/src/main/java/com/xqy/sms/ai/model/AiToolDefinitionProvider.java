package com.xqy.sms.ai.model;

import java.util.List;

/** Supplies the tool definitions visible to an AI model. */
public interface AiToolDefinitionProvider {

    List<AiToolDefinition> definitions();

    default List<AiToolDefinition> getDefinitions() {
        return definitions();
    }
}
