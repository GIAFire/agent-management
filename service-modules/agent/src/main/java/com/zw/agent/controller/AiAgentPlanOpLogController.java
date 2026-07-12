package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiAgentPlanOpLogEntity;
import com.zw.agent.service.AiAgentPlanOpLogService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * Agent计划操作日志表：记录plan_enter、plan_write、plan_exit、todo_write等计划相关事件 前端控制器
 * </p>
 *
 * @author 智纬
 * @since 2026-07-08
 */
@RestController
@RequestMapping("/agentPlanOpLog")
@AllArgsConstructor
public class AiAgentPlanOpLogController {
    private final AiAgentPlanOpLogService aiAgentPlanOpLogService;

    @GetMapping("/list")
    public Result<List<AiAgentPlanOpLogEntity>> list() {
        return Result.ok(aiAgentPlanOpLogService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiAgentPlanOpLogEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiAgentPlanOpLogService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiAgentPlanOpLogEntity> getById(@PathVariable Long id) {
        return Result.ok(aiAgentPlanOpLogService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiAgentPlanOpLogEntity entity) {
        return Result.ok(aiAgentPlanOpLogService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiAgentPlanOpLogEntity entity) {
        return Result.ok(aiAgentPlanOpLogService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiAgentPlanOpLogService.removeById(id));
    }

}