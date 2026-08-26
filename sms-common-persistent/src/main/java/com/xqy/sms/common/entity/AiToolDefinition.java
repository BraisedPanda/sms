package com.xqy.sms.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;

/** Metadata exposed to an AI model for a tool. */
@TableName("ai_tool_definition")
public class AiToolDefinition extends BaseEntity implements Serializable {

    private String domain;

    @TableField("tool_name")
    private String toolName;

    private String description;

    @TableField("argument_specification")
    private String argumentSpecification;

    private String keywords;

    private boolean enable;

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

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}
