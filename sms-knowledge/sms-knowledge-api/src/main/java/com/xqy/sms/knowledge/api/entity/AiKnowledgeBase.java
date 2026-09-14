package com.xqy.sms.knowledge.api.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xqy.sms.common.entity.BaseEntity;
import java.io.Serializable;

@TableName("ai_knowledge_base")
public class AiKnowledgeBase extends BaseEntity implements Serializable {
    private String name;
    private String description;
    @TableField("embedding_model_alias")
    private String embeddingModelAlias;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private Integer topk;
    private Double similarityThreshold;
    private Boolean enabled;
    private String remark;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDercription() { return description; }
    public void setDercription(String dercription) { this.description = dercription; }
    public String getEmbeddingModelAlias() { return embeddingModelAlias; }
    public void setEmbeddingModelAlias(String embeddingModelAlias) { this.embeddingModelAlias = embeddingModelAlias; }
    public Integer getChunkSize() { return chunkSize; }
    public void setChunkSize(Integer chunkSize) { this.chunkSize = chunkSize; }
    public Integer getChunkOverlap() { return chunkOverlap; }
    public void setChunkOverlap(Integer chunkOverlap) { this.chunkOverlap = chunkOverlap; }
    public Integer getTopk() { return topk; }
    public void setTopk(Integer topk) { this.topk = topk; }
    public Integer getTopK() { return topk; }
    public void setTopK(Integer topK) { this.topk = topK; }
    public Double getSimilarityThreshold() { return similarityThreshold; }
    public void setSimilarityThreshold(Double similarityThreshold) { this.similarityThreshold = similarityThreshold; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
