package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiAgentSysPromptEntity;
import com.zw.agent.service.AiAgentSysPromptService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * Agent 定义表：保存一个可视化 Agent 的基础身份信息 前端控制器
 * </p>
 *
 * @author 智纬
 * @since 2026-07-12
 */
@AllArgsConstructor
@RestController
@RequestMapping("/sysPrompt")
public class AiAgentSysPromptController {

    private final AiAgentSysPromptService sysPromptService;

    @GetMapping("/list")
    public Result<List<AiAgentSysPromptEntity>> list() {

        return Result.ok(sysPromptService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiAgentSysPromptEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(sysPromptService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiAgentSysPromptEntity> getById(@PathVariable Long id) {
        return Result.ok(sysPromptService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiAgentSysPromptEntity entity) {
        return Result.ok(sysPromptService.save(entity));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiAgentSysPromptEntity entity) {
        return Result.ok(sysPromptService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(sysPromptService.removeById(id));
    }

}
