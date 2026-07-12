package com.zw.common.utils;

public class DefaultValue {

    public static Integer boolToInt(Boolean value, Integer defaultValue) {
        return value == null ? defaultValue : (value ? 1 : 0);
    }

    public static Integer defaultInt(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    public static Long defaultLong(Long value, Long defaultValue) {
        return value == null ? defaultValue : value;
    }

    public static String firstText(String value, String defaultValue) {
        return hasText(value) ? value : defaultValue;
    }

    public static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
