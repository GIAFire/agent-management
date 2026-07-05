package com.zw.agent.runtime;

import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.common.context.UserInfo;
import lombok.Data;

@Data
public class AgentCallContext {

    UserInfo userInfo;
    AgentConfigDTO agentConfig;
    Long sessionId;
    Long runId;
    String workspacePath;
    String runtimeUserKey;
    String runtimeSessionKey;
}
