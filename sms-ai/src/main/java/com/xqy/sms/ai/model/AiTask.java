package com.xqy.sms.ai.model;

import java.util.ArrayList;
import java.util.List;

/** A request to execute one registered AI tool. */
public class AiTask {

    private String domain;
    private String toolName;
    private String reason;
    private QueryCriteria query;
    private List<String> missngArgs = new ArrayList<>();

    public AiTask() {
    }

    public AiTask(String domain, String toolName, String reason, QueryCriteria query,
                  List<String> missngArgs) {
        this.domain = domain;
        this.toolName = toolName;
        this.reason = reason;
        this.query = query;
        this.missngArgs = missngArgs;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public QueryCriteria getQuery() {
        return query;
    }

    public void setQuery(QueryCriteria query) {
        this.query = query;
    }

    public List<String> getMissngArgs() {
        return missngArgs;
    }

    public void setMissngArgs(List<String> missngArgs) {
        this.missngArgs = missngArgs;
    }

    /** Correctly-spelled aliases for callers that do not use the original wire name. */
    public List<String> getMissingArgs() {
        return missngArgs;
    }

    public void setMissingArgs(List<String> missingArgs) {
        this.missngArgs = missingArgs;
    }
}
