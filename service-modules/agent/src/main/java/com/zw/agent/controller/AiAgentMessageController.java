package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiAgentMessageEntity;
import com.zw.agent.service.AiAgentMessageService;
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
public class AiAgentMessageController {
    private final AiAgentMessageService aiAgentMessageService;

    @GetMapping("/list")
    public Result<List<AiAgentMessageEntity>> list() {
        return Result.ok(aiAgentMessageService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiAgentMessageEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiAgentMessageService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiAgentMessageEntity> getById(@PathVariable Long id) {
        return Result.ok(aiAgentMessageService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiAgentMessageEntity entity) {
        return Result.ok(aiAgentMessageService.save(entity));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiAgentMessageEntity entity) {
        return Result.ok(aiAgentMessageService.updateById(entity));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiAgentMessageService.removeById(id));
    }

}
