package com.xqy.sms.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 通用日期时间工具
 */
public class DateTimeUtils {
    private static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(DEFAULT_PATTERN));
    }

    public static LocalDateTime parse(String dateTime) {
        if (dateTime == null || dateTime.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern(DEFAULT_PATTERN));
    }
}
