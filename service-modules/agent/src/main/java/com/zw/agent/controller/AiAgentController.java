package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiAgentEntity;
import com.zw.agent.service.AiAgentService;
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
@RequestMapping("/agent")
@AllArgsConstructor
public class AiAgentController {
    private final AiAgentService aiAgentService;

    @GetMapping("/list")
    public Result<List<AiAgentEntity>> list() {
        return Result.ok(aiAgentService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiAgentEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiAgentService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiAgentEntity> getById(@PathVariable Long id) {
        return Result.ok(aiAgentService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiAgentEntity entity) {
        return Result.ok(aiAgentService.save(entity));
    }

    @PutMapping("/update")
    public Result<Boolean> update(@RequestBody AiAgentEntity entity) {
        return Result.ok(aiAgentService.updateById(entity));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiAgentService.removeById(id));
    }

}
