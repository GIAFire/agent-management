package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiAgentRunEntity;
import com.zw.agent.service.AiAgentRunService;
import com.zw.common.entity.Result;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * Agent 运行表：一次用户请求对应一次 AgentScope call 或 streamEvents 执行 前端控制器
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@RestController
@RequestMapping("/agentRun")
@AllArgsConstructor
public class AiAgentRunController {
    private final AiAgentRunService aiAgentRunService;

    @GetMapping("/list")
    public Result<List<AiAgentRunEntity>> list() {
        return Result.ok(aiAgentRunService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiAgentRunEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiAgentRunService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiAgentRunEntity> getById(@PathVariable Long id) {
        return Result.ok(aiAgentRunService.getById(id));
    }

    @PostMapping
    public Result<Boolean> create(@RequestBody AiAgentRunEntity entity) {
        return Result.ok(aiAgentRunService.save(entity));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody AiAgentRunEntity entity) {
        return Result.ok(aiAgentRunService.updateById(entity));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiAgentRunService.removeById(id));
    }

}
