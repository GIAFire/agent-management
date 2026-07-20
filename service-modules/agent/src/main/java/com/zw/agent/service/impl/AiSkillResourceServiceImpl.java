package com.zw.agent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.agent.entity.AiSkillResourceEntity;
import com.zw.agent.entity.DTO.AiSkillResourceSaveRequest;
import com.zw.agent.mapper.AiSkillResourceMapper;
import com.zw.agent.service.AiSkillResourceService;
import com.zw.common.support.EntityDefaults;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * <p>
 * Skill附属文件表：保存Skill目录下的SKILL.md、references、scripts和样例资源 服务实现类
 * </p>
 *
 * @author 智伟
 * @since 2026-07-16
 */
@Service
@RequiredArgsConstructor
public class AiSkillResourceServiceImpl extends ServiceImpl<AiSkillResourceMapper, AiSkillResourceEntity> implements AiSkillResourceService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiSkillResourceEntity createResource(AiSkillResourceSaveRequest request) {
        AiSkillResourceEntity entity = buildEntity(request, true);
        if ("DIRECTORY".equals(entity.getFileRole())) {
            return fillAliasFields(entity);
        }
        save(EntityDefaults.create(entity));
        return fillAliasFields(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiSkillResourceEntity updateResource(AiSkillResourceSaveRequest request) {
        if (request == null || request.getId() == null) {
            throw new IllegalArgumentException("资源文件ID不能为空");
        }

        AiSkillResourceEntity entity = buildEntity(request, false);
        updateById(EntityDefaults.update(entity));
        AiSkillResourceEntity refreshed = getById(entity.getId());
        return fillAliasFields(refreshed == null ? entity : refreshed);
    }

    private AiSkillResourceEntity buildEntity(AiSkillResourceSaveRequest request, boolean create) {
        if (request == null) {
            throw new IllegalArgumentException("资源文件信息不能为空");
        }

        if (create && request.getSkillId() == null) {
            throw new IllegalArgumentException("技能ID不能为空");
        }

        String resourcePath = resolveResourcePath(request);
        if (create && !StringUtils.hasText(resourcePath)) {
            throw new IllegalArgumentException("资源文件路径不能为空");
        }
        String fileName = firstText(request.getFileName(), StringUtils.hasText(resourcePath) ? pathBasename(resourcePath) : null);
        String fileRole = firstText(request.getFileRole(),
                create || StringUtils.hasText(resourcePath) || request.getDirectory() != null
                        ? inferFileRole(resourcePath, request.getDirectory())
                        : null);
        String resourceContent = request.getResourceContent() != null
                ? request.getResourceContent()
                : request.getContent();
        if (create && resourceContent == null) {
            resourceContent = "";
        }

        return new AiSkillResourceEntity()
                .setId(request.getId())
                .setSkillId(request.getSkillId())
                .setFileRole(fileRole)
                .setFileName(fileName)
                .setResourcePath(resourcePath)
                .setResourceContent(resourceContent);
    }

    private String resolveResourcePath(AiSkillResourceSaveRequest request) {
        String resourcePath = firstText(request.getResourcePath(), request.getRelativePath());
        if (StringUtils.hasText(resourcePath)) {
            return normalizePath(resourcePath);
        }

        String fileName = request.getFileName();
        if (!StringUtils.hasText(fileName)) {
            return null;
        }
        String parentPath = normalizePath(request.getParentPath());
        return StringUtils.hasText(parentPath)
                ? parentPath + "/" + normalizePath(fileName)
                : normalizePath(fileName);
    }

    private String inferFileRole(String resourcePath, Boolean directory) {
        if (Boolean.TRUE.equals(directory)) {
            return "DIRECTORY";
        }
        String normalizedPath = normalizePath(resourcePath).toLowerCase();
        if ("skill.md".equals(normalizedPath)) {
            return "SKILL_MD";
        }
        if (normalizedPath.startsWith("references/")) {
            return "REFERENCE";
        }
        if (normalizedPath.startsWith("scripts/")) {
            return "SCRIPT";
        }
        if (normalizedPath.startsWith("examples/")) {
            return "EXAMPLE";
        }
        return "ASSET";
    }

    private AiSkillResourceEntity fillAliasFields(AiSkillResourceEntity entity) {
        if (entity == null) {
            return null;
        }
        entity.setRelativePath(entity.getResourcePath());
        entity.setContent(entity.getResourceContent());
        entity.setDirectory("DIRECTORY".equals(entity.getFileRole()));
        return entity;
    }

    private String normalizePath(String path) {
        return String.valueOf(path == null ? "" : path)
                .replace("\\", "/")
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
    }

    private String pathBasename(String path) {
        String normalizedPath = normalizePath(path);
        int index = normalizedPath.lastIndexOf('/');
        return index >= 0 ? normalizedPath.substring(index + 1) : normalizedPath;
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }
}
