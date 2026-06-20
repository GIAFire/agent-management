package com.zw.agent.runtime;

public record AgentRuntimeConfig(
        Long tenantId,
        Long agentId,
        Long configId,
        String agentName,
        String sysPrompt,
        String provider,
        String modelName,
        String workspacePath,
        int compactionTriggerMessages,
        int compactionKeepMessages
) {}