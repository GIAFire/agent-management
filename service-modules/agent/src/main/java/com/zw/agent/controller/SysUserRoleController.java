package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.SysUserRoleEntity;
import com.zw.agent.service.SysUserRoleService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 *  用户角色关联表 前端控制器
 * </p>
 *
 * @author 智纬
 * @since 2026-06-28
 */
@RestController
@RequestMapping("/sysUserRole")
@AllArgsConstructor
public class SysUserRoleController {
    private final SysUserRoleService sysUserRoleService;

    @GetMapping("/list")
    public Result<List<SysUserRoleEntity>> list() {
        return Result.ok(sysUserRoleService.list());
    }

    @GetMapping("/page")
    public Result<IPage<SysUserRoleEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(sysUserRoleService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<SysUserRoleEntity> getById(@PathVariable Long id) {
        return Result.ok(sysUserRoleService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody SysUserRoleEntity entity) {
        return Result.ok(sysUserRoleService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody SysUserRoleEntity entity) {
        return Result.ok(sysUserRoleService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(sysUserRoleService.removeById(id));
    }

}