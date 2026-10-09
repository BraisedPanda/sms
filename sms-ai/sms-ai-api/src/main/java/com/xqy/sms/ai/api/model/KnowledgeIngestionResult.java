package com.xqy.sms.ai.api.model;

import java.io.Serializable;

public class KnowledgeIngestionResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private int selected;
    private int imported;
    private int indexRevision;
    private java.util.List<String> jobIds = java.util.List.of();
    private String status;

    public KnowledgeIngestionResult() { }

    public KnowledgeIngestionResult(int selected, int imported, int indexRevision) {
        this.selected = selected;
        this.imported = imported;
        this.indexRevision = indexRevision;
    }

    public int getSelected() { return selected; }
    public void setSelected(int selected) { this.selected = selected; }
    public int getImported() { return imported; }
    public void setImported(int imported) { this.imported = imported; }
    public int getIndexRevision() { return indexRevision; }
    public void setIndexRevision(int indexRevision) { this.indexRevision = indexRevision; }
    public java.util.List<String> getJobIds() { return jobIds; }
    public java.util.List<String> jobIds() { return jobIds; }
    public void setJobIds(java.util.List<String> jobIds) { this.jobIds = jobIds; }
    public String getStatus() { return status; }
    public String status() { return status; }
    public void setStatus(String status) { this.status = status; }

    /** Compatibility accessors retained for existing RPC callers. */
    public int selected() { return selected; }
    public int imported() { return imported; }
    public int indexRevision() { return indexRevision; }
}
