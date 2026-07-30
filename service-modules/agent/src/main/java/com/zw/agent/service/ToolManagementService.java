package com.zw.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiToolCallLogEntity;
import com.zw.agent.entity.AiToolGroupConfigEntity;
import com.zw.agent.entity.AiToolInfoConfigEntity;
import com.zw.agent.entity.AiToolRolePermissionEntity;
import com.zw.agent.entity.SysRoleEntity;
import com.zw.agent.entity.DTO.ToolCallLogResponse;
import com.zw.agent.entity.DTO.ToolMetricsResponse;
import com.zw.common.context.UserContext;
import com.zw.common.context.UserInfo;
import com.zw.common.support.EntityDefaults;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ToolManagementService {

    private static final Set<String> BEHAVIORS = Set.of("ALLOW", "DENY", "ASK", "PASSTHROUGH");

    private final AiToolInfoConfigService toolService;
    private final AiToolGroupConfigService groupService;
    private final AiToolCallLogService callLogService;
    private final AiToolRolePermissionService permissionService;
    private final SysRoleService roleService;

    public List<AiToolInfoConfigEntity> listTools() {
        Long tenantId = currentTenantId();
        return toolService.list(new LambdaQueryWrapper<AiToolInfoConfigEntity>()
                .eq(AiToolInfoConfigEntity::getTenantId, tenantId)
                .orderByDesc(AiToolInfoConfigEntity::getUpdatedAt)
                .orderByAsc(AiToolInfoConfigEntity::getToolName));
    }

    public ToolMetricsResponse metrics() {
        Long tenantId = currentTenantId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime yesterdaySameTime = now.minusDays(1);

        long availableTools = toolService.count(new LambdaQueryWrapper<AiToolInfoConfigEntity>()
                .eq(AiToolInfoConfigEntity::getTenantId, tenantId));
        long enabledTools = toolService.count(new LambdaQueryWrapper<AiToolInfoConfigEntity>()
                .eq(AiToolInfoConfigEntity::getTenantId, tenantId)
                .eq(AiToolInfoConfigEntity::getEnabled, true));
        long enabledGroups = groupService.count(new LambdaQueryWrapper<AiToolGroupConfigEntity>()
                .eq(AiToolGroupConfigEntity::getTenantId, tenantId)
                .eq(AiToolGroupConfigEntity::getEnabled, true));
        long todayCalls = countCalls(tenantId, todayStart, now, null);
        long yesterdayCalls = countCalls(tenantId, yesterdayStart, yesterdaySameTime, null);
        long successes = countCalls(tenantId, todayStart, now, "SUCCESS");
        long failures = countCalls(tenantId, todayStart, now, "FAILED");

        Double changePercent = yesterdayCalls == 0
                ? null
                : percentage(todayCalls - yesterdayCalls, yesterdayCalls);
        long executedCalls = successes + failures;
        Double successRate = executedCalls == 0 ? null : percentage(successes, executedCalls);
        return new ToolMetricsResponse(
                availableTools,
                enabledTools,
                enabledGroups,
                todayCalls,
                changePercent,
                successRate,
                failures
        );
    }

    public IPage<ToolCallLogResponse> pageCallLogs(
            long current,
            long size,
            Long toolId,
            String successStatus
    ) {
        Long tenantId = currentTenantId();
        LambdaQueryWrapper<AiToolCallLogEntity> query = new LambdaQueryWrapper<AiToolCallLogEntity>()
                .eq(AiToolCallLogEntity::getTenantId, tenantId);
        if (toolId != null) {
            query.eq(AiToolCallLogEntity::getToolId, toolId);
        }
        if (StringUtils.hasText(successStatus)) {
            String normalizedStatus = successStatus.trim().toUpperCase(Locale.ROOT);
            query.in(AiToolCallLogEntity::getSuccessStatus,
                    normalizedStatus,
                    normalizedStatus.toLowerCase(Locale.ROOT));
        }
        query.orderByDesc(AiToolCallLogEntity::getStartedAt)
                .orderByDesc(AiToolCallLogEntity::getCreatedAt);
        return callLogService.page(new Page<>(sanitizePage(current), sanitizeSize(size)), query)
                .convert(this::toCallLogResponse);
    }

    public IPage<AiToolRolePermissionEntity> pagePermissions(long current, long size, Long toolId) {
        Long tenantId = currentTenantId();
        LambdaQueryWrapper<AiToolRolePermissionEntity> query =
                new LambdaQueryWrapper<AiToolRolePermissionEntity>()
                        .eq(AiToolRolePermissionEntity::getTenantId, tenantId);
        if (toolId != null) {
            query.eq(AiToolRolePermissionEntity::getToolId, toolId);
        }
        query.orderByDesc(AiToolRolePermissionEntity::getUpdatedAt)
                .orderByDesc(AiToolRolePermissionEntity::getCreatedAt);
        return permissionService.page(new Page<>(sanitizePage(current), sanitizeSize(size)), query);
    }

    @Transactional
    public AiToolRolePermissionEntity savePermission(AiToolRolePermissionEntity request) {
        if (request == null || request.getToolId() == null || request.getRoleId() == null) {
            throw new IllegalArgumentException("toolId and roleId must not be null");
        }
        Long tenantId = currentTenantId();
        AiToolInfoConfigEntity tool = toolService.getOne(
                new LambdaQueryWrapper<AiToolInfoConfigEntity>()
                        .eq(AiToolInfoConfigEntity::getTenantId, tenantId)
                        .eq(AiToolInfoConfigEntity::getId, request.getToolId()),
                false
        );
        if (tool == null) {
            throw new IllegalArgumentException("Tool does not exist in the current tenant");
        }
        SysRoleEntity role = roleService.getOne(
                new LambdaQueryWrapper<SysRoleEntity>()
                        .eq(SysRoleEntity::getTenantId, tenantId)
                        .eq(SysRoleEntity::getId, request.getRoleId()),
                false
        );
        if (role == null) {
            throw new IllegalArgumentException("Role does not exist in the current tenant");
        }
        String behavior = normalizeBehavior(request.getBehavior());
        AiToolRolePermissionEntity existing = permissionService.getOne(
                new LambdaQueryWrapper<AiToolRolePermissionEntity>()
                        .eq(AiToolRolePermissionEntity::getTenantId, tenantId)
                        .eq(AiToolRolePermissionEntity::getToolId, request.getToolId())
                        .eq(AiToolRolePermissionEntity::getRoleId, request.getRoleId()),
                false
        );
        if (request.getId() == null && existing != null && Integer.valueOf(1).equals(existing.getStatus())) {
            throw new IllegalArgumentException("An active permission already exists for this tool and role");
        }
        AiToolRolePermissionEntity target = existing == null ? new AiToolRolePermissionEntity() : existing;
        if (request.getId() != null) {
            target = permissionService.getOne(
                    new LambdaQueryWrapper<AiToolRolePermissionEntity>()
                            .eq(AiToolRolePermissionEntity::getTenantId, tenantId)
                            .eq(AiToolRolePermissionEntity::getId, request.getId()),
                    false
            );
            if (target == null) {
                throw new IllegalArgumentException("Permission does not exist in the current tenant");
            }
            if (!request.getToolId().equals(target.getToolId())
                    || !request.getRoleId().equals(target.getRoleId())) {
                throw new IllegalArgumentException("The tool and role of an existing permission cannot be changed");
            }
        }
        target.setTenantId(tenantId);
        target.setToolId(tool.getId());
        target.setToolName(tool.getToolName());
        target.setRoleId(role.getId());
        target.setRoleCode(role.getRoleCode());
        target.setBehavior(behavior);
        target.setRuleContent(request.getRuleContent());
        target.setDescription(request.getDescription());
        target.setSource("admin");
        int status = request.getStatus() == null ? 1 : request.getStatus();
        if (status != 0 && status != 1) {
            throw new IllegalArgumentException("status must be 0 or 1");
        }
        target.setStatus(status);
        if (target.getId() == null) {
            permissionService.save(EntityDefaults.create(target));
        } else {
            permissionService.updateById(EntityDefaults.update(target));
        }
        return target;
    }

    @Transactional
    public boolean disablePermission(Long id) {
        Long tenantId = currentTenantId();
        AiToolRolePermissionEntity existing = permissionService.getOne(
                new LambdaQueryWrapper<AiToolRolePermissionEntity>()
                        .eq(AiToolRolePermissionEntity::getTenantId, tenantId)
                        .eq(AiToolRolePermissionEntity::getId, id),
                false
        );
        if (existing == null) {
            return false;
        }
        existing.setStatus(0);
        return permissionService.updateById(EntityDefaults.update(existing));
    }

    private long countCalls(
            Long tenantId,
            LocalDateTime start,
            LocalDateTime end,
            String successStatus
    ) {
        LambdaQueryWrapper<AiToolCallLogEntity> query = new LambdaQueryWrapper<AiToolCallLogEntity>()
                .eq(AiToolCallLogEntity::getTenantId, tenantId)
                .ge(AiToolCallLogEntity::getStartedAt, start)
                .lt(AiToolCallLogEntity::getStartedAt, end);
        if (successStatus != null) {
            query.in(AiToolCallLogEntity::getSuccessStatus,
                    successStatus,
                    successStatus.toLowerCase(Locale.ROOT));
        }
        return callLogService.count(query);
    }

    private ToolCallLogResponse toCallLogResponse(AiToolCallLogEntity entity) {
        return new ToolCallLogResponse(
                entity.getId(),
                entity.getRunId(),
                entity.getSessionId(),
                entity.getAgentId(),
                entity.getToolId(),
                entity.getToolName(),
                entity.getToolCallId(),
                entity.getPermissionBehavior(),
                entity.getSuccessStatus(),
                entity.getStartedAt(),
                entity.getEndedAt(),
                entity.getDurationMs()
        );
    }

    private String normalizeBehavior(String behavior) {
        String normalized = behavior == null ? "" : behavior.trim().toUpperCase(Locale.ROOT);
        if (!BEHAVIORS.contains(normalized)) {
            throw new IllegalArgumentException("behavior must be ALLOW, DENY, ASK or PASSTHROUGH");
        }
        return normalized;
    }

    private Double percentage(long numerator, long denominator) {
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private long sanitizePage(long current) {
        return Math.max(1, current);
    }

    private long sanitizeSize(long size) {
        return Math.min(100, Math.max(1, size));
    }

    private Long currentTenantId() {
        UserInfo userInfo = UserContext.get();
        if (userInfo == null || userInfo.getTenantId() == null) {
            throw new IllegalStateException("Authenticated tenant context is required");
        }
        return userInfo.getTenantId();
    }

}
