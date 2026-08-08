package com.zhiran.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiran.agent.entity.SysUserEntity;
import com.zhiran.agent.service.SysUserService;
import com.zhiran.common.entity.Result;
import com.zhiran.common.support.EntityDefaults;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author zhiRan
 * @since 2026-07-14
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/sysUser")
public class SysUserController {

    private final SysUserService sysUserService;
    private final PasswordEncoder passwordEncoder;

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
        entity.setPassword(passwordEncoder.encode(entity.getPassword()));
        sysUserService.save(EntityDefaults.create(entity));
        return Result.ok();
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody SysUserEntity entity) {
        if (!StringUtils.hasText(entity.getPassword())) {
            SysUserEntity old = sysUserService.getById(entity.getId());
            if (old != null) {
                entity.setPassword(old.getPassword());
            }
        }else {
            entity.setPassword(passwordEncoder.encode(entity.getPassword()));
        }
        return Result.ok(sysUserService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(sysUserService.removeById(id));
    }

}
