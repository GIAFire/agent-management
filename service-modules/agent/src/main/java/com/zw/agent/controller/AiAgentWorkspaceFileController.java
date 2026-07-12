package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiAgentWorkspaceFileEntity;
import com.zw.agent.service.AiAgentWorkspaceFileService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * Agent工作区文件表 前端控制器
 * </p>
 *
 * @author 智纬
 * @since 2026-07-05
 */
@RestController
@RequestMapping("/workspaceFile")
@AllArgsConstructor
public class AiAgentWorkspaceFileController {
    private final AiAgentWorkspaceFileService aiAgentWorkspaceFileService;

    @GetMapping("/list")
    public Result<List<AiAgentWorkspaceFileEntity>> list() {
        return Result.ok(aiAgentWorkspaceFileService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiAgentWorkspaceFileEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiAgentWorkspaceFileService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiAgentWorkspaceFileEntity> getById(@PathVariable Long id) {
        return Result.ok(aiAgentWorkspaceFileService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiAgentWorkspaceFileEntity entity) {
        return Result.ok(aiAgentWorkspaceFileService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiAgentWorkspaceFileEntity entity) {
        return Result.ok(aiAgentWorkspaceFileService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiAgentWorkspaceFileService.removeById(id));
    }

}