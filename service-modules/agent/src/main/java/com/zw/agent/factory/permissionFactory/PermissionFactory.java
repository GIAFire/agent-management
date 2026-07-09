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

    private static final String PLAN_MODE_PERMISSION_SOURCE = "planModeDefaults";
    private static final List<String> PLAN_MODE_AUTO_ALLOW_TOOLS = List.of(
            "plan_enter",
            "plan_write",
            "todo_write"
    );

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
            if (isPlanModeAutoAllowTool(config, toolName)) {
                continue;
            }
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

        addPlanModeAutoAllowRules(config, builder);
        return builder.build();
    }

    /**
     * 为 Plan Mode 内置工具补充默认放行规则。
     * plan_enter 和 plan_write 是官方计划模式白名单工具，本身只负责进入计划阶段和写计划文件；
     * todo_write 只同步结构化任务清单。它们不应该触发用户确认，否则会打断“进入计划 -> 写计划”的正常流程。
     * plan_exit 不在这里放行，继续交给 AgentScope 的 ASK/HITL 流程处理，保证退出计划模式时必须经过用户审批。
     */
    private void addPlanModeAutoAllowRules(AgentConfigDTO config, PermissionContextState.Builder builder) {
        if (!isPlanModeOrTaskListEnabled(config)) {
            return;
        }
        for (String toolName : PLAN_MODE_AUTO_ALLOW_TOOLS) {
            PermissionRule rule = new PermissionRule(
                    toolName,
                    null,
                    PermissionBehavior.ALLOW,
                    PLAN_MODE_PERMISSION_SOURCE
            );
            builder.addAllowRule(toolName, rule);
        }
    }

    /**
     * 判断当前 Agent 配置是否启用了 Plan Mode 或任务列表能力。
     * 只有这些能力打开时才注入 plan_enter、plan_write、todo_write 的默认权限，避免影响普通 Agent 的权限规则。
     */
    private boolean isPlanModeOrTaskListEnabled(AgentConfigDTO config) {
        return Objects.equals(config.getPlanModeEnabled(), 1)
                || Objects.equals(config.getTaskListEnabled(), 1);
    }

    /**
     * 判断当前工具是否属于 Plan Mode 默认放行工具。
     * 如果这些内置工具出现在 Toolkit 列表里，也跳过普通角色权限查询，避免先写入 DENY/ASK 规则后再写入 ALLOW 规则造成优先级不明确。
     */
    private boolean isPlanModeAutoAllowTool(AgentConfigDTO config, String toolName) {
        if (!isPlanModeOrTaskListEnabled(config) || toolName == null) {
            return false;
        }
        return PLAN_MODE_AUTO_ALLOW_TOOLS.contains(toolName.trim().toLowerCase(Locale.ROOT));
    }
}
