package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiKnowledgeRetrievalHitEntity;
import com.zw.agent.service.AiKnowledgeRetrievalHitService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 知识库检索命中明细表：记录每次检索命中的文档切片、分数和引用信息 前端控制器
 * </p>
 *
 * @author 智纬
 * @since 2026-07-06
 */
@RestController
@RequestMapping("/knowledgeRetrievalHit")
@AllArgsConstructor
public class AiKnowledgeRetrievalHitController {
    private final AiKnowledgeRetrievalHitService aiKnowledgeRetrievalHitService;

    @GetMapping("/list")
    public Result<List<AiKnowledgeRetrievalHitEntity>> list() {
        return Result.ok(aiKnowledgeRetrievalHitService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiKnowledgeRetrievalHitEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiKnowledgeRetrievalHitService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiKnowledgeRetrievalHitEntity> getById(@PathVariable Long id) {
        return Result.ok(aiKnowledgeRetrievalHitService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiKnowledgeRetrievalHitEntity entity) {
        return Result.ok(aiKnowledgeRetrievalHitService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiKnowledgeRetrievalHitEntity entity) {
        return Result.ok(aiKnowledgeRetrievalHitService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiKnowledgeRetrievalHitService.removeById(id));
    }

}