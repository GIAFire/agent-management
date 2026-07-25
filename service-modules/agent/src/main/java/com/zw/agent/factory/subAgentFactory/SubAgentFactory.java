package com.zw.agent.factory.subAgentFactory;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zw.agent.entity.AiAgentEntity;
import com.zw.agent.entity.AiSubagentEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.service.AiAgentService;
import com.zw.agent.service.AiSubagentService;
import io.agentscope.harness.agent.subagent.SubagentDeclaration;
import io.agentscope.harness.agent.subagent.WorkspaceMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class SubAgentFactory {

    private final AiSubagentService subAgentService;
    private final AiAgentService agentService;

    public List<SubagentDeclaration> buildSubAgent(AgentConfigDTO config){
        List<SubagentDeclaration> subAgentBuildList = new ArrayList<>();
        List<AiSubagentEntity> subAgentList = subAgentService.subAgentList(config.getAgentId());

        return subAgentBuildList;
    }

    public List<AiAgentEntity> buildSubAgentFactory(AgentConfigDTO config){
        List<AiAgentEntity> subAgentList = agentService.subAgentList(config.getAgentId());

        return subAgentList;
    }
}
