package com.zw.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zw.agent.entity.AiAgentWorkspaceFileEntity;
import com.zw.agent.entity.DTO.AiSkillFileSaveRequest;
import com.zw.agent.entity.AiSkillFileEntity;
import com.zw.agent.entity.AiSkillInfoEntity;
import com.zw.agent.mapper.AiSkillInfoMapper;
import com.zw.agent.mapper.AiSkillFileMapper;
import com.zw.agent.service.AiAgentWorkspaceFileService;
import com.zw.agent.service.AiSkillFileService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.common.support.EntityDefaults;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

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
public class AiSkillFileServiceImpl extends ServiceImpl<AiSkillFileMapper, AiSkillFileEntity> implements AiSkillFileService {

    private static final String SKILL_MD_ROLE = "SKILL_MD";
    private static final String SKILL_MD_PATH = "SKILL.md";
    private static final String DIRECTORY_ROLE = "DIRECTORY";
    private static final String DIRECTORY_MIME_TYPE = "inode/directory";
    private static final String LOCAL_STORAGE = "LOCAL";

    private final AiSkillInfoMapper skillInfoMapper;
    private final AiAgentWorkspaceFileService workspaceFileService;

    @Override
    public AiSkillFileEntity getSkillMdFile(Long skillId) {
        if (skillId == null) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<AiSkillFileEntity>()
                .eq(AiSkillFileEntity::getSkillId, skillId)
                .eq(AiSkillFileEntity::getFileRole, SKILL_MD_ROLE)
                .eq(AiSkillFileEntity::getResourcePath, SKILL_MD_PATH)
                .last("LIMIT 1"));
    }

    @Override
    public AiSkillFileEntity saveOrUpdateSkillMdFile(AiSkillInfoEntity skill, AiAgentWorkspaceFileEntity workspaceFile) {
        AiSkillFileEntity entity = getSkillMdFile(skill.getId());
        if (entity == null) {
            entity = new AiSkillFileEntity();
        }

        entity.setTenantId(skill.getTenantId());
        entity.setSkillId(skill.getId());
        entity.setFileRole(SKILL_MD_ROLE);
        entity.setFileName(SKILL_MD_PATH);
        entity.setResourcePath(SKILL_MD_PATH);
        entity.setMimeType(workspaceFile.getMimeType());
        entity.setFileExt(workspaceFile.getFileExt());
        entity.setSizeBytes(workspaceFile.getSizeBytes());
        entity.setChecksum(workspaceFile.getChecksum());
        entity.setStorageBackend(workspaceFile.getStorageBackend());
        entity.setStorageKey(workspaceFile.getStorageKey());
        entity.setWorkspaceFileId(workspaceFile.getId());
        entity.setExecutable((byte) 0);

        if (entity.getId() == null) {
            save(EntityDefaults.create(entity));
            return entity;
        }
        updateById(EntityDefaults.update(entity));
        return entity;
    }

    @Override
    public List<AiSkillFileEntity> listBySkillId(Long skillId) {
        return list(new LambdaQueryWrapper<AiSkillFileEntity>()
                .eq(AiSkillFileEntity::getSkillId, skillId)
                .orderByAsc(AiSkillFileEntity::getResourcePath));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiSkillFileEntity createSkillPackageNode(AiSkillFileSaveRequest request) {
        AiSkillInfoEntity skill = requireSkill(request.getSkillId());
        String relativePath = normalizeRelativePath(request.getRelativePath(), request.getParentPath(), request.getFileName());
        ensureUniquePath(skill.getId(), relativePath);

        if (Boolean.TRUE.equals(request.getDirectory())) {
            AiSkillFileEntity directory = buildDirectoryEntity(skill, relativePath);
            save(EntityDefaults.create(directory));
            return directory;
        }

        String fileName = StringUtils.getFilename(relativePath);
        String content = request.getContent() == null ? "" : request.getContent();
        AiAgentWorkspaceFileEntity workspaceFile = workspaceFileService.saveSkillPackageFile(
                skill,
                relativePath,
                fileName,
                content
        );
        AiSkillFileEntity entity = buildFileEntity(skill, workspaceFile, relativePath, resolveFileRole(request.getFileRole(), relativePath));
        entity.setExecutable(request.getExecutable() == null ? (byte) 0 : request.getExecutable());
        save(EntityDefaults.create(entity));
        if (SKILL_MD_ROLE.equals(entity.getFileRole())) {
            skill.setSkillContent(content);
            skillInfoMapper.updateById(EntityDefaults.update(skill));
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiSkillFileEntity updateSkillPackageFile(AiSkillFileSaveRequest request) {
        AiSkillFileEntity entity = requireFile(request.getId());
        if (DIRECTORY_ROLE.equals(entity.getFileRole())) {
            throw new IllegalArgumentException("文件夹不能保存文本内容");
        }

        AiSkillInfoEntity skill = requireSkill(entity.getSkillId());
        AiAgentWorkspaceFileEntity workspaceFile = entity.getWorkspaceFileId() == null
                ? null
                : workspaceFileService.getById(entity.getWorkspaceFileId());
        String content = request.getContent() == null ? "" : request.getContent();
        workspaceFile = workspaceFileService.updateSkillPackageFile(
                workspaceFile,
                skill,
                entity.getResourcePath(),
                entity.getFileName(),
                content
        );

        entity.setMimeType(workspaceFile.getMimeType());
        entity.setFileExt(workspaceFile.getFileExt());
        entity.setSizeBytes(workspaceFile.getSizeBytes());
        entity.setChecksum(workspaceFile.getChecksum());
        entity.setStorageBackend(workspaceFile.getStorageBackend());
        entity.setStorageKey(workspaceFile.getStorageKey());
        entity.setWorkspaceFileId(workspaceFile.getId());
        updateById(EntityDefaults.update(entity));

        if (SKILL_MD_ROLE.equals(entity.getFileRole())) {
            skill.setSkillContent(content);
            skillInfoMapper.updateById(EntityDefaults.update(skill));
        }
        return entity;
    }

    @Override
    public String readSkillPackageFile(Long id) {
        AiSkillFileEntity entity = requireFile(id);
        if (DIRECTORY_ROLE.equals(entity.getFileRole()) || !StringUtils.hasText(entity.getStorageKey())) {
            return "";
        }
        return workspaceFileService.readSkillPackageFile(entity.getStorageKey());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteSkillPackageNode(Long id) {
        AiSkillFileEntity entity = requireFile(id);
        String prefix = entity.getResourcePath() + "/";
        List<AiSkillFileEntity> targets = list(new LambdaQueryWrapper<AiSkillFileEntity>()
                .eq(AiSkillFileEntity::getSkillId, entity.getSkillId())
                .and(wrapper -> wrapper
                        .eq(AiSkillFileEntity::getId, entity.getId())
                        .or()
                        .likeRight(AiSkillFileEntity::getResourcePath, prefix)));

        for (AiSkillFileEntity target : targets) {
            if (target.getWorkspaceFileId() != null) {
                workspaceFileService.removeById(target.getWorkspaceFileId());
            }
            if (StringUtils.hasText(target.getStorageKey())) {
                workspaceFileService.deleteSkillPackageStorage(target.getStorageKey());
            }
            removeById(target.getId());
        }
        return true;
    }

    private AiSkillInfoEntity requireSkill(Long skillId) {
        if (skillId == null) {
            throw new IllegalArgumentException("技能ID不能为空");
        }
        AiSkillInfoEntity skill = skillInfoMapper.selectById(skillId);
        if (skill == null) {
            throw new IllegalArgumentException("技能不存在");
        }
        return skill;
    }

    private AiSkillFileEntity requireFile(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("文件ID不能为空");
        }
        AiSkillFileEntity entity = getById(id);
        if (entity == null) {
            throw new IllegalArgumentException("文件不存在");
        }
        return entity;
    }

    private void ensureUniquePath(Long skillId, String relativePath) {
        long count = count(new LambdaQueryWrapper<AiSkillFileEntity>()
                .eq(AiSkillFileEntity::getSkillId, skillId)
                .eq(AiSkillFileEntity::getResourcePath, relativePath));
        if (count > 0) {
            throw new IllegalArgumentException("同目录下已存在同名文件或文件夹");
        }
    }

    private AiSkillFileEntity buildDirectoryEntity(AiSkillInfoEntity skill, String relativePath) {
        AiSkillFileEntity entity = new AiSkillFileEntity();
        entity.setTenantId(skill.getTenantId());
        entity.setSkillId(skill.getId());
        entity.setFileRole(DIRECTORY_ROLE);
        entity.setFileName(StringUtils.getFilename(relativePath));
        entity.setResourcePath(relativePath);
        entity.setMimeType(DIRECTORY_MIME_TYPE);
        entity.setFileExt("");
        entity.setSizeBytes(0L);
        entity.setStorageBackend(LOCAL_STORAGE);
        entity.setStorageKey(null);
        entity.setWorkspaceFileId(null);
        entity.setExecutable((byte) 0);
        return entity;
    }

    private AiSkillFileEntity buildFileEntity(
            AiSkillInfoEntity skill,
            AiAgentWorkspaceFileEntity workspaceFile,
            String relativePath,
            String fileRole
    ) {
        AiSkillFileEntity entity = new AiSkillFileEntity();
        entity.setTenantId(skill.getTenantId());
        entity.setSkillId(skill.getId());
        entity.setFileRole(fileRole);
        entity.setFileName(StringUtils.getFilename(relativePath));
        entity.setResourcePath(relativePath);
        entity.setMimeType(workspaceFile.getMimeType());
        entity.setFileExt(workspaceFile.getFileExt());
        entity.setSizeBytes(workspaceFile.getSizeBytes());
        entity.setChecksum(workspaceFile.getChecksum());
        entity.setStorageBackend(workspaceFile.getStorageBackend());
        entity.setStorageKey(workspaceFile.getStorageKey());
        entity.setWorkspaceFileId(workspaceFile.getId());
        entity.setExecutable((byte) 0);
        return entity;
    }

    private String normalizeRelativePath(String relativePath, String parentPath, String fileName) {
        String path = StringUtils.hasText(relativePath)
                ? relativePath
                : joinPath(parentPath, fileName);
        path = path == null ? "" : path.replace("\\", "/").replaceAll("^/+", "").replaceAll("/+$", "");
        path = StringUtils.cleanPath(path);
        if (!StringUtils.hasText(path) || path.startsWith("../") || path.contains("/../") || "..".equals(path)) {
            throw new IllegalArgumentException("非法文件路径");
        }
        return path;
    }

    private String joinPath(String parentPath, String fileName) {
        String name = fileName == null ? "" : fileName.trim();
        if (!StringUtils.hasText(name) || name.contains("/") || name.contains("\\")) {
            throw new IllegalArgumentException("文件名不能为空且不能包含路径分隔符");
        }
        if (!StringUtils.hasText(parentPath)) {
            return name;
        }
        return parentPath.replace("\\", "/").replaceAll("/+$", "") + "/" + name;
    }

    private String resolveFileRole(String requestedRole, String relativePath) {
        if (StringUtils.hasText(requestedRole)) {
            return requestedRole;
        }
        if (SKILL_MD_PATH.equals(relativePath)) {
            return SKILL_MD_ROLE;
        }
        if (relativePath.startsWith("references/")) {
            return "REFERENCE";
        }
        if (relativePath.startsWith("scripts/")) {
            return "SCRIPT";
        }
        if (relativePath.startsWith("examples/")) {
            return "EXAMPLE";
        }
        return "ASSET";
    }
}
