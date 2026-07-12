package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiAgentPlanTaskEntity;
import com.zw.agent.service.AiAgentPlanTaskService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * Agent计划任务表：保存todo_write生成的结构化任务清单和执行状态 前端控制器
 * </p>
 *
 * @author 智纬
 * @since 2026-07-08
 */
@RestController
@RequestMapping("/agentPlanTask")
@AllArgsConstructor
public class AiAgentPlanTaskController {
    private final AiAgentPlanTaskService aiAgentPlanTaskService;

    @GetMapping("/list")
    public Result<List<AiAgentPlanTaskEntity>> list() {
        return Result.ok(aiAgentPlanTaskService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiAgentPlanTaskEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiAgentPlanTaskService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiAgentPlanTaskEntity> getById(@PathVariable Long id) {
        return Result.ok(aiAgentPlanTaskService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiAgentPlanTaskEntity entity) {
        return Result.ok(aiAgentPlanTaskService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiAgentPlanTaskEntity entity) {
        return Result.ok(aiAgentPlanTaskService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiAgentPlanTaskService.removeById(id));
    }

}