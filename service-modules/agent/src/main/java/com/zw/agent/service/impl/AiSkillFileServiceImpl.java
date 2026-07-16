package com.zw.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zw.agent.entity.AiAgentWorkspaceFileEntity;
import com.zw.agent.entity.AiSkillFileEntity;
import com.zw.agent.entity.AiSkillInfoEntity;
import com.zw.agent.mapper.AiSkillFileMapper;
import com.zw.agent.service.AiSkillFileService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.common.support.EntityDefaults;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Skill附属文件表：保存Skill目录下的SKILL.md、references、scripts和样例资源 服务实现类
 * </p>
 *
 * @author 智伟
 * @since 2026-07-16
 */
@Service
public class AiSkillFileServiceImpl extends ServiceImpl<AiSkillFileMapper, AiSkillFileEntity> implements AiSkillFileService {

    private static final String SKILL_MD_ROLE = "SKILL_MD";
    private static final String SKILL_MD_PATH = "SKILL.md";

    @Override
    public AiSkillFileEntity getSkillMdFile(Long skillId) {
        if (skillId == null) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<AiSkillFileEntity>()
                .eq(AiSkillFileEntity::getSkillId, skillId)
                .eq(AiSkillFileEntity::getFileRole, SKILL_MD_ROLE)
                .eq(AiSkillFileEntity::getRelativePath, SKILL_MD_PATH)
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
        entity.setRelativePath(SKILL_MD_PATH);
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
}
