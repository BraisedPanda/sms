package com.xqy.sms.ai.api.model;

import java.io.Serializable;

/** Transport-neutral command for importing staged knowledge document chunks. */
public record KnowledgeIngestionCommand(Long knowledgeBaseId, Long documentVersionId,
                                        Integer limit, Integer indexRevision) implements Serializable { }
