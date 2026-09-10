package com.xqy.sms.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

/** Database metadata used to build a provider-specific chat model handle. */
@TableName("ai_model_definition")
public class AiModelDefinition extends BaseEntity implements Serializable {

    private String provider;

    @TableField("model_name")
    private String modelName;

    @TableField("base_url")
    private String baseUrl;

    /** Environment variable name or an explicit key value for local-only use. */
    @TableField("api_key")
    private String apiKey;

    private String alias;

    private String capabilities;

    @TableField("fallback_alias")
    private String fallbackAlias;

    private String enabled;

    private String remark;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(String capabilities) {
        this.capabilities = capabilities;
    }

    /** Compatibility accessor for the original field spelling in the requirements. */
    public String getCapalities() {
        return capabilities;
    }

    /** Compatibility mutator for the original field spelling in the requirements. */
    public void setCapalities(String capalities) {
        this.capabilities = capalities;
    }

    public String getFallbackAlias() {
        return fallbackAlias;
    }

    public void setFallbackAlias(String fallbackAlias) {
        this.fallbackAlias = fallbackAlias;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
