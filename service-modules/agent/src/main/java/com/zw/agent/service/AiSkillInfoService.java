package com.zw.agent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zw.agent.entity.AiSkillInfoEntity;
import com.zw.agent.entity.DTO.SkillFileDTO;
import java.util.Collection;
import java.util.List;

public interface AiSkillInfoService extends IService<AiSkillInfoEntity> {

    SkillFileDTO getAgentSkill(
            String skillCode,
            Long agentId,
            Long agentConfigId,
            Long tenantId,
            Collection<String> roleCodes
    );

    List<SkillFileDTO> getAgentSkillNames(
            Long agentId,
            Long agentConfigId,
            Long tenantId,
            Collection<String> roleCodes
    );
}
