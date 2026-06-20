package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiAgentVersionEntity;
import com.zw.agent.service.AiAgentVersionService;
import com.zw.common.entity.Result;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * Agent 版本表：保存每次可视化配置发布后的不可变快照 前端控制器
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@RestController
@RequestMapping("/agentVersion")
@AllArgsConstructor
public class AiAgentVersionController {
    private final AiAgentVersionService aiAgentVersionService;

    @GetMapping("/list")
    public Result<List<AiAgentVersionEntity>> list() {
        return Result.ok(aiAgentVersionService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiAgentVersionEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiAgentVersionService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiAgentVersionEntity> getById(@PathVariable Long id) {
        return Result.ok(aiAgentVersionService.getById(id));
    }

    @PostMapping
    public Result<Boolean> create(@RequestBody AiAgentVersionEntity entity) {
        return Result.ok(aiAgentVersionService.save(entity));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody AiAgentVersionEntity entity) {
        return Result.ok(aiAgentVersionService.updateById(entity));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiAgentVersionService.removeById(id));
    }

}
