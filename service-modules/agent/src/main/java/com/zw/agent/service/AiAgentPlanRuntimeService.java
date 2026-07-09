package com.zw.agent.service;

import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.event.AgentRuntimeEvent;
import com.zw.agent.service.plan.PlanRuntimeEventTracker;
import com.zw.common.context.UserInfo;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.harness.agent.HarnessAgent;

import java.util.Map;

public interface AiAgentPlanRuntimeService {

    /**
     * 创建一次 Agent 流式调用内使用的 Plan 事件跟踪器，用于缓存工具名和工具入参增量。
     */
    PlanRuntimeEventTracker newTracker();

    /**
     * 处理 AgentScope 原始运行事件：识别 plan_enter、plan_write、plan_exit、todo_write，
     * 同步业务表状态，并返回可追加到 SSE payload 的计划/任务快照。
     */
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
