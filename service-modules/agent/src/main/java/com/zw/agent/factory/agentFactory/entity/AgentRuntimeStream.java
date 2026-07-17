package com.zw.agent.factory.agentFactory.entity;

import com.zw.agent.event.AgentRuntimeEvent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.harness.agent.HarnessAgent;
import lombok.AllArgsConstructor;
import lombok.Data;
import reactor.core.publisher.Flux;

@AllArgsConstructor
@Data
public class AgentRuntimeStream {
    HarnessAgent agent;
    RuntimeContext runtimeContext;
    Flux<AgentRuntimeEvent> runtimeEvents;
    Long userId;
    Long sessionId;

    public AgentRuntimeStream(HarnessAgent agent, Flux<AgentRuntimeEvent> runtimeEvents) {
        this.agent = agent;
        this.runtimeEvents = runtimeEvents;
    }

    public AgentRuntimeStream(HarnessAgent harnessAgent, RuntimeContext context,Flux<AgentRuntimeEvent> runtimeEvent) {
        this.agent = harnessAgent;
        this.runtimeEvents = runtimeEvent;
        this.runtimeContext = context;
    }
}
