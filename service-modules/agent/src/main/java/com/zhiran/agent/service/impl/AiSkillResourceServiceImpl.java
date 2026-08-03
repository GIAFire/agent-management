package com.zhiran.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiran.agent.entity.AiSkillInfoEntity;
import com.zhiran.agent.entity.AiSkillResourceEntity;
import com.zhiran.agent.entity.DTO.AiSkillResourceSaveRequest;
import com.zhiran.agent.mapper.AiSkillResourceMapper;
import com.zhiran.agent.service.AiSkillInfoService;
import com.zhiran.agent.service.AiSkillResourceService;
import com.zhiran.common.context.UserContext;
import com.zhiran.common.context.UserInfo;
import com.zhiran.common.support.EntityDefaults;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AiSkillResourceServiceImpl
        extends ServiceImpl<AiSkillResourceMapper, AiSkillResourceEntity>
        implements AiSkillResourceService {

    private static final int MAX_RESOURCE_BYTES = 1024 * 1024;

    private final AiSkillResourceMapper resourceMapper;
    private final AiSkillInfoService skillService;

    @Override
    public List<AiSkillResourceEntity> listBySkill(Long skillId) {
        Long tenantId = currentTenantId();
        requireSkill(skillId, tenantId);
        return list(new LambdaQueryWrapper<AiSkillResourceEntity>()
                        .eq(AiSkillResourceEntity::getTenantId, tenantId)
                        .eq(AiSkillResourceEntity::getSkillId, skillId)
                        .orderByAsc(AiSkillResourceEntity::getResourcePath))
                .stream()
                .map(this::fillAliases)
                .toList();
    }

    @Override
    public String getContent(Long id) {
        AiSkillResourceEntity resource = requireResource(id, currentTenantId());
        return resource.getResourceContent();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiSkillResourceEntity createResource(AiSkillResourceSaveRequest request) {
        if (request == null || request.getSkillId() == null) {
            throw new IllegalArgumentException("Skill id must not be null");
        }
        Long tenantId = currentTenantId();
        requireSkill(request.getSkillId(), tenantId);
        String path = resolvePath(request);
        validateResourcePath(path);
        if (Boolean.TRUE.equals(request.getDirectory())) {
            return virtualDirectory(request.getSkillId(), path);
        }
        ensureNotSkillMd(path);
        String content = contentOf(request, true);
        validateContent(content);
        ensurePathAvailable(tenantId, request.getSkillId(), path, null);

        AiSkillResourceEntity resource = new AiSkillResourceEntity()
                .setSkillId(request.getSkillId())
                .setFileRole(inferFileRole(path))
                .setFileName(basename(path))
                .setResourcePath(path)
                .setResourceContent(content);
        resource.setTenantId(tenantId);
        save(EntityDefaults.create(resource));
        return fillAliases(resource);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiSkillResourceEntity updateResource(AiSkillResourceSaveRequest request) {
        if (request == null || request.getId() == null) {
            throw new IllegalArgumentException("Resource id must not be null");
        }
        Long tenantId = currentTenantId();
        AiSkillResourceEntity existing = requireResource(request.getId(), tenantId);
        requireSkill(existing.getSkillId(), tenantId);
        if (request.getSkillId() != null && !request.getSkillId().equals(existing.getSkillId())) {
            throw new IllegalArgumentException("A resource cannot be moved to another skill");
        }
        String path = StringUtils.hasText(request.getResourcePath())
                || StringUtils.hasText(request.getRelativePath())
                || StringUtils.hasText(request.getFileName())
                ? resolvePath(request)
                : existing.getResourcePath();
        validateResourcePath(path);
        ensureNotSkillMd(path);
        String content = contentOf(request, false);
        if (content == null) {
            content = existing.getResourceContent();
        }
        validateContent(content);
        ensurePathAvailable(tenantId, existing.getSkillId(), path, existing.getId());

        existing.setFileRole(inferFileRole(path))
                .setFileName(basename(path))
                .setResourcePath(path)
                .setResourceContent(content);
        updateById(EntityDefaults.update(existing));
        return fillAliases(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteResource(Long id) {
        Long tenantId = currentTenantId();
        requireResource(id, tenantId);
        return resourceMapper.hardDeleteById(id, tenantId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteFolder(Long skillId, String folderPath) {
        Long tenantId = currentTenantId();
        requireSkill(skillId, tenantId);
        String path = normalizePath(folderPath);
        validateResourcePath(path);
        ensureNotSkillMd(path);
        return resourceMapper.hardDeleteFolder(skillId, tenantId, path);
    }

    private AiSkillInfoEntity requireSkill(Long id, Long tenantId) {
        AiSkillInfoEntity skill = skillService.getOne(
                new LambdaQueryWrapper<AiSkillInfoEntity>()
                        .eq(AiSkillInfoEntity::getTenantId, tenantId)
                        .eq(AiSkillInfoEntity::getId, id),
                false
        );
        if (skill == null) {
            throw new IllegalArgumentException("Skill does not exist in the current tenant");
        }
        return skill;
    }

    private AiSkillResourceEntity requireResource(Long id, Long tenantId) {
        if (id == null) {
            throw new IllegalArgumentException("Resource id must not be null");
        }
        AiSkillResourceEntity resource = getOne(
                new LambdaQueryWrapper<AiSkillResourceEntity>()
                        .eq(AiSkillResourceEntity::getTenantId, tenantId)
                        .eq(AiSkillResourceEntity::getId, id),
                false
        );
        if (resource == null) {
            throw new IllegalArgumentException("Resource does not exist in the current tenant");
        }
        return resource;
    }

    private void ensurePathAvailable(
            Long tenantId,
            Long skillId,
            String path,
            Long excludedId
    ) {
        LambdaQueryWrapper<AiSkillResourceEntity> query =
                new LambdaQueryWrapper<AiSkillResourceEntity>()
                        .eq(AiSkillResourceEntity::getTenantId, tenantId)
                        .eq(AiSkillResourceEntity::getSkillId, skillId)
                        .eq(AiSkillResourceEntity::getResourcePath, path);
        if (excludedId != null) {
            query.ne(AiSkillResourceEntity::getId, excludedId);
        }
        if (count(query) > 0) {
            throw new IllegalArgumentException("A resource already exists at this path");
        }
    }

    private String resolvePath(AiSkillResourceSaveRequest request) {
        String direct = firstText(request.getResourcePath(), request.getRelativePath());
        if (StringUtils.hasText(direct)) {
            return normalizePath(direct);
        }
        String fileName = normalizePath(request.getFileName());
        String parent = normalizePath(request.getParentPath());
        return StringUtils.hasText(parent) ? parent + "/" + fileName : fileName;
    }

    private String normalizePath(String value) {
        return value == null ? "" : value.trim();
    }

    private void validateResourcePath(String path) {
        if (!StringUtils.hasText(path)
                || path.startsWith("/")
                || path.endsWith("/")
                || path.contains("\\")
                || path.matches("^[A-Za-z]:.*")) {
            throw new IllegalArgumentException("Resource path must be a relative slash-separated path");
        }
        String[] segments = path.split("/", -1);
        for (String segment : segments) {
            if (!StringUtils.hasText(segment)
                    || ".".equals(segment)
                    || "..".equals(segment)) {
                throw new IllegalArgumentException("Resource path contains an invalid segment");
            }
        }
    }

    private void ensureNotSkillMd(String path) {
        if ("skill.md".equals(path.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("SKILL.md is managed as the skill main instructions");
        }
    }

    private String contentOf(AiSkillResourceSaveRequest request, boolean defaultEmpty) {
        String content = request.getResourceContent() != null
                ? request.getResourceContent()
                : request.getContent();
        return content == null && defaultEmpty ? "" : content;
    }

    private void validateContent(String content) {
        if (content == null) {
            throw new IllegalArgumentException("Resource content must not be null");
        }
        if (content.getBytes(StandardCharsets.UTF_8).length > MAX_RESOURCE_BYTES) {
            throw new IllegalArgumentException("Resource content must not exceed 1 MiB");
        }
    }

    private String inferFileRole(String path) {
        String normalized = path.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("references/")) {
            return "REFERENCE";
        }
        if (normalized.startsWith("scripts/")) {
            return "SCRIPT";
        }
        if (normalized.startsWith("examples/")) {
            return "EXAMPLE";
        }
        return "ASSET";
    }

    private String basename(String path) {
        int index = path.lastIndexOf('/');
        return index < 0 ? path : path.substring(index + 1);
    }

    private AiSkillResourceEntity virtualDirectory(Long skillId, String path) {
        return fillAliases(new AiSkillResourceEntity()
                .setSkillId(skillId)
                .setFileRole("DIRECTORY")
                .setFileName(basename(path))
                .setResourcePath(path)
                .setResourceContent(""));
    }

    private AiSkillResourceEntity fillAliases(AiSkillResourceEntity entity) {
        entity.setRelativePath(entity.getResourcePath());
        entity.setContent(entity.getResourceContent());
        entity.setDirectory("DIRECTORY".equals(entity.getFileRole()));
        return entity;
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private Long currentTenantId() {
        UserInfo userInfo = UserContext.get();
        if (userInfo == null || userInfo.getTenantId() == null) {
            throw new IllegalStateException("Authenticated tenant context is required");
        }
        return userInfo.getTenantId();
    }
}
