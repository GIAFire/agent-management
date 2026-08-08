package com.zhiran.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiran.agent.entity.AiSubagentTaskEntity;
import com.zhiran.agent.service.AiSubagentTaskService;
import com.zhiran.common.context.UserContext;
import com.zhiran.common.entity.Result;
import com.zhiran.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 子Agent任务表：记录agent_spawn/agent_send产生的同步或后台委派任务 前端控制器
 * </p>
 *
 * @author zhiRan
 * @since 2026-07-11
 */
@RestController
@RequestMapping("/subagentTask")
@AllArgsConstructor
public class AiSubagentTaskController {
    private final AiSubagentTaskService aiSubagentTaskService;

    @GetMapping("/list")
    public Result<List<AiSubagentTaskEntity>> list() {
        return Result.ok(aiSubagentTaskService.list(new LambdaQueryWrapper<AiSubagentTaskEntity>()
                .eq(AiSubagentTaskEntity::getTenantId, UserContext.get().getTenantId())));
    }

    @GetMapping("/page")
    public Result<IPage<AiSubagentTaskEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiSubagentTaskService.page(
                new Page<>(current, size),
                new LambdaQueryWrapper<AiSubagentTaskEntity>()
                        .eq(AiSubagentTaskEntity::getTenantId, UserContext.get().getTenantId())
        ));
    }

    @GetMapping("/{id}")
    public Result<AiSubagentTaskEntity> getById(@PathVariable Long id) {
        return Result.ok(aiSubagentTaskService.getOne(new LambdaQueryWrapper<AiSubagentTaskEntity>()
                .eq(AiSubagentTaskEntity::getTenantId, UserContext.get().getTenantId())
                .eq(AiSubagentTaskEntity::getId, id)));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiSubagentTaskEntity entity) {
        entity.setId(null);
        entity.setTenantId(UserContext.get().getTenantId());
        return Result.ok(aiSubagentTaskService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiSubagentTaskEntity entity) {
        if (entity.getId() == null) {
            return Result.ok(false);
        }
        Long tenantId = UserContext.get().getTenantId();
        entity.setTenantId(tenantId);
        return Result.ok(aiSubagentTaskService.update(
                EntityDefaults.update(entity),
                new LambdaQueryWrapper<AiSubagentTaskEntity>()
                        .eq(AiSubagentTaskEntity::getTenantId, tenantId)
                        .eq(AiSubagentTaskEntity::getId, entity.getId())
        ));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiSubagentTaskService.remove(new LambdaQueryWrapper<AiSubagentTaskEntity>()
                .eq(AiSubagentTaskEntity::getTenantId, UserContext.get().getTenantId())
                .eq(AiSubagentTaskEntity::getId, id)));
    }

}
