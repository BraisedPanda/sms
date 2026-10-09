package com.xqy.sms.common.dto;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ManagementRows {
    private ManagementRows() { }

    public static Map<String, Object> publicRow(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        row.forEach((column, value) -> {
            StringBuilder key = new StringBuilder();
            boolean upper = false;
            for (char character : column.toCharArray()) {
                if (character == '_') upper = true;
                else { key.append(upper ? Character.toUpperCase(character) : character); upper = false; }
            }
            Object safe = value instanceof Long || value instanceof BigInteger ? value.toString()
                    : value instanceof Timestamp ? ((Timestamp) value).toLocalDateTime().toString() : value;
            result.put(key.toString(), safe);
        });
        return result;
    }

    public static List<Map<String, Object>> publicRows(List<Map<String, Object>> rows) {
        return rows.stream().map(ManagementRows::publicRow).toList();
    }

    public static String text(Object value, int maximum, boolean required) {
        String text = value == null ? null : value.toString().trim();
        if (required && (text == null || text.isBlank())) throw new IllegalArgumentException("必填字段不能为空");
        if (text != null && text.codePointCount(0, text.length()) > maximum) throw new IllegalArgumentException("字段长度超过 " + maximum);
        return text;
    }

    public static long id(Object value) {
        try {
            long id = Long.parseLong(String.valueOf(value));
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException error) { throw new IllegalArgumentException("无效的 ID"); }
    }
}
