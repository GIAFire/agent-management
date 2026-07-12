package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.SysRoleEntity;
import com.zw.agent.service.SysRoleService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 角色表 前端控制器
 * </p>
 *
 * @author 智纬
 * @since 2026-06-28
 */
@RestController
@RequestMapping("/sysRole")
@AllArgsConstructor
public class SysRoleController {
    private final SysRoleService sysRoleService;

    @GetMapping("/list")
    public Result<List<SysRoleEntity>> list() {
        return Result.ok(sysRoleService.list());
    }

    @GetMapping("/page")
    public Result<IPage<SysRoleEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(sysRoleService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<SysRoleEntity> getById(@PathVariable Long id) {
        return Result.ok(sysRoleService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody SysRoleEntity entity) {
        return Result.ok(sysRoleService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody SysRoleEntity entity) {
        return Result.ok(sysRoleService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(sysRoleService.removeById(id));
    }

}