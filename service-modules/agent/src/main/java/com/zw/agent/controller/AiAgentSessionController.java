package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiAgentSessionEntity;
import com.zw.agent.service.AiAgentSessionService;
import com.zw.common.entity.Result;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * Agent 会话表：保存用户与 Agent 的一次连续对话 前端控制器
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@RestController
@RequestMapping("/agentSession")
@AllArgsConstructor
public class AiAgentSessionController {
    private final AiAgentSessionService aiAgentSessionService;

    @GetMapping("/list")
    public Result<List<AiAgentSessionEntity>> list() {
        return Result.ok(aiAgentSessionService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiAgentSessionEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiAgentSessionService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiAgentSessionEntity> getById(@PathVariable Long id) {
        return Result.ok(aiAgentSessionService.getById(id));
    }

    @PostMapping
    public Result<Boolean> create(@RequestBody AiAgentSessionEntity entity) {
        return Result.ok(aiAgentSessionService.save(entity));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody AiAgentSessionEntity entity) {
        return Result.ok(aiAgentSessionService.updateById(entity));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiAgentSessionService.removeById(id));
    }

}
