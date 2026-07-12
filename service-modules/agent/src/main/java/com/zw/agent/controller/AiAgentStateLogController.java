package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiAgentStateLogEntity;
import com.zw.agent.service.AiAgentStateLogService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * AgentState状态引用表：记录AgentScope运行时状态在外部存储中的位置和元数据 前端控制器
 * </p>
 *
 * @author 智纬
 * @since 2026-07-04
 */
@RestController
@RequestMapping("/agentStateLog")
@AllArgsConstructor
public class AiAgentStateLogController {
    private final AiAgentStateLogService aiAgentStateLogService;

    @GetMapping("/list")
    public Result<List<AiAgentStateLogEntity>> list() {
        return Result.ok(aiAgentStateLogService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiAgentStateLogEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiAgentStateLogService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiAgentStateLogEntity> getById(@PathVariable Long id) {
        return Result.ok(aiAgentStateLogService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiAgentStateLogEntity entity) {
        return Result.ok(aiAgentStateLogService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiAgentStateLogEntity entity) {
        return Result.ok(aiAgentStateLogService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiAgentStateLogService.removeById(id));
    }

}