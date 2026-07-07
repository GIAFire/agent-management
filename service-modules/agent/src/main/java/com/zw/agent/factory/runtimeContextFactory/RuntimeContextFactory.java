package com.zw.agent.factory.runtimeContextFactory;

import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.runtime.AgentCallContext;
import com.zw.agent.runtime.AgentRuntimeKeys;
import com.zw.agent.service.AiToolRolePermissionService;
import com.zw.common.context.UserInfo;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.core.state.AgentState;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class RuntimeContextFactory {


    public RuntimeContext buildRuntimeContext(AgentConfigDTO config,
                                               UserInfo userInfo,
                                               Long sessionId,
                                               Long runId,
                                               String userKey,
                                               String sessionKey) {

        RuntimeContext.Builder builder = RuntimeContext.builder()
                .userId(userKey)
                .sessionId(sessionKey);
        AgentCallContext agentCallContext = new AgentCallContext();
        agentCallContext.setAgentConfig(config);
        agentCallContext.setUserInfo(userInfo);
        agentCallContext.setSessionId(sessionId);
        agentCallContext.setRunId(runId);
        agentCallContext.setRuntimeUserKey(userKey);
        agentCallContext.setRuntimeSessionKey(sessionKey);
        builder.put(AgentCallContext.class, agentCallContext);

        return builder.build();
    }
}
