package com.zw.agent.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zw.agent.entity.AiSkillInfoEntity;
import com.zw.agent.entity.AiSkillRoleEntity;
import com.zw.agent.entity.DTO.AiSkillInfoSaveRequest;
import com.zw.agent.entity.DTO.SkillFileDTO;
import com.zw.agent.mapper.AiSkillInfoMapper;
import com.zw.agent.service.AiSkillInfoService;
import com.zw.agent.service.AiSkillRoleService;
import com.zw.common.support.EntityDefaults;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Skill定义表：保存可复用能力包的基础信息和当前发布版本 服务实现类
 * </p>
 *
 * @author 智伟
 * @since 2026-07-16
 */
@Service
@RequiredArgsConstructor
public class AiSkillInfoServiceImpl extends ServiceImpl<AiSkillInfoMapper, AiSkillInfoEntity> implements AiSkillInfoService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AiSkillInfoMapper skillInfoMapper;
    private final AiSkillRoleService aiSkillRoleService;

    @Override
    public SkillFileDTO getAgentSkill(String name, Long agentId,Long tenantId) {
        return skillInfoMapper.getAgentSkill(name, agentId,tenantId);
    }

    @Override
    public List<SkillFileDTO> getAgentSkillName(Long agentId, Long tenantId) {
        return skillInfoMapper.getAgentSkillName(agentId,tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiSkillInfoEntity createWithRoles(AiSkillInfoSaveRequest request) {
        AiSkillInfoEntity entity = buildEntity(request, true);
        save(EntityDefaults.create(entity));
        List<String> roleCodes = normalizeRoleCodes(request, true);
        syncSkillRoles(entity.getId(), roleCodes);
        return fillAliasFields(entity, roleCodes, request == null ? null : request.getCategory());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateWithRoles(AiSkillInfoSaveRequest request) {
        if (request == null || request.getId() == null) {
            throw new IllegalArgumentException("技能ID不能为空");
        }

        AiSkillInfoEntity entity = buildEntity(request, false);
        Boolean updated = updateById(EntityDefaults.update(entity));
        if (request.getRoleCodes() != null || request.getRoleIds() != null) {
            syncSkillRoles(entity.getId(), normalizeRoleCodes(request, true));
        }
        return updated;
    }

    private AiSkillInfoEntity buildEntity(AiSkillInfoSaveRequest request, boolean create) {
        if (request == null) {
            throw new IllegalArgumentException("技能信息不能为空");
        }

        String name = firstText(request.getName(), request.getSkillName());
        if (create && !StringUtils.hasText(name)) {
            throw new IllegalArgumentException("技能名称不能为空");
        }

        String description = request.getDescription();
        String skillContent = request.getSkillContent() != null
                ? request.getSkillContent()
                : request.getSkillMdContent();
        if (create && skillContent == null) {
            skillContent = "";
        }

        AiSkillInfoEntity entity = new AiSkillInfoEntity()
                .setId(request.getId())
                .setName(name)
                .setDescription(description)
                .setSkillContent(skillContent)
                .setSource(firstText(request.getSource(), request.getSkillKey()))
                .setMetadataJson(resolveMetadataJson(request))
                .setRiskLevel(firstText(request.getRiskLevel(), create ? "LOW" : null))
                .setTagsJson(request.getTagsJson())
                .setStatus(request.getStatus() == null && create ? (byte) 1 : request.getStatus());
        return entity;
    }

    private String resolveMetadataJson(AiSkillInfoSaveRequest request) {
        if (StringUtils.hasText(request.getMetadataJson())) {
            return request.getMetadataJson();
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        putIfHasText(metadata, "skillKey", request.getSkillKey());
        putIfHasText(metadata, "category", request.getCategory());
        putIfHasText(metadata, "scopeType", request.getScopeType());
        putIfHasText(metadata, "scopeValue", request.getScopeValue());
        if (request.getRequiresShell() != null) {
            metadata.put("requiresShell", request.getRequiresShell());
        }
        if (request.getRequiresSandbox() != null) {
            metadata.put("requiresSandbox", request.getRequiresSandbox());
        }

        if (metadata.isEmpty()) {
            return null;
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("技能元数据序列化失败", e);
        }
    }

    private void syncSkillRoles(Long skillInfoId, List<String> roleCodes) {
        aiSkillRoleService.remove(Wrappers.<AiSkillRoleEntity>lambdaQuery()
                .eq(AiSkillRoleEntity::getSkillInfoId, skillInfoId));

        if (roleCodes.isEmpty()) {
            return;
        }

        List<AiSkillRoleEntity> entities = roleCodes.stream()
                .map(roleCode -> EntityDefaults.create(new AiSkillRoleEntity()
                        .setSkillInfoId(skillInfoId)
                        .setRoleCode(roleCode)))
                .toList();
        aiSkillRoleService.saveBatch(entities);
    }

    private List<String> normalizeRoleCodes(AiSkillInfoSaveRequest request, boolean defaultAll) {
        List<String> rawCodes = request.getRoleCodes() != null ? request.getRoleCodes() : request.getRoleIds();
        if (rawCodes == null) {
            return defaultAll ? List.of("0") : List.of();
        }

        List<String> roleCodes = rawCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (roleCodes.isEmpty()) {
            return defaultAll ? List.of("0") : List.of();
        }
        if (roleCodes.contains("0")) {
            return List.of("0");
        }
        return new ArrayList<>(roleCodes);
    }

    private AiSkillInfoEntity fillAliasFields(AiSkillInfoEntity entity, List<String> roleCodes, String category) {
        entity.setSkillName(entity.getName());
        entity.setSkillKey(entity.getSource());
        entity.setSkillMdContent(entity.getSkillContent());
        entity.setCategory(category);
        entity.setRoleCodes(roleCodes);
        return entity;
    }

    private void putIfHasText(Map<String, Object> metadata, String key, String value) {
        if (StringUtils.hasText(value)) {
            metadata.put(key, value);
        }
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

}
