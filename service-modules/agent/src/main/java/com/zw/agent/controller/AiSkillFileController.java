package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiSkillFileEntity;
import com.zw.agent.entity.DTO.AiSkillFileSaveRequest;
import com.zw.agent.service.AiSkillFileService;
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
@RequestMapping("/skillFile")
@AllArgsConstructor
public class AiSkillFileController {

    private final AiSkillFileService aiSkillFileService;

    @GetMapping("/list")
    public Result<List<AiSkillFileEntity>> list() {
        return Result.ok(aiSkillFileService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiSkillFileEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiSkillFileService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiSkillFileEntity> getById(@PathVariable Long id) {
        return Result.ok(aiSkillFileService.getById(id));
    }

    @GetMapping("/skill/{skillId}")
    public Result<List<AiSkillFileEntity>> listBySkillId(@PathVariable Long skillId) {
        return Result.ok(aiSkillFileService.listBySkillId(skillId));
    }

    @GetMapping("/content/{id}")
    public Result<String> readSkillPackageFile(@PathVariable Long id) {
        return Result.ok(aiSkillFileService.readSkillPackageFile(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiSkillFileEntity entity) {
        return Result.ok(aiSkillFileService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/createPackageNode")
    public Result<AiSkillFileEntity> createPackageNode(@RequestBody AiSkillFileSaveRequest request) {
        return Result.ok(aiSkillFileService.createSkillPackageNode(request));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiSkillFileEntity entity) {
        return Result.ok(aiSkillFileService.updateById(EntityDefaults.update(entity)));
    }

    @PostMapping("/updatePackageFile")
    public Result<AiSkillFileEntity> updatePackageFile(@RequestBody AiSkillFileSaveRequest request) {
        return Result.ok(aiSkillFileService.updateSkillPackageFile(request));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiSkillFileService.removeById(id));
    }

    @GetMapping("/deletePackageNode/{id}")
    public Result<Boolean> deletePackageNode(@PathVariable Long id) {
        return Result.ok(aiSkillFileService.deleteSkillPackageNode(id));
    }

}
