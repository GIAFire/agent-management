package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiToolCallLogEntity;
import com.zw.agent.service.AiToolCallLogService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
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

    @GetMapping("/list")
    public Result<List<AiToolCallLogEntity>> list() {
        return Result.ok(aiToolCallLogService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiToolCallLogEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiToolCallLogService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiToolCallLogEntity> getById(@PathVariable Long id) {
        return Result.ok(aiToolCallLogService.getById(id));
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