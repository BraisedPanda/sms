package com.xqy.sms.ai.model;

import com.xqy.sms.common.entity.AiModelDefinition;

/** Provider adapter that turns a model definition into LangChain4j models. */
public interface ModelAdapter {

    String provider();

    ModelHandle create(AiModelDefinition modelDefinition);
}
