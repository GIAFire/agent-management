package com.zhiran.agent.factory.skillFactory;

import com.zhiran.agent.entity.DTO.AgentConfigDTO;
import com.zhiran.agent.service.AiSkillInfoService;
import com.zhiran.common.context.UserInfo;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SkillFactory {
    private final AiSkillInfoService skillInfoService;

    public AgentSkillRepository mysqlSkillFactory(
            AgentConfigDTO config,
            UserInfo userInfo) {
        return new SkillRepository(
                config.getAgentId(),
                config.getAgentConfigId(),
                userInfo.getTenantId(),
                userInfo.getRoleCodes(),
                skillInfoService);
    }
}
