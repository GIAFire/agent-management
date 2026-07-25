package com.zw.agent.factory.skillFactory;

import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.service.AiSkillInfoService;
import com.zw.agent.service.AiSkillResourceService;
import com.zw.common.context.UserInfo;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Slf4j
@Component
@RequiredArgsConstructor
public class SkillFactory {
    private final AiSkillInfoService skillInfoService;
    private final AiSkillResourceService skillResourceService;

    public AgentSkillRepository mysqlSkillFactory(
            AgentConfigDTO config,
            UserInfo userInfo) {
        return new SkillRepository(
                config.getAgentId(),
                userInfo.getTenantId(),
                skillInfoService,
                skillResourceService);
    }
}
