package com.zw.agent.event;

public record AgentRuntimeEvent(
        String eventType,
        String sseEvent,
        String delta,
        Object rawEvent
) {}