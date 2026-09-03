package com.xqy.sms.ai.model;

import java.util.List;
import java.util.Map;

/** Result returned after an AI tool has executed. */
public class AiTaskResult {

    private String toolCallId;
    private String domain;
    private String toolName;
    private String question;
    private boolean success;
    private QueryCriteria query;
    private List<?> items;
    private Map<String, Object> summary;
    private List<ToolSource> sources;
    private String errorCode;
    private String errorMessage;

    public AiTaskResult() {
    }

    public AiTaskResult(String toolCallId, String domain, String toolName, String question,
                        boolean success, QueryCriteria query, List<?> items,
                        Map<String, Object> summary, List<ToolSource> sources,
                        String errorCode, String errorMessage) {
        this.toolCallId = toolCallId;
        this.domain = domain;
        this.toolName = toolName;
        this.question = question;
        this.success = success;
        this.query = query;
        this.items = items;
        this.summary = summary;
        this.sources = sources;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /** Compatibility constructor for the original result shape. */
    @Deprecated
    public AiTaskResult(String toolName, String ignoredType, List<?> items, Map<String, Object> summary) {
        this(null, null, toolName, null, true, null, items, summary, null, null, null);
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    /** Compatibility accessor matching the former record component method. */
    public String toolName() {
        return toolName;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public boolean isSuccess() {
        return success;
    }

    /** JavaBean-style alias for serializers or callers that prefer getX(). */
    public boolean getSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public QueryCriteria getQuery() {
        return query;
    }

    public void setQuery(QueryCriteria query) {
        this.query = query;
    }

    public List<?> getItems() {
        return items;
    }

    public void setItems(List<?> items) {
        this.items = items;
    }

    public Map<String, Object> getSummary() {
        return summary;
    }

    public void setSummary(Map<String, Object> summary) {
        this.summary = summary;
    }

    public List<ToolSource> getSources() {
        return sources;
    }

    public void setSources(List<ToolSource> sources) {
        this.sources = sources;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
