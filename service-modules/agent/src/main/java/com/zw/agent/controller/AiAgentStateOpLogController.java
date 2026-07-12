package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiAgentStateOpLogEntity;
import com.zw.agent.service.AiAgentStateOpLogService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * AgentState操作日志表：记录状态加载、保存、压缩、清理等操作 前端控制器
 * </p>
 *
 * @author 智纬
 * @since 2026-07-04
 */
@RestController
@RequestMapping("/agentStateOpLog")
@AllArgsConstructor
public class AiAgentStateOpLogController {
    private final AiAgentStateOpLogService aiAgentStateOpLogService;

    @GetMapping("/list")
    public Result<List<AiAgentStateOpLogEntity>> list() {
        return Result.ok(aiAgentStateOpLogService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiAgentStateOpLogEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiAgentStateOpLogService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiAgentStateOpLogEntity> getById(@PathVariable Long id) {
        return Result.ok(aiAgentStateOpLogService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiAgentStateOpLogEntity entity) {
        return Result.ok(aiAgentStateOpLogService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiAgentStateOpLogEntity entity) {
        return Result.ok(aiAgentStateOpLogService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiAgentStateOpLogService.removeById(id));
    }

}