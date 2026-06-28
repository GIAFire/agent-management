package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiModelConfigEntity;
import com.zw.agent.service.AiModelConfigService;
import com.zw.agent.support.EntityDefaults;
import com.zw.common.entity.Result;
import java.util.List;
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
public class AiModelConfigController {
    private final AiModelConfigService aiModelConfigService;

    @GetMapping("/list")
    public Result<List<AiModelConfigEntity>> list() {

        return Result.ok(aiModelConfigService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiModelConfigEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiModelConfigService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiModelConfigEntity> getById(@PathVariable Long id) {
        return Result.ok(aiModelConfigService.getById(id));
    }

    @PostMapping("/save")
    public Result<Boolean> save(@RequestBody AiModelConfigEntity entity) {
//        BeanUtils.copyProperties(entity, modelConfigEntity);
        return Result.ok(aiModelConfigService.save(EntityDefaults.create(entity)));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody AiModelConfigEntity entity) {
        return Result.ok(aiModelConfigService.updateById(EntityDefaults.update(entity)));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiModelConfigService.removeById(id));
    }

}
