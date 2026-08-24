package com.xqy.sms.ai.model;

public class BusinessContext {

    private String domain;
    private String toolName;
    private String taskQuestion;
    private QueryCriteria  queryCriteria;

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

    public String getTaskQuestion() {
        return taskQuestion;
    }

    public void setTaskQuestion(String taskQuestion) {
        this.taskQuestion = taskQuestion;
    }

    public QueryCriteria getQueryCriteria() {
        return queryCriteria;
    }

    public void setQueryCriteria(QueryCriteria queryCriteria) {
        this.queryCriteria = queryCriteria;
    }
}
