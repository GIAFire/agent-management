package com.zw.agent.service.plan;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlanRuntimeEventTracker {

    private final Map<String, String> toolNames = new ConcurrentHashMap<>();
    private final Map<String, StringBuilder> toolInputBuffers = new ConcurrentHashMap<>();

    public void startTool(String toolCallId, String toolName) {
        if (toolCallId == null || toolCallId.isBlank()) {
            return;
        }
        toolNames.put(toolCallId, toolName);
        toolInputBuffers.put(toolCallId, new StringBuilder());
    }

    public void appendInput(String toolCallId, String delta) {
        if (toolCallId == null || toolCallId.isBlank() || delta == null || delta.isEmpty()) {
            return;
        }
        toolInputBuffers.computeIfAbsent(toolCallId, ignored -> new StringBuilder()).append(delta);
    }

    public String toolName(String toolCallId) {
        return toolNames.get(toolCallId);
    }

    public String toolInput(String toolCallId) {
        StringBuilder buffer = toolInputBuffers.get(toolCallId);
        return buffer == null ? "" : buffer.toString();
    }
}
