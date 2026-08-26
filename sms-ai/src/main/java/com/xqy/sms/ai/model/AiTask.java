package com.xqy.sms.ai.model;

import java.util.ArrayList;
import java.util.List;

/** A request to execute one registered AI tool. */
public class AiTask {

    private String domain;
    private String toolName;
    private String reason;
    private QueryCriteria query;
    private List<String> missingArgs = new ArrayList<>();

    public AiTask() {
    }

    public AiTask(String domain, String toolName, String reason, QueryCriteria query,
                  List<String> missingArgs) {
        this.domain = domain;
        this.toolName = toolName;
        this.reason = reason;
        this.query = query;
        this.missingArgs = missingArgs;
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

    public List<String> getMissingArgs() {
        return missingArgs;
    }

    public void setMissingArgs(List<String> missingArgs) {
        this.missingArgs = missingArgs;
    }

    /** Backward-compatible aliases for callers using the original misspelling. */
    public List<String> getMissngArgs() {
        return missingArgs;
    }

    public void setMissngArgs(List<String> missingArgs) {
        this.missingArgs = missingArgs;
    }
}
