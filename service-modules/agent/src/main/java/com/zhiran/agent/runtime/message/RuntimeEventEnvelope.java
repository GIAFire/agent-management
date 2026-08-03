package com.zhiran.agent.runtime.message;

import com.zhiran.agent.event.AgentRuntimeEvent;

import java.util.Map;

public record RuntimeEventEnvelope(
        AgentRuntimeEvent runtimeEvent,
        Map<String, Object> planPayload
) {
}
