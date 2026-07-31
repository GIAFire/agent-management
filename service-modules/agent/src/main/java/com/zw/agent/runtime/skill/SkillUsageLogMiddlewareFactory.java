package com.zw.agent.runtime.skill;

import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.service.AiSkillInfoService;
import com.zw.agent.service.AiSkillLogService;
import io.agentscope.core.middleware.MiddlewareBase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SkillUsageLogMiddlewareFactory {

    private final AiSkillInfoService skillService;
    private final AiSkillLogService logService;

    public MiddlewareBase create(AgentConfigDTO config) {
        return new SkillUsageLogMiddleware(skillService, logService, config);
    }
}
