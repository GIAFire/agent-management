package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiSkillResourceEntity;
import com.zw.agent.entity.DTO.AiSkillResourceSaveRequest;
import com.zw.agent.service.AiSkillResourceService;
import com.zw.common.entity.Result;
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
        return Result.ok(enrichList(aiSkillResourceService.list()));
    }

    @GetMapping("/page")
    public Result<IPage<AiSkillResourceEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        IPage<AiSkillResourceEntity> page = aiSkillResourceService.page(new Page<>(current, size));
        page.setRecords(enrichList(page.getRecords()));
        return Result.ok(page);
    }

    @GetMapping("/{id}")
    public Result<AiSkillResourceEntity> getById(@PathVariable Long id) {
        return Result.ok(enrich(aiSkillResourceService.getById(id)));
    }

    @GetMapping("/skill/{skillId}")
    public Result<List<AiSkillResourceEntity>> listBySkill(@PathVariable Long skillId) {
        return Result.ok(enrichList(aiSkillResourceService.list(Wrappers.<AiSkillResourceEntity>lambdaQuery()
                .eq(AiSkillResourceEntity::getSkillId, skillId)
                .orderByAsc(AiSkillResourceEntity::getResourcePath))));
    }

    @GetMapping("/content/{id}")
    public Result<String> getContent(@PathVariable Long id) {
        AiSkillResourceEntity entity = aiSkillResourceService.getById(id);
        return Result.ok(entity == null ? "" : entity.getResourceContent());
    }

    @PostMapping("/create")
    public Result<AiSkillResourceEntity> create(@RequestBody AiSkillResourceSaveRequest request) {
        return Result.ok(aiSkillResourceService.createResource(request));
    }


    @PostMapping("/update")
    public Result<AiSkillResourceEntity> update(@RequestBody AiSkillResourceSaveRequest request) {
        return Result.ok(aiSkillResourceService.updateResource(request));
    }

    @PostMapping("/createPackageNode")
    public Result<AiSkillResourceEntity> createPackageNode(@RequestBody AiSkillResourceSaveRequest request) {
        return Result.ok(aiSkillResourceService.createResource(request));
    }

    @PostMapping("/updatePackageFile")
    public Result<AiSkillResourceEntity> updatePackageFile(@RequestBody AiSkillResourceSaveRequest request) {
        return Result.ok(aiSkillResourceService.updateResource(request));
    }


    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiSkillResourceService.removeById(id));
    }

    @GetMapping("/deletePackageNode/{id}")
    public Result<Boolean> deletePackageNode(@PathVariable Long id) {
        return Result.ok(aiSkillResourceService.removeById(id));
    }

    private List<AiSkillResourceEntity> enrichList(List<AiSkillResourceEntity> entities) {
        return entities == null ? List.of() : entities.stream().map(this::enrich).toList();
    }

    private AiSkillResourceEntity enrich(AiSkillResourceEntity entity) {
        if (entity == null) {
            return null;
        }
        entity.setRelativePath(entity.getResourcePath());
        entity.setContent(entity.getResourceContent());
        entity.setDirectory("DIRECTORY".equals(entity.getFileRole()));
        return entity;
    }

}
