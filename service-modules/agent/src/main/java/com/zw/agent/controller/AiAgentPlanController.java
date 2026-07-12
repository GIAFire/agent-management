package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiAgentPlanEntity;
import com.zw.agent.service.AiAgentPlanService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * Agent计划表：保存Plan Mode生成的计划元数据、内容快照、审批和执行状态 前端控制器
 * </p>
 *
 * @author 智纬
 * @since 2026-07-08
 */
@RestController
@RequestMapping("/agentPlan")
@AllArgsConstructor
public class AiAgentPlanController {
    private final AiAgentPlanService aiAgentPlanService;

    @GetMapping("/list")
    public Result<List<AiAgentPlanEntity>> list() {
        return Result.ok(aiAgentPlanService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiAgentPlanEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiAgentPlanService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiAgentPlanEntity> getById(@PathVariable Long id) {
        return Result.ok(aiAgentPlanService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiAgentPlanEntity entity) {
        return Result.ok(aiAgentPlanService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiAgentPlanEntity entity) {
        return Result.ok(aiAgentPlanService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiAgentPlanService.removeById(id));
    }

}