package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiSkillInfoEntity;
import com.zw.agent.service.AiSkillInfoService;
import com.zw.common.entity.Result;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * <p>
 * Skill定义表：保存可复用能力包的基础信息和当前发布版本 前端控制器
 * </p>
 *
 * @author 智伟
 * @since 2026-07-16
 */
@RestController
@RequestMapping("/skillInfo")
@AllArgsConstructor
public class AiSkillInfoController {

    private final AiSkillInfoService aiSkillInfoService;

    @GetMapping("/list")
    public Result<List<AiSkillInfoEntity>> list() {
        return Result.ok(aiSkillInfoService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiSkillInfoEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiSkillInfoService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiSkillInfoEntity> getById(@PathVariable Long id) {
        return Result.ok(aiSkillInfoService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiSkillInfoEntity entity) {
        return Result.ok(aiSkillInfoService.save(entity));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiSkillInfoEntity entity) {
        return Result.ok(aiSkillInfoService.updateById(entity));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        Boolean ret = aiSkillInfoService.removeById(id);
        return Result.ok(ret);
    }

}
