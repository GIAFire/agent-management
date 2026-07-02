package com.zw.agent.factory.toolResultFactory;

import com.zw.agent.entity.DTO.AgentConfigDTO;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import io.agentscope.harness.agent.memory.compaction.ToolResultEvictionConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ToolResultEvictionFactory {

    public ToolResultEvictionConfig buildToolResultEviction(
            AgentConfigDTO config
    ){
        if (config.getToolResultEvictionEnabled())
            return ToolResultEvictionConfig.defaults();
        else
            return null;
    }
}
