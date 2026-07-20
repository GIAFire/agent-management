package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiSkillResourceEntity;
import com.zw.agent.service.AiSkillResourceService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * <p>
 * Skill附属文件表：保存Skill目录下的SKILL.md、references、scripts和样例资源 前端控制器
 * </p>
 *
 * @author 智伟
 * @since 2026-07-16
 */
@RestController
@RequestMapping("/skillResource")
@AllArgsConstructor
public class AiSkillResourceController {

    private final AiSkillResourceService aiSkillResourceService;

    @GetMapping("/list")
    public Result<List<AiSkillResourceEntity>> list() {
        return Result.ok(aiSkillResourceService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiSkillResourceEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiSkillResourceService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiSkillResourceEntity> getById(@PathVariable Long id) {
        return Result.ok(aiSkillResourceService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiSkillResourceEntity entity) {
        return Result.ok(aiSkillResourceService.save(EntityDefaults.create(entity)));
    }


    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiSkillResourceEntity entity) {
        return Result.ok(aiSkillResourceService.updateById(EntityDefaults.update(entity)));
    }


    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiSkillResourceService.removeById(id));
    }

}
