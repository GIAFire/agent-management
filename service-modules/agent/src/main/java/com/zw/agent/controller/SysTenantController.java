package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.SysTenantEntity;
import com.zw.agent.service.SysTenantService;
import com.zw.common.entity.Result;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 租户表：平台多租户隔离的根表 前端控制器
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@RestController
@RequestMapping("/tenant")
@AllArgsConstructor
public class SysTenantController {
    private final SysTenantService aiTenantService;

    @GetMapping("/list")
    public Result<List<SysTenantEntity>> list() {
        return Result.ok(aiTenantService.list());
    }

    @GetMapping("/page")
    public Result<IPage<SysTenantEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiTenantService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<SysTenantEntity> getById(@PathVariable Long id) {
        return Result.ok(aiTenantService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody SysTenantEntity entity) {
        return Result.ok(aiTenantService.save(entity));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody SysTenantEntity entity) {
        return Result.ok(aiTenantService.updateById(entity));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiTenantService.removeById(id));
    }

}
