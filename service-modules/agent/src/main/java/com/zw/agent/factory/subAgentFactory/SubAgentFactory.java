package com.zw.agent.factory.subAgentFactory;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zw.agent.entity.AiAgentEntity;
import com.zw.agent.entity.AiHttpHeaderEntity;
import com.zw.agent.entity.AiSubagentEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.entity.DTO.SubagentHeaderDTO;
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
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class SubAgentFactory {

    private final AiSubagentService subAgentService;
    private final AiAgentService agentService;

    public List<SubagentDeclaration> buildRemoteSubAgent(AgentConfigDTO config){
        List<SubagentDeclaration> subAgentBuildList = new ArrayList<>();
        List<SubagentHeaderDTO> remoteSubagentSubAgentList =
                subAgentService.subAgentList(config.getAgentId(), config.getTenantId());
        for (SubagentHeaderDTO remoteSubagent: remoteSubagentSubAgentList) {
            Map<String, String> headers = remoteSubagent.getHeaderEntityList() != null
                    ? remoteSubagent.getHeaderEntityList().stream()
                    .collect(Collectors.toMap(
                            AiHttpHeaderEntity::getHeaderName,
                            AiHttpHeaderEntity::getHeaderValue,
                            (v1, v2) -> v1
                    ))
                    : Map.of();
            subAgentBuildList.add(
                    SubagentDeclaration.builder()
                            .name(remoteSubagent.getSubagentName())
                            .description(remoteSubagent.getDescription())
                            .url(remoteSubagent.getRemoteUrl())
                            .headers(headers)
                            .build()
            );
        }
        return subAgentBuildList;
    }

    public List<AiAgentEntity> buildSubAgentFactory(AgentConfigDTO config){
        List<AiAgentEntity> subAgentList =
                agentService.subAgentList(config.getAgentId(), config.getTenantId());

        return subAgentList;
    }
}
