package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiSkillAgentBindingEntity;
import com.zw.agent.service.AiSkillAgentBindingService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * <p>
 * Agent与Skill绑定表：定义某个Agent配置版本安装哪些Skill以及安装作用域 前端控制器
 * </p>
 *
 * @author 智伟
 * @since 2026-07-16
 */
@RestController
@RequestMapping("/skillAgentBinding")
@AllArgsConstructor
public class AiSkillAgentBindingController {

    private final AiSkillAgentBindingService aiSkillAgentBindingService;

    @GetMapping("/list")
    public Result<List<AiSkillAgentBindingEntity>> list() {
        return Result.ok(aiSkillAgentBindingService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiSkillAgentBindingEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiSkillAgentBindingService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiSkillAgentBindingEntity> getById(@PathVariable Long id) {
        return Result.ok(aiSkillAgentBindingService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiSkillAgentBindingEntity entity) {
        return Result.ok(aiSkillAgentBindingService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiSkillAgentBindingEntity entity) {
        return Result.ok(aiSkillAgentBindingService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiSkillAgentBindingService.removeById(id));
    }

}