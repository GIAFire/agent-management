package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiAgentModelEntity;
import com.zw.agent.service.AiAgentModelService;
import com.zw.common.entity.Result;
import java.util.List;
import com.zw.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 模型配置表：把凭证、模型名、生成参数组合成可被 Agent 选择的模型 前端控制器
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@RestController
@RequestMapping("/modelConfig")
@AllArgsConstructor
public class AiAgentModelController {
    private final AiAgentModelService aiAgentModelService;

    @GetMapping("/list")
    public Result<List<AiAgentModelEntity>> list() {

        return Result.ok(aiAgentModelService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiAgentModelEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiAgentModelService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiAgentModelEntity> getById(@PathVariable Long id) {
        return Result.ok(aiAgentModelService.getById(id));
    }

    @PostMapping("/save")
    public Result<Boolean> save(@RequestBody AiAgentModelEntity entity) {
//        BeanUtils.copyProperties(entity, modelConfigEntity);
        return Result.ok(aiAgentModelService.save(EntityDefaults.create(entity)));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody AiAgentModelEntity entity) {
        return Result.ok(aiAgentModelService.updateById(EntityDefaults.update(entity)));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiAgentModelService.removeById(id));
    }

}
