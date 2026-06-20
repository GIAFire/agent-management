package com.zw.agent.runtime;

import com.zw.RedisService;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentRuntimeFactory {

    private final Map<String, HarnessAgent> cache = new ConcurrentHashMap<>();

    public HarnessAgent getOrCreate(AgentRuntimeConfig config) {
        String cacheKey = config.tenantId() + ":" + config.agentId() + ":" + config.versionNo();
        return cache.computeIfAbsent(cacheKey, key ->
                HarnessAgent.builder()
                        .name(config.agentName())
                        .sysPrompt(config.sysPrompt())
                        .model(config.provider() + ":" + config.modelName())
                        .workspace(Path.of(config.workspacePath()))
                        .compaction(
                                CompactionConfig.builder()
                                        .triggerMessages(config.compactionTriggerMessages())
                                        .keepMessages(config.compactionKeepMessages())
                                        .build()
                        )
                        .build()
        );
    }

    public Mono<String> call(AgentRuntimeConfig config, String runtimeUserKey, String sessionKey, String text) {
        HarnessAgent agent = getOrCreate(config);

        RuntimeContext context = RuntimeContext.builder()
                // TODO userID拼成租户ID:用户ID,后续方便Redis/MySQL/OSS天然租户隔离
                .userId(runtimeUserKey)
                .sessionId(sessionKey)
                .build();

        return agent.call(new UserMessage(text), context)
                .map(msg -> msg.getTextContent());
    }
}