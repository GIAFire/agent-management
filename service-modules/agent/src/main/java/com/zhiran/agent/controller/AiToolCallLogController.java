package com.zhiran.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiran.common.context.UserContext;
import com.zhiran.agent.entity.AiToolCallLogEntity;
import com.zhiran.agent.service.AiToolCallLogService;
import com.zhiran.agent.service.ToolManagementService;
import com.zhiran.agent.entity.DTO.ToolCallLogResponse;
import com.zhiran.common.entity.Result;
import com.zhiran.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 工具调用审计表：记录Agent每一次工具调用的权限结果、参数、结果和耗时 前端控制器
 * </p>
 *
 * @author 智纬
 * @since 2026-06-28
 */
@RestController
@RequestMapping("/toolCallLog")
@AllArgsConstructor
public class AiToolCallLogController {
    private final AiToolCallLogService aiToolCallLogService;
    private final ToolManagementService toolManagementService;

    @GetMapping("/list")
    public Result<List<AiToolCallLogEntity>> list() {
        return Result.ok(aiToolCallLogService.list(new LambdaQueryWrapper<AiToolCallLogEntity>()
                .eq(AiToolCallLogEntity::getTenantId, UserContext.get().getTenantId())
                .orderByDesc(AiToolCallLogEntity::getStartedAt)));
    }

    @GetMapping("/page")
    public Result<IPage<AiToolCallLogEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiToolCallLogService.page(new Page<>(current, size),
                new LambdaQueryWrapper<AiToolCallLogEntity>()
                        .eq(AiToolCallLogEntity::getTenantId, UserContext.get().getTenantId())
                        .orderByDesc(AiToolCallLogEntity::getStartedAt)));
    }

    @GetMapping("/management/page")
    public Result<IPage<ToolCallLogResponse>> managementPage(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long toolId,
            @RequestParam(required = false) String successStatus
    ) {
        return Result.ok(toolManagementService.pageCallLogs(current, size, toolId, successStatus));
    }

    @GetMapping("/{id}")
    public Result<AiToolCallLogEntity> getById(@PathVariable Long id) {
        return Result.ok(aiToolCallLogService.getOne(new LambdaQueryWrapper<AiToolCallLogEntity>()
                .eq(AiToolCallLogEntity::getTenantId, UserContext.get().getTenantId())
                .eq(AiToolCallLogEntity::getId, id)));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiToolCallLogEntity entity) {
        return Result.ok(aiToolCallLogService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiToolCallLogEntity entity) {
        return Result.ok(aiToolCallLogService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiToolCallLogService.removeById(id));
    }

}
