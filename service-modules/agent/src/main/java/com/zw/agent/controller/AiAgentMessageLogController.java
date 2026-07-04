package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiAgentMessageLogEntity;
import com.zw.agent.service.AiAgentMessageLogService;
import com.zw.common.entity.Result;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * Agent 消息表：保存用户输入、Agent 回复、工具消息等完整上下文 前端控制器
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@RestController
@RequestMapping("/agentMessage")
@AllArgsConstructor
public class AiAgentMessageLogController {
    private final AiAgentMessageLogService aiAgentMessageLogService;

    @GetMapping("/list")
    public Result<List<AiAgentMessageLogEntity>> list() {
        return Result.ok(aiAgentMessageLogService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiAgentMessageLogEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiAgentMessageLogService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiAgentMessageLogEntity> getById(@PathVariable Long id) {
        return Result.ok(aiAgentMessageLogService.getById(id));
    }

    @PostMapping
    public Result<Boolean> create(@RequestBody AiAgentMessageLogEntity entity) {
        return Result.ok(aiAgentMessageLogService.save(entity));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody AiAgentMessageLogEntity entity) {
        return Result.ok(aiAgentMessageLogService.updateById(entity));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiAgentMessageLogService.removeById(id));
    }

}
