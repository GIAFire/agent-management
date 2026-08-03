package com.zhiran.agent.runtime;

import com.zhiran.agent.constant.AgentConstant;
import java.nio.file.Path;

public final class AgentWorkspaceResolver {

    private static final String WORKSPACE_HOME_PROPERTY = "agentscope.workspace.home";

    private AgentWorkspaceResolver() {
    }

    public static Path resolve(Long tenantId, Long userId, Long agentId) {
        if (tenantId == null || userId == null || agentId == null) {
            throw new IllegalArgumentException(
                    "tenantId、userId 和 agentId 不能为空");
        }
        return root()
                .resolve("tenants").resolve(String.valueOf(tenantId))
                .resolve("users").resolve(String.valueOf(userId))
                .resolve("agents").resolve(String.valueOf(agentId))
                .toAbsolutePath()
                .normalize();
    }

    public static Path root() {
        String override = System.getProperty(WORKSPACE_HOME_PROPERTY);
        String configured = override == null || override.isBlank()
                ? AgentConstant.WORK_PACE_PATH
                : override.trim();
        return Path.of(configured).toAbsolutePath().normalize();
    }
}
