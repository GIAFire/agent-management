package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiToolRolePermissionEntity;
import com.zw.agent.service.AiToolRolePermissionService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * Agent权限规则表：定义某个工具在不同调用模式下允许、拒绝或询问 前端控制器
 * </p>
 *
 * @author 智纬
 * @since 2026-06-28
 */
@RestController
@RequestMapping("/toolRolePermission")
@AllArgsConstructor
public class AiToolRolePermissionController {
    private final AiToolRolePermissionService aiToolRolePermissionService;

    @GetMapping("/list")
    public Result<List<AiToolRolePermissionEntity>> list() {
        return Result.ok(aiToolRolePermissionService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiToolRolePermissionEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiToolRolePermissionService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiToolRolePermissionEntity> getById(@PathVariable Long id) {
        return Result.ok(aiToolRolePermissionService.getById(id));
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