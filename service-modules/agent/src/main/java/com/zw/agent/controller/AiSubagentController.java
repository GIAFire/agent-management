package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zw.agent.entity.AiSubagentEntity;
import com.zw.agent.service.AiSubagentService;
import com.zw.agent.service.SubagentManagementService;
import com.zw.agent.entity.DTO.SubagentAgentOptionResponse;
import com.zw.agent.entity.DTO.SubagentListItemResponse;
import com.zw.agent.entity.DTO.SubagentMetricsResponse;
import com.zw.agent.entity.DTO.SubagentSaveRequest;
import com.zw.agent.entity.DTO.SubagentTaskResponse;
import com.zw.common.entity.Result;
import com.zw.common.context.UserContext;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 子Agent定义表：保存可复用专家Agent的能力描述、模型、工具、知识库和安全配置 前端控制器
 * </p>
 *
 * @author 智纬
 * @since 2026-07-11
 */
@RestController
@RequestMapping("/subagent")
@AllArgsConstructor
public class AiSubagentController {
    private final AiSubagentService aiSubagentService;
    private final SubagentManagementService managementService;

    @GetMapping("/list")
    public Result<List<AiSubagentEntity>> list() {
        return Result.ok(aiSubagentService.list(new LambdaQueryWrapper<AiSubagentEntity>()
                .eq(AiSubagentEntity::getTenantId, UserContext.get().getTenantId())
                .orderByDesc(AiSubagentEntity::getUpdatedAt)));
    }

    @GetMapping("/page")
    public Result<IPage<SubagentListItemResponse>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Byte sourceType,
            @RequestParam(required = false) Byte enabled,
            @RequestParam(required = false) Boolean sourceAvailable
    ) {
        return Result.ok(managementService.page(
                current,
                size,
                keyword,
                sourceType,
                enabled,
                sourceAvailable
        ));
    }

    @GetMapping("/metrics")
    public Result<SubagentMetricsResponse> metrics() {
        return Result.ok(managementService.metrics());
    }

    @GetMapping("/local-agent-options")
    public Result<List<SubagentAgentOptionResponse>> localAgentOptions() {
        return Result.ok(managementService.localAgentOptions());
    }

    @GetMapping("/recent-tasks")
    public Result<List<SubagentTaskResponse>> recentTasks(
            @RequestParam(defaultValue = "6") int limit,
            @RequestParam(defaultValue = "false") boolean exceptionsOnly
    ) {
        return Result.ok(managementService.recentTasks(limit, exceptionsOnly));
    }

    @GetMapping("/{id}")
    public Result<AiSubagentEntity> getById(@PathVariable Long id) {
        return Result.ok(aiSubagentService.getOne(new LambdaQueryWrapper<AiSubagentEntity>()
                .eq(AiSubagentEntity::getTenantId, UserContext.get().getTenantId())
                .eq(AiSubagentEntity::getId, id)));
    }

    @PostMapping("/create")
    public Result<SubagentListItemResponse> create(@RequestBody SubagentSaveRequest request) {
        try {
            return Result.ok(managementService.create(request));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return Result.fail(exception.getMessage());
        }
    }

    @PostMapping("/update")
    public Result<SubagentListItemResponse> update(@RequestBody SubagentSaveRequest request) {
        try {
            return Result.ok(managementService.update(request));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return Result.fail(exception.getMessage());
        }
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        try {
            return Result.ok(managementService.delete(id));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return Result.fail(exception.getMessage());
        }
    }

}
