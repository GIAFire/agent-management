package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiAgentConfigEntity;
import com.zw.agent.service.AiAgentConfigService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * Agent 配置表：保存每次可视化配置发布后的不可变快照 前端控制器
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@RestController
@RequestMapping("/agentConfig")
@AllArgsConstructor
public class AiAgentConfigController {
    private final AiAgentConfigService aiAgentConfigService;

    @GetMapping("/list")
    public Result<List<AiAgentConfigEntity>> list() {
        return Result.ok(aiAgentConfigService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiAgentConfigEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiAgentConfigService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiAgentConfigEntity> getById(@PathVariable Long id) {
        return Result.ok(aiAgentConfigService.getById(id));
    }

    @PostMapping
    public Result<Boolean> create(@RequestBody AiAgentConfigEntity entity) {
        return Result.ok(aiAgentConfigService.save(EntityDefaults.create(entity)));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody AiAgentConfigEntity entity) {
        return Result.ok(aiAgentConfigService.updateById(EntityDefaults.update(entity)));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiAgentConfigService.removeById(id));
    }

}
