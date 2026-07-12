package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiKnowledgeBaseEntity;
import com.zw.agent.service.AiKnowledgeBaseService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 知识库表：平台知识库抽象层，兼容RAGFlow及不同向量库 前端控制器
 * </p>
 *
 * @author 智纬
 * @since 2026-07-06
 */
@RestController
@RequestMapping("/knowledgeBase")
@AllArgsConstructor
public class AiKnowledgeBaseController {
    private final AiKnowledgeBaseService aiKnowledgeBaseService;

    @GetMapping("/list")
    public Result<List<AiKnowledgeBaseEntity>> list() {
        return Result.ok(aiKnowledgeBaseService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiKnowledgeBaseEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiKnowledgeBaseService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiKnowledgeBaseEntity> getById(@PathVariable Long id) {
        return Result.ok(aiKnowledgeBaseService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiKnowledgeBaseEntity entity) {
        return Result.ok(aiKnowledgeBaseService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiKnowledgeBaseEntity entity) {
        return Result.ok(aiKnowledgeBaseService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiKnowledgeBaseService.removeById(id));
    }

}