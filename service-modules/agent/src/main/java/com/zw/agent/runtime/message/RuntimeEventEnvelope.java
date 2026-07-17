package com.zw.agent.runtime.message;

import com.zw.agent.event.AgentRuntimeEvent;

import java.util.Map;

public record RuntimeEventEnvelope(
        AgentRuntimeEvent runtimeEvent,
        Map<String, Object> planPayload
) {
}
