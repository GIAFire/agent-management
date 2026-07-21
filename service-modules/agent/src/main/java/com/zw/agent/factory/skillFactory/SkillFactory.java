package com.zw.agent.factory.skillFactory;

import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.common.context.UserInfo;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Slf4j
@Component
public class SkillFactory {
    @Autowired
    DataSource dataSource;
    @Autowired
    SkillRepository SkillRepository;

    public AgentSkillRepository mysqlSkillFactory(AgentConfigDTO config, UserInfo userInfo) {
        SkillRepository.setAgentId(config.getAgentId());
        SkillRepository.setUserInfo(userInfo);
        return SkillRepository;
    }
}
