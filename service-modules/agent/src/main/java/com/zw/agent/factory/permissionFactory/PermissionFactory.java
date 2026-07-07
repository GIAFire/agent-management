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

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class PermissionFactory {

    private final AiToolRolePermissionService toolRolePermissionService;


    public PermissionContextState buildPermissionContext(
            AgentConfigDTO config,
            UserInfo userInfo,
            Toolkit toolkit
    ){
        List<String> roleCodes = userInfo.getRoleCodes();

        PermissionContextState.Builder builder = PermissionContextState.builder()
                .mode(PermissionMode.valueOf(config.getPermissionMode()));

        Set<String> toolNames = toolkit.getToolNames();

        for (String toolName : toolNames) {
            List<AiToolRolePermissionEntity> permissionList = toolRolePermissionService.list(new LambdaQueryWrapper<AiToolRolePermissionEntity>()
                    .eq(AiToolRolePermissionEntity::getToolName, toolName)
                    .in(AiToolRolePermissionEntity::getRoleCode, roleCodes)
                    .eq(AiToolRolePermissionEntity::getTenantId, userInfo.getTenantId())
                    .eq(AiToolRolePermissionEntity::getStatus, (byte) 1));

            AiToolRolePermissionEntity toolRolePermission = permissionList.stream()
                    .collect(Collectors.collectingAndThen(
                            Collectors.toCollection(() -> new TreeSet<>(
                                    Comparator.comparing(AiToolRolePermissionEntity::getToolId)
                            )),
                            list -> list.isEmpty() ? null : list.first() // 取第一个
                    ));

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
                    }
                }
            }
        }

        return builder.build();
    }
}
