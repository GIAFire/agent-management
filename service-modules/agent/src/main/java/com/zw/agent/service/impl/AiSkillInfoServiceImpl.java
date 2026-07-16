package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentWorkspaceFileEntity;
import com.zw.agent.entity.AiSkillFileEntity;
import com.zw.agent.entity.AiSkillInfoEntity;
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

    private static final String DEFAULT_SKILL_MD_FILE = "SKILL.md";

    private final AiAgentWorkspaceFileService workspaceFileService;
    private final AiSkillFileService skillFileService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiSkillInfoEntity createSkillPackage(AiSkillInfoEntity entity) {
        applyCreateDefaults(entity);
        save(EntityDefaults.create(entity));
        saveOrUpdateSkillMdArtifacts(entity);
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
                    skill.getSkillMdContent()
            );
        } else {
            workspaceFile = workspaceFileService.updateSkillPackageFile(
                    workspaceFile,
                    skill,
                    DEFAULT_SKILL_MD_FILE,
                    DEFAULT_SKILL_MD_FILE,
                    skill.getSkillMdContent()
            );
        }

        skillFileService.saveOrUpdateSkillMdFile(skill, workspaceFile);
    }

    private void applyCreateDefaults(AiSkillInfoEntity entity) {
        UserInfo userInfo = UserContext.get();
        if (entity.getTenantId() == null && userInfo != null) {
            entity.setTenantId(userInfo.getTenantId());
        }
        if (!StringUtils.hasText(entity.getSkillMdContent())) {
            entity.setSkillMdContent(buildDefaultSkillMd(entity));
        }
        if (!StringUtils.hasText(entity.getRiskLevel())) {
            entity.setRiskLevel("LOW");
        }
        if (entity.getRequiresShell() == null) {
            entity.setRequiresShell((byte) 0);
        }
        if (entity.getRequiresSandbox() == null) {
            entity.setRequiresSandbox((byte) 0);
        }
        if (!StringUtils.hasText(entity.getScopeType())) {
            entity.setScopeType("TENANT");
        }
        if (entity.getStatus() == null) {
            entity.setStatus((byte) 1);
        }
    }

    private void applyUpdateDefaults(AiSkillInfoEntity entity, AiSkillInfoEntity existing) {
        if (entity.getTenantId() == null) {
            entity.setTenantId(existing.getTenantId());
        }
        if (!StringUtils.hasText(entity.getSkillKey())) {
            entity.setSkillKey(existing.getSkillKey());
        }
        if (!StringUtils.hasText(entity.getSkillName())) {
            entity.setSkillName(existing.getSkillName());
        }
        if (!StringUtils.hasText(entity.getDescription())) {
            entity.setDescription(existing.getDescription());
        }
        if (!StringUtils.hasText(entity.getSkillMdContent())) {
            entity.setSkillMdContent(existing.getSkillMdContent());
        }
        if (!StringUtils.hasText(entity.getRiskLevel())) {
            entity.setRiskLevel(existing.getRiskLevel());
        }
        if (entity.getRequiresShell() == null) {
            entity.setRequiresShell(existing.getRequiresShell());
        }
        if (entity.getRequiresSandbox() == null) {
            entity.setRequiresSandbox(existing.getRequiresSandbox());
        }
        if (!StringUtils.hasText(entity.getScopeType())) {
            entity.setScopeType(existing.getScopeType());
        }
        if (entity.getScopeValue() == null) {
            entity.setScopeValue(existing.getScopeValue());
        }
        if (entity.getCategory() == null) {
            entity.setCategory(existing.getCategory());
        }
        if (entity.getTagsJson() == null) {
            entity.setTagsJson(existing.getTagsJson());
        }
        if (entity.getStatus() == null) {
            entity.setStatus(existing.getStatus());
        }
    }

    private String buildDefaultSkillMd(AiSkillInfoEntity entity) {
        String name = StringUtils.hasText(entity.getSkillName()) ? entity.getSkillName() : "未命名技能";
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
}
