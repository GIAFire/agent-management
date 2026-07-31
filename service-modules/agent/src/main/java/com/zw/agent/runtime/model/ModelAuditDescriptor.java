package com.zw.agent.runtime.model;

import com.zw.common.context.UserInfo;
import java.util.List;

public record ModelAuditDescriptor(
        UserInfo userInfo,
        Long modelConfigId,
        Long agentId,
        Long agentConfigId,
        String configName,
        String providerName,
        String protocol,
        String modelName,
        String sourcePath,
        List<String> secretValues
) {
}
