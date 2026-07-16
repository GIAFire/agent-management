package com.zw.agent.service;

import com.zw.agent.entity.AiSkillFileEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zw.agent.entity.AiAgentWorkspaceFileEntity;
import com.zw.agent.entity.DTO.AiSkillFileSaveRequest;
import com.zw.agent.entity.AiSkillInfoEntity;

import java.util.List;

/**
 * <p>
 * Skill附属文件表：保存Skill目录下的SKILL.md、references、scripts和样例资源 服务类
 * </p>
 *
 * @author 智伟
 * @since 2026-07-16
 */
public interface AiSkillFileService extends IService<AiSkillFileEntity> {

    AiSkillFileEntity getSkillMdFile(Long skillId);

    AiSkillFileEntity saveOrUpdateSkillMdFile(AiSkillInfoEntity skill, AiAgentWorkspaceFileEntity workspaceFile);

    List<AiSkillFileEntity> listBySkillId(Long skillId);

    AiSkillFileEntity createSkillPackageNode(AiSkillFileSaveRequest request);

    AiSkillFileEntity updateSkillPackageFile(AiSkillFileSaveRequest request);

    String readSkillPackageFile(Long id);

    Boolean deleteSkillPackageNode(Long id);
}
