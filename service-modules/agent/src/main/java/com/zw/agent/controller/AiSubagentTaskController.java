package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiSubagentTaskEntity;
import com.zw.agent.service.AiSubagentTaskService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 子Agent任务表：记录agent_spawn/agent_send产生的同步或后台委派任务 前端控制器
 * </p>
 *
 * @author 智纬
 * @since 2026-07-11
 */
@RestController
@RequestMapping("/subagentTask")
@AllArgsConstructor
public class AiSubagentTaskController {
    private final AiSubagentTaskService aiSubagentTaskService;

    @GetMapping("/list")
    public Result<List<AiSubagentTaskEntity>> list() {
        return Result.ok(aiSubagentTaskService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiSubagentTaskEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiSubagentTaskService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiSubagentTaskEntity> getById(@PathVariable Long id) {
        return Result.ok(aiSubagentTaskService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiSubagentTaskEntity entity) {
        return Result.ok(aiSubagentTaskService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiSubagentTaskEntity entity) {
        return Result.ok(aiSubagentTaskService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiSubagentTaskService.removeById(id));
    }

}