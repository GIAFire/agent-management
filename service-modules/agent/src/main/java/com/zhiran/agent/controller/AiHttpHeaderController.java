package com.zhiran.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiran.common.context.UserContext;
import com.zhiran.agent.entity.AiHttpHeaderEntity;
import com.zhiran.agent.service.AiHttpHeaderService;
import com.zhiran.common.entity.Result;
import com.zhiran.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * HTTP请求头配置表 前端控制器
 * </p>
 *
 * @author zhiRan
 * @since 2026-07-26
 */
@AllArgsConstructor
@RestController
@RequestMapping("/httpHeader")
public class AiHttpHeaderController {

    private final AiHttpHeaderService httpHeaderService;

    @GetMapping("/list")
    public Result<List<AiHttpHeaderEntity>> list() {
        return Result.ok(httpHeaderService.list(new LambdaQueryWrapper<AiHttpHeaderEntity>()
                .eq(AiHttpHeaderEntity::getTenantId, UserContext.get().getTenantId())));
    }

    @GetMapping("/page")
    public Result<IPage<AiHttpHeaderEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(httpHeaderService.page(new Page<>(current, size),
                new LambdaQueryWrapper<AiHttpHeaderEntity>()
                        .eq(AiHttpHeaderEntity::getTenantId, UserContext.get().getTenantId())));
    }

    @GetMapping("/{id}")
    public Result<AiHttpHeaderEntity> getById(@PathVariable Long id) {
        return Result.ok(httpHeaderService.getOne(new LambdaQueryWrapper<AiHttpHeaderEntity>()
                .eq(AiHttpHeaderEntity::getTenantId, UserContext.get().getTenantId())
                .eq(AiHttpHeaderEntity::getId, id)));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiHttpHeaderEntity entity) {
        entity.setId(null);
        entity.setTenantId(UserContext.get().getTenantId());
        return Result.ok(httpHeaderService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiHttpHeaderEntity entity) {
        if (entity.getId() == null) {
            return Result.ok(false);
        }
        Long tenantId = UserContext.get().getTenantId();
        entity.setTenantId(tenantId);
        return Result.ok(httpHeaderService.update(
                EntityDefaults.update(entity),
                new LambdaQueryWrapper<AiHttpHeaderEntity>()
                        .eq(AiHttpHeaderEntity::getTenantId, tenantId)
                        .eq(AiHttpHeaderEntity::getId, entity.getId())
        ));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(httpHeaderService.remove(new LambdaQueryWrapper<AiHttpHeaderEntity>()
                .eq(AiHttpHeaderEntity::getTenantId, UserContext.get().getTenantId())
                .eq(AiHttpHeaderEntity::getId, id)));
    }


}
