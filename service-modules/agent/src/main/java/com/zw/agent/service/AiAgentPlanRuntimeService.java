package com.zw.agent.service;

import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.event.AgentRuntimeEvent;
import com.zw.agent.service.plan.PlanRuntimeEventTracker;
import com.zw.common.context.UserInfo;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.harness.agent.HarnessAgent;

import java.util.Map;

public interface AiAgentPlanRuntimeService {

    PlanRuntimeEventTracker newTracker();

    Map<String, Object> handleRuntimeEvent(
            HarnessAgent agent,
            RuntimeContext runtimeContext,
            AgentRuntimeEvent runtimeEvent,
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId,
            PlanRuntimeEventTracker tracker
    );
}
