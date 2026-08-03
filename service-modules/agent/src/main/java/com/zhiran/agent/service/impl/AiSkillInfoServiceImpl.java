package com.zhiran.agent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiran.agent.entity.AiSkillInfoEntity;
import com.zhiran.agent.entity.DTO.SkillFileDTO;
import com.zhiran.agent.mapper.AiSkillInfoMapper;
import com.zhiran.agent.service.AiSkillInfoService;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiSkillInfoServiceImpl
        extends ServiceImpl<AiSkillInfoMapper, AiSkillInfoEntity>
        implements AiSkillInfoService {

    private final AiSkillInfoMapper skillInfoMapper;

    @Override
    public SkillFileDTO getAgentSkill(
            String skillCode,
            Long agentId,
            Long agentConfigId,
            Long tenantId,
            Collection<String> roleCodes
    ) {
        return skillInfoMapper.getAgentSkill(
                skillCode, agentId, agentConfigId, tenantId, roleCodes
        );
    }

    @Override
    public List<SkillFileDTO> getAgentSkillNames(
            Long agentId,
            Long agentConfigId,
            Long tenantId,
            Collection<String> roleCodes
    ) {
        return skillInfoMapper.getAgentSkillName(
                agentId, agentConfigId, tenantId, roleCodes
        );
    }
}
