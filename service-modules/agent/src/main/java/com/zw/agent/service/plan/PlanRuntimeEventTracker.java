package com.zw.agent.service.plan;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlanRuntimeEventTracker {

    // 工具入参是流式增量到达的，先按 toolCallId 缓存，等 TOOL_RESULT_END 时再统一落库。
    private final Map<String, String> toolNames = new ConcurrentHashMap<>();
    private final Map<String, StringBuilder> toolInputBuffers = new ConcurrentHashMap<>();

    /**
     * 记录一次工具调用的基础信息，并初始化该调用的入参增量缓存。
     */
    public void startTool(String toolCallId, String toolName) {
        if (toolCallId == null || toolCallId.isBlank()) {
            return;
        }
        toolNames.put(toolCallId, toolName);
        toolInputBuffers.put(toolCallId, new StringBuilder());
    }

    /**
     * 追加工具调用参数的流式片段，用于后续在结果结束事件里拼成完整原始入参。
     */
    public void appendInput(String toolCallId, String delta) {
        if (toolCallId == null || toolCallId.isBlank() || delta == null || delta.isEmpty()) {
            return;
        }
        toolInputBuffers.computeIfAbsent(toolCallId, ignored -> new StringBuilder()).append(delta);
    }

    /**
     * 根据工具调用 ID 取回工具名，补偿部分结果事件里工具名为空的情况。
     */
    public String toolName(String toolCallId) {
        return toolNames.get(toolCallId);
    }

    /**
     * 根据工具调用 ID 取回已拼接的完整入参文本；没有缓存时返回空字符串。
     */
    public String toolInput(String toolCallId) {
        StringBuilder buffer = toolInputBuffers.get(toolCallId);
        return buffer == null ? "" : buffer.toString();
    }
}
