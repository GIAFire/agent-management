package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zw.agent.entity.AiSkillInfoEntity;
import com.zw.agent.entity.AiSkillRoleEntity;
import com.zw.agent.entity.DTO.AiSkillInfoSaveRequest;
import com.zw.agent.service.AiSkillInfoService;
import com.zw.agent.service.AiSkillRoleService;
import com.zw.common.entity.Result;
import lombok.AllArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AiSkillInfoService aiSkillInfoService;
    private final AiSkillRoleService aiSkillRoleService;

    @GetMapping("/list")
    public Result<List<AiSkillInfoEntity>> list() {
        return Result.ok(enrichList(aiSkillInfoService.list()));
    }

    @GetMapping("/page")
    public Result<IPage<AiSkillInfoEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        IPage<AiSkillInfoEntity> page = aiSkillInfoService.page(new Page<>(current, size));
        page.setRecords(enrichList(page.getRecords()));
        return Result.ok(page);
    }

    @GetMapping("/{id}")
    public Result<AiSkillInfoEntity> getById(@PathVariable Long id) {
        return Result.ok(enrich(aiSkillInfoService.getById(id)));
    }

    @PostMapping("/create")
    public Result<AiSkillInfoEntity> create(@RequestBody AiSkillInfoSaveRequest request) {
        return Result.ok(aiSkillInfoService.createWithRoles(request));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiSkillInfoSaveRequest request) {
        return Result.ok(aiSkillInfoService.updateWithRoles(request));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        aiSkillRoleService.remove(Wrappers.<AiSkillRoleEntity>lambdaQuery()
                .eq(AiSkillRoleEntity::getSkillInfoId, id));
        Boolean ret = aiSkillInfoService.removeById(id);
        return Result.ok(ret);
    }

    private List<AiSkillInfoEntity> enrichList(List<AiSkillInfoEntity> entities) {
        return entities == null ? Collections.emptyList() : entities.stream().map(this::enrich).toList();
    }

    private AiSkillInfoEntity enrich(AiSkillInfoEntity entity) {
        if (entity == null) {
            return null;
        }
        Map<String, Object> metadata = parseMetadata(entity.getMetadataJson());
        entity.setSkillName(entity.getName());
        entity.setSkillMdContent(entity.getSkillContent());
        entity.setSkillKey(firstText(asString(metadata.get("skillKey")), entity.getSource()));
        entity.setCategory(asString(metadata.get("category")));
        entity.setRoleCodes(aiSkillRoleService.list(Wrappers.<AiSkillRoleEntity>lambdaQuery()
                        .eq(AiSkillRoleEntity::getSkillInfoId, entity.getId()))
                .stream()
                .map(AiSkillRoleEntity::getRoleCode)
                .toList());
        return entity;
    }

    private Map<String, Object> parseMetadata(String metadataJson) {
        if (!StringUtils.hasText(metadataJson)) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(metadataJson, new TypeReference<>() {
            });
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

}
