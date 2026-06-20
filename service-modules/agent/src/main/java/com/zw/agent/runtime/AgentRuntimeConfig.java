package com.zw.agent.runtime;

public record AgentRuntimeConfig(
        Long tenantId,
        Long agentId,
        Long versionNo,
        String agentName,
        String sysPrompt,
        String provider,
        String modelName,
        String workspacePath,
        int compactionTriggerMessages,
        int compactionKeepMessages
) {}