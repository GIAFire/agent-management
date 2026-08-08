package com.zhiran.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiran.common.context.UserContext;
import com.zhiran.agent.entity.AiToolRolePermissionEntity;
import com.zhiran.agent.service.AiToolRolePermissionService;
import com.zhiran.agent.service.ToolManagementService;
import com.zhiran.common.entity.Result;
import com.zhiran.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * Agent权限规则表：定义某个工具在不同调用模式下允许、拒绝或询问 前端控制器
 * </p>
 *
 * @author zhiRan
 * @since 2026-06-28
 */
@RestController
@RequestMapping("/toolRolePermission")
@AllArgsConstructor
public class AiToolRolePermissionController {
    private final AiToolRolePermissionService aiToolRolePermissionService;
    private final ToolManagementService toolManagementService;

    @GetMapping("/list")
    public Result<List<AiToolRolePermissionEntity>> list() {
        return Result.ok(aiToolRolePermissionService.list(
                new LambdaQueryWrapper<AiToolRolePermissionEntity>()
                        .eq(AiToolRolePermissionEntity::getTenantId, UserContext.get().getTenantId())
                        .orderByDesc(AiToolRolePermissionEntity::getUpdatedAt)));
    }

    @GetMapping("/page")
    public Result<IPage<AiToolRolePermissionEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiToolRolePermissionService.page(new Page<>(current, size),
                new LambdaQueryWrapper<AiToolRolePermissionEntity>()
                        .eq(AiToolRolePermissionEntity::getTenantId, UserContext.get().getTenantId())
                        .orderByDesc(AiToolRolePermissionEntity::getUpdatedAt)));
    }

    @GetMapping("/management/page")
    public Result<IPage<AiToolRolePermissionEntity>> managementPage(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long toolId
    ) {
        return Result.ok(toolManagementService.pagePermissions(current, size, toolId));
    }

    @PostMapping("/management/save")
    public Result<AiToolRolePermissionEntity> managementSave(
            @RequestBody AiToolRolePermissionEntity entity
    ) {
        try {
            return Result.ok(toolManagementService.savePermission(entity));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return Result.fail(exception.getMessage());
        }
    }

    @PostMapping("/management/{id}/disable")
    public Result<Boolean> managementDisable(@PathVariable Long id) {
        return Result.ok(toolManagementService.disablePermission(id));
    }

    @GetMapping("/{id}")
    public Result<AiToolRolePermissionEntity> getById(@PathVariable Long id) {
        return Result.ok(aiToolRolePermissionService.getOne(
                new LambdaQueryWrapper<AiToolRolePermissionEntity>()
                        .eq(AiToolRolePermissionEntity::getTenantId, UserContext.get().getTenantId())
                        .eq(AiToolRolePermissionEntity::getId, id)));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiToolRolePermissionEntity entity) {
        return Result.ok(aiToolRolePermissionService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiToolRolePermissionEntity entity) {
        return Result.ok(aiToolRolePermissionService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiToolRolePermissionService.removeById(id));
    }

}
