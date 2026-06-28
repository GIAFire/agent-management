package com.zw.agent.tools;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.tool.ToolCallParam;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ToolSchemaUtils {

    private ToolSchemaUtils() {
    }

    static Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", false);
        return schema;
    }

    static Map<String, Object> stringProperty(String description) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "string");
        schema.put("description", description);
        return schema;
    }

    static String requiredString(ToolCallParam param, String name) {
        String value = optionalString(param, name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required parameter: " + name);
        }
        return value;
    }

    static String optionalString(ToolCallParam param, String name) {
        Object value = input(param).get(name);
        if (value == null) {
            return null;
        }
        String stringValue = String.valueOf(value);
        return stringValue.isBlank() ? null : stringValue;
    }

    static String runtimeUserId(ToolCallParam param) {
        RuntimeContext runtimeContext = param == null ? null : param.getRuntimeContext();
        return runtimeContext == null ? null : runtimeContext.getUserId();
    }

    private static Map<String, Object> input(ToolCallParam param) {
        if (param == null || param.getInput() == null) {
            return Collections.emptyMap();
        }
        return param.getInput();
    }
}
