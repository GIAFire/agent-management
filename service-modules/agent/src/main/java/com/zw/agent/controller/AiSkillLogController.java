package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiSkillLogEntity;
import com.zw.agent.service.AiSkillLogService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * <p>
 * Skill使用日志表：记录Agent读取、加载、执行Skill的行为 前端控制器
 * </p>
 *
 * @author 智伟
 * @since 2026-07-16
 */
@RestController
@RequestMapping("/skillLog")
@AllArgsConstructor
public class AiSkillLogController {

    private final AiSkillLogService aiSkillLogService;

    @GetMapping("/list")
    public Result<List<AiSkillLogEntity>> list() {
        return Result.ok(aiSkillLogService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiSkillLogEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiSkillLogService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiSkillLogEntity> getById(@PathVariable Long id) {
        return Result.ok(aiSkillLogService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiSkillLogEntity entity) {
        return Result.ok(aiSkillLogService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiSkillLogEntity entity) {
        return Result.ok(aiSkillLogService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiSkillLogService.removeById(id));
    }

}