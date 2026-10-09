package com.xqy.sms.common.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** Stable, Hessian-compatible page contract; IDs in rows are strings. */
public class PageResult implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<Map<String, Object>> records = List.of();
    private long current;
    private long size;
    private long total;

    public PageResult() { }
    public PageResult(List<Map<String, Object>> records, long current, long size, long total) {
        this.records = records; this.current = current; this.size = size; this.total = total;
    }
    public List<Map<String, Object>> getRecords() { return records; }
    public void setRecords(List<Map<String, Object>> records) { this.records = records; }
    public long getCurrent() { return current; }
    public void setCurrent(long current) { this.current = current; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
}
