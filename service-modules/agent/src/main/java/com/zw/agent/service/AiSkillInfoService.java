package com.zw.agent.service;

import com.zw.agent.entity.AiSkillInfoEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zw.agent.entity.DTO.AiSkillInfoSaveRequest;
import com.zw.agent.entity.DTO.SkillFileDTO;

import java.util.List;

/**
 * <p>
 * Skill定义表：保存可复用能力包的基础信息和当前发布版本 服务类
 * </p>
 *
 * @author 智伟
 * @since 2026-07-16
 */
public interface AiSkillInfoService extends IService<AiSkillInfoEntity> {

    SkillFileDTO getAgentSkill(String name,Long agentId, Long tenantId);

    List<SkillFileDTO> getAgentSkillName(Long agentId, Long tenantId);

    AiSkillInfoEntity createWithRoles(AiSkillInfoSaveRequest request);

    Boolean updateWithRoles(AiSkillInfoSaveRequest request);

}
