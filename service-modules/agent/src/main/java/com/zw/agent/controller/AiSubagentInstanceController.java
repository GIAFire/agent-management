package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiSubagentInstanceEntity;
import com.zw.agent.service.AiSubagentInstanceService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 子Agent运行实例表：记录父Agent创建的子Agent实例、会话、工作区和暴露状态 前端控制器
 * </p>
 *
 * @author 智纬
 * @since 2026-07-11
 */
@RestController
@RequestMapping("/subagentInstance")
@AllArgsConstructor
public class AiSubagentInstanceController {
    private final AiSubagentInstanceService aiSubagentInstanceService;

    @GetMapping("/list")
    public Result<List<AiSubagentInstanceEntity>> list() {
        return Result.ok(aiSubagentInstanceService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiSubagentInstanceEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiSubagentInstanceService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiSubagentInstanceEntity> getById(@PathVariable Long id) {
        return Result.ok(aiSubagentInstanceService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiSubagentInstanceEntity entity) {
        return Result.ok(aiSubagentInstanceService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiSubagentInstanceEntity entity) {
        return Result.ok(aiSubagentInstanceService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiSubagentInstanceService.removeById(id));
    }

}