package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiAgentRunEventEntity;
import com.zw.agent.service.AiAgentRunEventService;
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
public class AiAgentRunEventController {
    private final AiAgentRunEventService aiAgentRunEventService;

    @GetMapping("/list")
    public Result<List<AiAgentRunEventEntity>> list() {
        return Result.ok(aiAgentRunEventService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiAgentRunEventEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiAgentRunEventService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiAgentRunEventEntity> getById(@PathVariable Long id) {
        return Result.ok(aiAgentRunEventService.getById(id));
    }

    @PostMapping
    public Result<Boolean> create(@RequestBody AiAgentRunEventEntity entity) {
        return Result.ok(aiAgentRunEventService.save(entity));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody AiAgentRunEventEntity entity) {
        return Result.ok(aiAgentRunEventService.updateById(entity));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiAgentRunEventService.removeById(id));
    }

}
