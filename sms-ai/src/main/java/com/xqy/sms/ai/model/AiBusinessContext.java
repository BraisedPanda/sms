package com.xqy.sms.ai.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Structured context shared by the planning, tool execution, and response
 * stages of an AI business request.
 */
public class AiBusinessContext {

    private String contextId;
    private String requestId;
    private String domain;
    private String toolName;
    private String question;
    private String subQuestion;
    private String status;
    private String errorCode;
    private String errorMessage;
    private QueryCriteria  queryCriteria;
    private List<String> missingArgs;
    private Integer resultCount;
    private boolean truncated;
    private Map<String, Object> summary;
    private List<ToolSource> sources;
    private LocalDateTime createTime;

    public String getContextId() {
        return contextId;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
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

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getSubQuestion() {
        return subQuestion;
    }

    public void setSubQuestion(String subQuestion) {
        this.subQuestion = subQuestion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public List<String> getMissingArgs() {
        return missingArgs;
    }

    public void setMissingArgs(List<String> missingArgs) {
        this.missingArgs = missingArgs;
    }

    public QueryCriteria getQueryCriteria() {
        return queryCriteria;
    }

    public void setQueryCriteria(QueryCriteria queryCriteria) {
        this.queryCriteria = queryCriteria;
    }

    public Integer getResultCount() {
        return resultCount;
    }

    public void setResultCount(Integer resultCount) {
        this.resultCount = resultCount;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public boolean getTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
