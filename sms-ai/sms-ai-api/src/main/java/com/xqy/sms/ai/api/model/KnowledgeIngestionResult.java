package com.xqy.sms.ai.api.model;

import java.io.Serializable;

public class KnowledgeIngestionResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private int selected;
    private int imported;
    private int indexRevision;

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

    /** Compatibility accessors retained for existing RPC callers. */
    public int selected() { return selected; }
    public int imported() { return imported; }
    public int indexRevision() { return indexRevision; }
}
