package com.zw.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zw.agent.constant.AgentConstant;
import com.zw.agent.entity.AiAgentWorkspaceFileEntity;
import com.zw.agent.entity.AiSkillFileEntity;
import com.zw.agent.entity.AiSkillInfoEntity;
import com.zw.agent.entity.DTO.SkillFileDTO;
import com.zw.agent.mapper.AiSkillInfoMapper;
import com.zw.agent.service.AiAgentWorkspaceFileService;
import com.zw.agent.service.AiSkillFileService;
import com.zw.agent.service.AiSkillInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.common.context.UserContext;
import com.zw.common.context.UserInfo;
import com.zw.common.support.EntityDefaults;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.zw.agent.constant.AgentConstant.DEFAULT_SKILL_MD_FILE;

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


    private final AiAgentWorkspaceFileService workspaceFileService;
    private final AiSkillFileService skillFileService;
    private final AiSkillInfoMapper skillInfoMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiSkillInfoEntity createSkillPackage(AiSkillInfoEntity entity) {
        applyCreateDefaults(entity);
        save(EntityDefaults.create(entity));
        // 保存到工作区
//        saveOrUpdateSkillMdArtifacts(entity);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiSkillInfoEntity updateSkillPackage(AiSkillInfoEntity entity) {
        AiSkillInfoEntity existing = getById(entity.getId());
        if (existing == null) {
            throw new IllegalArgumentException("技能不存在");
        }

        applyUpdateDefaults(entity, existing);
        updateById(EntityDefaults.update(entity));

        AiSkillInfoEntity updated = getById(entity.getId());
        saveOrUpdateSkillMdArtifacts(updated);
        return updated;
    }

    private void saveOrUpdateSkillMdArtifacts(AiSkillInfoEntity skill) {
        AiSkillFileEntity skillFile = skillFileService.getSkillMdFile(skill.getId());
        AiAgentWorkspaceFileEntity workspaceFile = null;
        if (skillFile != null && skillFile.getWorkspaceFileId() != null) {
            workspaceFile = workspaceFileService.getById(skillFile.getWorkspaceFileId());
        }

        if (workspaceFile == null) {
            workspaceFile = workspaceFileService.saveSkillPackageFile(
                    skill,
                    DEFAULT_SKILL_MD_FILE,
                    DEFAULT_SKILL_MD_FILE,
                    skill.getSkillContent()
            );
        } else {
            workspaceFile = workspaceFileService.updateSkillPackageFile(
                    workspaceFile,
                    skill,
                    DEFAULT_SKILL_MD_FILE,
                    DEFAULT_SKILL_MD_FILE,
                    skill.getSkillContent()
            );
        }

        skillFileService.saveOrUpdateSkillMdFile(skill, workspaceFile);
    }

    private void applyCreateDefaults(AiSkillInfoEntity entity) {
        UserInfo userInfo = UserContext.get();
        if (entity.getTenantId() == null && userInfo != null) {
            entity.setTenantId(userInfo.getTenantId());
        }
        if (!StringUtils.hasText(entity.getSkillContent())) {
            entity.setSkillContent(buildDefaultSkillMd(entity));
        }
        if (!StringUtils.hasText(entity.getRiskLevel())) {
            entity.setRiskLevel("LOW");
        }
        if (entity.getStatus() == null) {
            entity.setStatus((byte) 1);
        }
    }

    private void applyUpdateDefaults(AiSkillInfoEntity entity, AiSkillInfoEntity existing) {
        if (entity.getTenantId() == null) {
            entity.setTenantId(existing.getTenantId());
        }
        if (!StringUtils.hasText(entity.getName())) {
            entity.setName(existing.getName());
        }
        if (!StringUtils.hasText(entity.getDescription())) {
            entity.setDescription(existing.getDescription());
        }
        if (!StringUtils.hasText(entity.getSkillContent())) {
            entity.setSkillContent(existing.getSkillContent());
        }
        if (!StringUtils.hasText(entity.getRiskLevel())) {
            entity.setRiskLevel(existing.getRiskLevel());
        }
        if (entity.getTagsJson() == null) {
            entity.setTagsJson(existing.getTagsJson());
        }
        if (entity.getStatus() == null) {
            entity.setStatus(existing.getStatus());
        }
    }

    private String buildDefaultSkillMd(AiSkillInfoEntity entity) {
        String name = StringUtils.hasText(entity.getName()) ? entity.getName() : "未命名技能";
        String description = StringUtils.hasText(entity.getDescription()) ? entity.getDescription() : "请补充技能说明";
        return String.join("\n",
                "---",
                "name: " + name,
                "description: " + description,
                "---",
                "",
                "# " + name,
                "",
                "## 使用场景",
                "- " + description,
                "",
                "## 工作流程",
                "1. 理解任务输入和约束。",
                "2. 选择合适的工具或业务流程执行任务。",
                "3. 输出结构化结果，并说明关键依据。",
                "");
    }


    @Override
    public Boolean removeSkillFile(Long id) {
        skillInfoMapper.deleteById(id);
        skillFileService.remove(new LambdaQueryWrapper<AiSkillFileEntity>()
                .eq(AiSkillFileEntity::getSkillId, id));
        workspaceFileService.remove(new LambdaQueryWrapper<AiAgentWorkspaceFileEntity>()
                .eq(AiAgentWorkspaceFileEntity::getSkillId, id));


        return workspaceFileService.remove(new LambdaQueryWrapper<AiAgentWorkspaceFileEntity>()
                .eq(AiAgentWorkspaceFileEntity::getSkillId, id));
    }

    @Override
    public SkillFileDTO getAgentSkill(String name,Long agentId) {
        return skillInfoMapper.getAgentSkill(name,agentId);
    }

    @Override
    public List<SkillFileDTO> getAgentSkillName(Long agentId) {
        return skillInfoMapper.getAgentSkillName(agentId);
    }
}
