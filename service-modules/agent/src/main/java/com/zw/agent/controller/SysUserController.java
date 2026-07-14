package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.SysUserEntity;
import com.zw.agent.service.SysUserService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 智纬
 * @since 2026-07-14
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/sysUser")
public class SysUserController {

    private final SysUserService sysUserService;

    @GetMapping("/list")
    public Result<List<SysUserEntity>> list() {
        return Result.ok(sysUserService.list());
    }

    @GetMapping("/page")
    public Result<IPage<SysUserEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(sysUserService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<SysUserEntity> getById(@PathVariable Long id) {
        return Result.ok(sysUserService.getById(id));
    }

    @PostMapping("/create")
    public Result<SysUserEntity> create(@RequestBody SysUserEntity entity) {
        sysUserService.save(EntityDefaults.create(entity));
        return Result.ok(entity);
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody SysUserEntity entity) {
        if (!StringUtils.hasText(entity.getPassword())) {
            SysUserEntity old = sysUserService.getById(entity.getId());
            if (old != null) {
                entity.setPassword(old.getPassword());
            }
        }
        return Result.ok(sysUserService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(sysUserService.removeById(id));
    }

}
