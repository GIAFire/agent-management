package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiKnowledgeBackendConfigEntity;
import com.zw.agent.service.AiKnowledgeBackendConfigService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 知识库向量存储配置表 前端控制器
 * </p>
 *
 * @author 智纬
 * @since 2026-07-06
 */
@RestController
@RequestMapping("/knowledgeVectorConfig")
@AllArgsConstructor
public class AiKnowledgeVectorConfigController {
    private final AiKnowledgeBackendConfigService aiKnowledgeBackendConfigService;

    @GetMapping("/list")
    public Result<List<AiKnowledgeBackendConfigEntity>> list() {
        return Result.ok(aiKnowledgeBackendConfigService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiKnowledgeBackendConfigEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiKnowledgeBackendConfigService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiKnowledgeBackendConfigEntity> getById(@PathVariable Long id) {
        return Result.ok(aiKnowledgeBackendConfigService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiKnowledgeBackendConfigEntity entity) {
        return Result.ok(aiKnowledgeBackendConfigService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiKnowledgeBackendConfigEntity entity) {
        return Result.ok(aiKnowledgeBackendConfigService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiKnowledgeBackendConfigService.removeById(id));
    }

}