package com.zw.agent.factory.subAgentFactory;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zw.agent.entity.AiSubagentEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.service.AiSubagentService;
import io.agentscope.harness.agent.subagent.SubagentDeclaration;
import io.agentscope.harness.agent.subagent.WorkspaceMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

    public List<SubagentDeclaration> buildSubAgent(AgentConfigDTO config){
        List<SubagentDeclaration> subAgentBuildList = new ArrayList<>();
        List<AiSubagentEntity> subAgentList = subAgentService.subAgentList(config.getAgentId());
        for (AiSubagentEntity subagent: subAgentList) {
            List<String> toolList = subagent.getToolAllowList() != null ? Arrays.stream(subagent.getToolAllowList().split(",")).collect(Collectors.toList()) : new ArrayList<>();
            subAgentBuildList.add(
                    SubagentDeclaration.builder()
                    .name(subagent.getSubagentName())
                    .description(subagent.getDescription())
                    .workspace(subagent.getWorkspacePath() != null ? Path.of(subagent.getWorkspacePath()) : null)
                    .workspaceMode(WorkspaceMode.ISOLATED)
                    .model(config.getModelName())
                    .persistSession(subagent.getPersistSession() == 1)
                    .steps(subagent.getMaxSteps())
                    .exposeToUser(subagent.getExposeToUser() == 1)
                    .tools(toolList)
                    .build()
            );
        }

        return subAgentBuildList;
    }
}
