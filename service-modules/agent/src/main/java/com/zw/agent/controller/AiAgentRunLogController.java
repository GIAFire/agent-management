package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiAgentRunLogEntity;
import com.zw.agent.service.AiAgentRunLogService;
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
public class AiAgentRunLogController {
    private final AiAgentRunLogService aiAgentRunLogService;

    @GetMapping("/list")
    public Result<List<AiAgentRunLogEntity>> list() {
        return Result.ok(aiAgentRunLogService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiAgentRunLogEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiAgentRunLogService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiAgentRunLogEntity> getById(@PathVariable Long id) {
        return Result.ok(aiAgentRunLogService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiAgentRunLogEntity entity) {
        return Result.ok(aiAgentRunLogService.save(entity));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiAgentRunLogEntity entity) {
        return Result.ok(aiAgentRunLogService.updateById(entity));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiAgentRunLogService.removeById(id));
    }

}
