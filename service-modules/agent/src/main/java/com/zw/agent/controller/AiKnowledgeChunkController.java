package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiKnowledgeChunkEntity;
import com.zw.agent.service.AiKnowledgeChunkService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chunk")
@RequiredArgsConstructor
public class AiKnowledgeChunkController {

    private final AiKnowledgeChunkService aiKnowledgeChunkService;
    @GetMapping("/list")
    public Result<List<AiKnowledgeChunkEntity>> list() {
        return Result.ok(aiKnowledgeChunkService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiKnowledgeChunkEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiKnowledgeChunkService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiKnowledgeChunkEntity> getById(@PathVariable Long id) {
        return Result.ok(aiKnowledgeChunkService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiKnowledgeChunkEntity entity) {
        return Result.ok(aiKnowledgeChunkService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiKnowledgeChunkEntity entity) {
        return Result.ok(aiKnowledgeChunkService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiKnowledgeChunkService.removeById(id));
    }
}
