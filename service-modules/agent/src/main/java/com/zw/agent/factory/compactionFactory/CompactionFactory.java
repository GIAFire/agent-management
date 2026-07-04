package com.zw.agent.factory.compactionFactory;

import com.zw.agent.entity.DTO.AgentConfigDTO;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class CompactionFactory {

    public CompactionConfig buildCompaction(
            AgentConfigDTO config
    ){
        return CompactionConfig.builder()
                .triggerMessages(config.getTriggerMessages())     // 30 条触发
                .keepMessages(config.getKeepMessages())        // 压缩后保留最近 10 条原文
                .truncateArgs(CompactionConfig.TruncateArgsConfig.builder()
                        .maxArgLength(1000)
                        .truncationText("... [truncated] ...")
                        .build())                                   // 截断大文本参数长度
                .build();
    }
}
