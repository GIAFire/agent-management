package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiSubagentEntity;
import com.zw.agent.service.AiSubagentService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 子Agent定义表：保存可复用专家Agent的能力描述、模型、工具、知识库和安全配置 前端控制器
 * </p>
 *
 * @author 智纬
 * @since 2026-07-11
 */
@RestController
@RequestMapping("/subagent")
@AllArgsConstructor
public class AiSubagentController {
    private final AiSubagentService aiSubagentService;

    @GetMapping("/list")
    public Result<List<AiSubagentEntity>> list() {
        return Result.ok(aiSubagentService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiSubagentEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiSubagentService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiSubagentEntity> getById(@PathVariable Long id) {
        return Result.ok(aiSubagentService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiSubagentEntity entity) {
        return Result.ok(aiSubagentService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiSubagentEntity entity) {
        return Result.ok(aiSubagentService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiSubagentService.removeById(id));
    }

}