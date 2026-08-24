package com.xqy.sms.ai.model;

import java.util.ArrayList;
import java.util.List;

/** Metadata exposed to an AI model for a tool. */
public class AiToolDefinition {

    private String domain;
    private String toolname;
    private String description;
    private String argumentSpecification;
    private List<String> keywords = new ArrayList<>();
    private boolean enable;

    public AiToolDefinition() {
    }

    public AiToolDefinition(String domain, String toolname, String description,
                            String argumentSpecification, List<String> keywords, boolean enable) {
        this.domain = domain;
        this.toolname = toolname;
        this.description = description;
        this.argumentSpecification = argumentSpecification;
        this.keywords = keywords;
        this.enable = enable;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getToolname() {
        return toolname;
    }

    public void setToolname(String toolname) {
        this.toolname = toolname;
    }

    public String getToolName() {
        return toolname;
    }

    public void setToolName(String toolName) {
        this.toolname = toolName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getArgumentSpecification() {
        return argumentSpecification;
    }

    public void setArgumentSpecification(String argumentSpecification) {
        this.argumentSpecification = argumentSpecification;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}
