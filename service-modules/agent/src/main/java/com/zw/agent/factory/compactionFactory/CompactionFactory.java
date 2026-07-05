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
                                .orElse(50)  // 默认值
                )     // 50 条触发压缩
                .keepMessages(
                        Optional.ofNullable(config.getKeepMessages())
                                .orElse(20)
                )        // 压缩后保留最近 20 条原文
                .triggerTokens(
                        Optional.ofNullable(config.getTriggerTokens())
                                .orElse(8000)
                )       // 按 token 估算触发上下文压缩
                .keepTokens(
                        Optional.ofNullable(config.getKeepTokens())
                                .orElse(1)
                )       // 非 0 时按 token 预算从尾部往前算，覆盖 keepMessages
                .truncateArgs(CompactionConfig.TruncateArgsConfig.builder()
                        .maxArgLength(1000)
                        .truncationText("... [truncated] ...")
                        .build())    // 截断大文本参数长度
                .build();
    }
}
