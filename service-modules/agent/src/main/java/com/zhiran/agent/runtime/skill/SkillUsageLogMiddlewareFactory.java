package com.zhiran.agent.runtime.skill;

import com.zhiran.agent.entity.DTO.AgentConfigDTO;
import com.zhiran.agent.service.AiSkillInfoService;
import com.zhiran.agent.service.AiSkillLogService;
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
