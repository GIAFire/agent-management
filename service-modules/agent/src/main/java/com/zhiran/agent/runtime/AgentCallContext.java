package com.zhiran.agent.runtime;

import com.zhiran.agent.entity.DTO.AgentConfigDTO;
import com.zhiran.common.context.UserInfo;
import lombok.Data;

@Data
public class AgentCallContext {

    UserInfo userInfo;
    AgentConfigDTO agentConfig;
    Long sessionId;
    Long runId;
    String workspacePath;
}
