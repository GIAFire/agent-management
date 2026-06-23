package com.zw.agent.event;

public record AgentRuntimeEvent(
        // 事件类型
        String eventType,
        // SSE事件类型
        String sseEvent,
        // 事件内容
        String delta,
        // 原始事件对象
        Object rawEvent
) {}