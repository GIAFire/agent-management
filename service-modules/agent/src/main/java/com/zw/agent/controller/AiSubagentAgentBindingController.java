package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiSubagentAgentBindingEntity;
import com.zw.agent.service.AiSubagentAgentBindingService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 主Agent与子Agent绑定表：定义某个主Agent版本可以委派哪些子Agent及调用策略 前端控制器
 * </p>
 *
 * @author 智纬
 * @since 2026-07-11
 */
@RestController
@RequestMapping("/subagentAgentBinding")
@AllArgsConstructor
public class AiSubagentAgentBindingController {
    private final AiSubagentAgentBindingService aiSubagentAgentBindingService;

    @GetMapping("/list")
    public Result<List<AiSubagentAgentBindingEntity>> list() {
        return Result.ok(aiSubagentAgentBindingService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiSubagentAgentBindingEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiSubagentAgentBindingService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiSubagentAgentBindingEntity> getById(@PathVariable Long id) {
        return Result.ok(aiSubagentAgentBindingService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiSubagentAgentBindingEntity entity) {
        return Result.ok(aiSubagentAgentBindingService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiSubagentAgentBindingEntity entity) {
        return Result.ok(aiSubagentAgentBindingService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiSubagentAgentBindingService.removeById(id));
    }

}