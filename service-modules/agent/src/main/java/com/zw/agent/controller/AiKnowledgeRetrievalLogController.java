package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiKnowledgeRetrievalLogEntity;
import com.zw.agent.service.AiKnowledgeRetrievalLogService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 知识库检索日志表：记录每次RAG检索请求、结果、注入内容和审计信息 前端控制器
 * </p>
 *
 * @author 智纬
 * @since 2026-07-06
 */
@RestController
@RequestMapping("/knowledgeRetrievalLog")
@AllArgsConstructor
public class AiKnowledgeRetrievalLogController {
    private final AiKnowledgeRetrievalLogService aiKnowledgeRetrievalLogService;

    @GetMapping("/list")
    public Result<List<AiKnowledgeRetrievalLogEntity>> list() {
        return Result.ok(aiKnowledgeRetrievalLogService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiKnowledgeRetrievalLogEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiKnowledgeRetrievalLogService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiKnowledgeRetrievalLogEntity> getById(@PathVariable Long id) {
        return Result.ok(aiKnowledgeRetrievalLogService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiKnowledgeRetrievalLogEntity entity) {
        return Result.ok(aiKnowledgeRetrievalLogService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiKnowledgeRetrievalLogEntity entity) {
        return Result.ok(aiKnowledgeRetrievalLogService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiKnowledgeRetrievalLogService.removeById(id));
    }

}