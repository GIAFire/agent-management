package com.zw.agent.factory.compactionFactory;

import com.zw.agent.entity.DTO.AgentConfigDTO;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class CompactionFactory {

    public CompactionConfig buildCompaction(
            AgentConfigDTO config
    ){
        return CompactionConfig.builder()
                .triggerMessages(
                        Optional.ofNullable(config.getTriggerMessages())
                                .orElse(50)
                )
                .keepMessages(
                        Optional.ofNullable(config.getKeepMessages())
                                .orElse(20)
                )
                .triggerTokens(
                        Optional.ofNullable(config.getTriggerTokens())
                                .orElse(8000)
                )
                .keepTokens(
                        Optional.ofNullable(config.getKeepTokens())
                                .orElse(1)
                )
                .truncateArgs(CompactionConfig.TruncateArgsConfig.builder()
                        .maxArgLength(1000)
                        .truncationText("... [truncated] ...")
                        .build())
                .build();
    }
}
