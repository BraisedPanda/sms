package com.xqy.sms.ai.api.model;

import java.io.Serializable;

public record KnowledgeIngestionResult(int selected, int imported,
                                       int indexRevision) implements Serializable { }
