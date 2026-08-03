package com.zhiran.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiran.agent.entity.AiKnowledgeAgentBindingEntity;
import com.zhiran.agent.service.AiKnowledgeAgentBindingService;
import com.zhiran.common.entity.Result;
import com.zhiran.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * Agent知识库绑定表：控制Agent配置可访问的知识库及检索参数 前端控制器
 * </p>
 *
 * @author 智纬
 * @since 2026-07-11
 */
@RestController
@RequestMapping("/knowledgeAgentBinding")
@AllArgsConstructor
public class AiKnowledgeAgentBindingController {
    private final AiKnowledgeAgentBindingService aiKnowledgeAgentBindingService;

    @GetMapping("/list")
    public Result<List<AiKnowledgeAgentBindingEntity>> list() {
        return Result.ok(aiKnowledgeAgentBindingService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiKnowledgeAgentBindingEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiKnowledgeAgentBindingService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiKnowledgeAgentBindingEntity> getById(@PathVariable Long id) {
        return Result.ok(aiKnowledgeAgentBindingService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiKnowledgeAgentBindingEntity entity) {
        return Result.ok(aiKnowledgeAgentBindingService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiKnowledgeAgentBindingEntity entity) {
        return Result.ok(aiKnowledgeAgentBindingService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiKnowledgeAgentBindingService.removeById(id));
    }

}