package com.zhiran.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiran.agent.entity.AiToolInfoConfigEntity;
import com.zhiran.agent.service.AiToolInfoConfigService;
import com.zhiran.agent.service.ToolManagementService;
import com.zhiran.agent.entity.DTO.ToolMetricsResponse;
import com.zhiran.common.entity.Result;
import com.zhiran.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * <p>
 * 全局工具配置表 前端控制器
 * </p>
 *
 * @author 智纬
 * @since 2026-06-27
 */
@RestController
@RequestMapping("/toolInfoConfig")
@AllArgsConstructor
public class AiToolInfoConfigController {

    private final AiToolInfoConfigService aiToolInfoConfigService;
    private final ToolManagementService toolManagementService;

    /**
     * 查询所有工具配置列表
     */
    @GetMapping("/list")
    public Result<List<AiToolInfoConfigEntity>> list() {
        return Result.ok(toolManagementService.listTools());
    }

    @GetMapping("/metrics")
    public Result<ToolMetricsResponse> metrics() {
        return Result.ok(toolManagementService.metrics());
    }

    /**
     * 分页查询工具配置
     */
    @GetMapping("/page")
    public Result<IPage<AiToolInfoConfigEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiToolInfoConfigService.page(new Page<>(current, size)));
    }

    /**
     * 根据ID查询工具配置
     */
    @GetMapping("/{id}")
    public Result<AiToolInfoConfigEntity> getById(@PathVariable Long id) {
        return Result.ok(aiToolInfoConfigService.getById(id));
    }

    /**
     * 新增工具配置
     */
    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiToolInfoConfigEntity entity) {
        return Result.ok(aiToolInfoConfigService.save(EntityDefaults.create(entity)));
    }

    /**
     * 更新工具配置
     */
    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiToolInfoConfigEntity entity) {
        return Result.ok(aiToolInfoConfigService.updateById(EntityDefaults.update(entity)));
    }

    /**
     * 根据ID删除工具配置
     */
    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiToolInfoConfigService.removeById(id));
    }

}
