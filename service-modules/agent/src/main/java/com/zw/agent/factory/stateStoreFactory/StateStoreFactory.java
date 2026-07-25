package com.zw.agent.factory.stateStoreFactory;

import com.zw.agent.entity.DTO.AgentConfigDTO;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class StateStoreFactory {

    public AgentStateStore buildStateStore(AgentConfigDTO config){

        return null;
    }
}
