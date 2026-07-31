package com.zw.agent.controller;

import com.zw.agent.entity.AiSkillResourceEntity;
import com.zw.agent.entity.DTO.AiSkillResourceSaveRequest;
import com.zw.agent.service.AiSkillResourceService;
import com.zw.common.entity.Result;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/skillResource")
@RequiredArgsConstructor
public class AiSkillResourceController {

    private final AiSkillResourceService resourceService;

    @GetMapping("/skill/{skillId}")
    public Result<List<AiSkillResourceEntity>> listBySkill(@PathVariable Long skillId) {
        return Result.ok(resourceService.listBySkill(skillId));
    }

    @GetMapping("/content/{id}")
    public Result<String> getContent(@PathVariable Long id) {
        return Result.ok(resourceService.getContent(id));
    }

    @PostMapping("/create")
    public Result<AiSkillResourceEntity> create(
            @RequestBody AiSkillResourceSaveRequest request
    ) {
        return Result.ok(resourceService.createResource(request));
    }

    @PostMapping("/update")
    public Result<AiSkillResourceEntity> update(
            @RequestBody AiSkillResourceSaveRequest request
    ) {
        return Result.ok(resourceService.updateResource(request));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(resourceService.deleteResource(id));
    }

    @DeleteMapping("/folder")
    public Result<Integer> deleteFolder(
            @RequestParam Long skillId,
            @RequestParam String path
    ) {
        return Result.ok(resourceService.deleteFolder(skillId, path));
    }
}
