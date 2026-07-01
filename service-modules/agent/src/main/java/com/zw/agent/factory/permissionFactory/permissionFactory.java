package com.zw.agent.factory.permissionFactory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zw.agent.entity.AiToolRolePermissionEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.service.AiToolRolePermissionService;
import com.zw.common.context.UserInfo;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionMode;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.core.tool.Toolkit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Component
public class permissionFactory {

    private final AiToolRolePermissionService toolRolePermissionService;


    public PermissionContextState buildPermissionContext(
            AgentConfigDTO config,
            UserInfo userInfo,
            Toolkit toolkit
    ){
        String roleCode = String.valueOf(userInfo.getRoleCode());

        PermissionContextState.Builder builder = PermissionContextState.builder()
                .mode(PermissionMode.valueOf(config.getPermissionMode()));

        Set<String> toolNames = toolkit.getToolNames();

        for (String toolName : toolNames) {
            AiToolRolePermissionEntity toolRolePermission = toolRolePermissionService.getOne(new LambdaQueryWrapper<AiToolRolePermissionEntity>()
                    .eq(AiToolRolePermissionEntity::getToolName, toolName)
                    .eq(AiToolRolePermissionEntity::getRoleCode, roleCode)
                    .eq(AiToolRolePermissionEntity::getTenantId, userInfo.getTenantId())
                    .eq(AiToolRolePermissionEntity::getStatus, (byte) 1));

            if (toolRolePermission == null) {
                PermissionRule rule =
                        new PermissionRule(
                                toolName,
                                null,
                                PermissionBehavior.DENY,
                                "userSettings"
                        );
                builder.addDenyRule(toolName, rule);
            } else {
                String behavior = toolRolePermission.getBehavior() == null
                        ? ""
                        : toolRolePermission.getBehavior().trim().toUpperCase();
                PermissionRule rule =
                        new PermissionRule(
                                toolName,
                                toolRolePermission.getRuleContent(),
                                PermissionBehavior.fromString(behavior),
                                "userSettings"
                        );

                switch (behavior) {
                    case "ALLOW" -> builder.addAllowRule(toolName, rule);
                    case "DENY" -> builder.addDenyRule(toolName, rule);
                    case "ASK" -> builder.addAskRule(toolName, rule);
                    default -> {
                        // PASSTHROUGH 不建议作为显式配置规则使用
                    }
                }
            }
        }

        return builder.build();
    }
}
