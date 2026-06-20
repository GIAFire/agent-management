package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiAgentDefinitionEntity;
import com.zw.agent.service.AiAgentDefinitionService;
import com.zw.common.entity.Result;

import java.util.List;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * Agent 定义表：保存一个可视化 Agent 的基础身份信息 前端控制器
 * </p>
 *
 * @author
 * @since 2026-06-20
 */
@RestController
@RequestMapping("/agentDefinition")
@AllArgsConstructor
public class AiAgentDefinitionController {
    private final AiAgentDefinitionService aiAgentDefinitionService;

    @GetMapping("/list")
    public Result<List<AiAgentDefinitionEntity>> list() {
        return Result.ok(aiAgentDefinitionService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiAgentDefinitionEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiAgentDefinitionService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiAgentDefinitionEntity> getById(@PathVariable Long id) {
        return Result.ok(aiAgentDefinitionService.getById(id));
    }

    @PostMapping
    public Result<Boolean> create(@RequestBody AiAgentDefinitionEntity entity) {
        return Result.ok(aiAgentDefinitionService.save(entity));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody AiAgentDefinitionEntity entity) {
        return Result.ok(aiAgentDefinitionService.updateById(entity));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiAgentDefinitionService.removeById(id));
    }

}
