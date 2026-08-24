package com.xqy.sms.ai.model;

/** Paging and filtering criteria supplied to an AI tool. */
public class QueryCriteria {

    private int limit;
    private QueryFilterNode filter;

    public QueryCriteria() {
    }

    public QueryCriteria(int limit, QueryFilterNode filter) {
        this.limit = limit;
        this.filter = filter;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public QueryFilterNode getFilter() {
        return filter;
    }

    public void setFilter(QueryFilterNode filter) {
        this.filter = filter;
    }
}
