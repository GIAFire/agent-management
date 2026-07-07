package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiAgentRunEventLogEntity;
import com.zw.agent.service.AiAgentRunEventLogService;
import com.zw.common.entity.Result;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * Agent 运行事件表：保存流式 token、工具调用、权限请求等事件 前端控制器
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@RestController
@RequestMapping("/agentRunEvent")
@AllArgsConstructor
public class AiAgentRunEventLogController {
    private final AiAgentRunEventLogService aiAgentRunEventLogService;

    @GetMapping("/list")
    public Result<List<AiAgentRunEventLogEntity>> list() {
        return Result.ok(aiAgentRunEventLogService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiAgentRunEventLogEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiAgentRunEventLogService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiAgentRunEventLogEntity> getById(@PathVariable Long id) {
        return Result.ok(aiAgentRunEventLogService.getById(id));
    }

    @PostMapping
    public Result<Boolean> create(@RequestBody AiAgentRunEventLogEntity entity) {
        return Result.ok(aiAgentRunEventLogService.save(entity));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody AiAgentRunEventLogEntity entity) {
        return Result.ok(aiAgentRunEventLogService.updateById(entity));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiAgentRunEventLogService.removeById(id));
    }

}
