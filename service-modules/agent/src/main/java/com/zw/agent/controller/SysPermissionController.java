package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.SysPermissionEntity;
import com.zw.agent.service.SysPermissionService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 权限表（菜单/接口） 前端控制器
 * </p>
 *
 * @author 智纬
 * @since 2026-06-28
 */
@RestController
@RequestMapping("/sysPermission")
@AllArgsConstructor
public class SysPermissionController {
    private final SysPermissionService sysPermissionService;

    @GetMapping("/list")
    public Result<List<SysPermissionEntity>> list() {
        return Result.ok(sysPermissionService.list());
    }

    @GetMapping("/page")
    public Result<IPage<SysPermissionEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(sysPermissionService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<SysPermissionEntity> getById(@PathVariable Long id) {
        return Result.ok(sysPermissionService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody SysPermissionEntity entity) {
        return Result.ok(sysPermissionService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody SysPermissionEntity entity) {
        return Result.ok(sysPermissionService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(sysPermissionService.removeById(id));
    }

}