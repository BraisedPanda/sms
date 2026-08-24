package com.xqy.sms.ai.model;

import java.util.ArrayList;
import java.util.List;

/** A recursive filter expression. A node may be a leaf or a logical expression. */
public class QueryFilterNode {

    private List<QueryFilterNode> and = new ArrayList<>();
    private List<QueryFilterNode> or = new ArrayList<>();
    private QueryFilterNode not;
    private String field;
    private String operator;
    private Object value;
    private List<Object> values = new ArrayList<>();

    public QueryFilterNode() {
    }

    public QueryFilterNode(List<QueryFilterNode> and, List<QueryFilterNode> or, QueryFilterNode not,
                           String field, String operator, Object value, List<Object> values) {
        this.and = and;
        this.or = or;
        this.not = not;
        this.field = field;
        this.operator = operator;
        this.value = value;
        this.values = values;
    }

    public List<QueryFilterNode> getAnd() {
        return and;
    }

    public void setAnd(List<QueryFilterNode> and) {
        this.and = and;
    }

    public List<QueryFilterNode> getOr() {
        return or;
    }

    public void setOr(List<QueryFilterNode> or) {
        this.or = or;
    }

    public QueryFilterNode getNot() {
        return not;
    }

    public void setNot(QueryFilterNode not) {
        this.not = not;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public List<Object> getValues() {
        return values;
    }

    public void setValues(List<Object> values) {
        this.values = values;
    }
}
