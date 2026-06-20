package com.zw.agent.runtime;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Agent运行时工厂类，负责创建和管理HarnessAgent实例。
 * 提供基于租户ID、Agent ID和版本号的缓存机制，确保相同配置的Agent实例复用。
 */
@Component
public class AgentRuntimeFactory {

    /**
     * HarnessAgent实例缓存，使用ConcurrentHashMap保证线程安全
     */
    private final Map<String, HarnessAgent> cache = new ConcurrentHashMap<>();

    /**
     * 根据配置获取或创建HarnessAgent实例。
     * 使用租户ID、Agent ID组合作为缓存键，实现多租户环境下的Agent实例隔离和复用。
     *
     * @param config Agent运行时配置，包含租户信息、Agent标识、模型配置、工作空间路径等
     * @return 构建好的HarnessAgent实例，如果是首次请求则创建新实例，否则返回缓存中的实例
     */
    public HarnessAgent getOrCreate(AgentRuntimeConfig config) {
        String cacheKey = config.tenantId() + ":" + config.agentId() + ":" + config.configId();
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

    /**
     * 调用Agent执行用户消息并获取响应。
     * 该方法会先通过配置获取或创建Agent实例，然后构建运行时上下文，
     * 最后异步执行Agent调用并返回文本格式的响应内容。
     *
     * @param config Agent运行时配置，用于获取或创建Agent实例
     * @param runtimeUserKey 运行时用户标识键，用于构建运行时上下文
     * @param sessionKey 会话标识键，用于跟踪会话状态
     * @param text 用户输入的文本消息内容
     * @return Mono封装的字符串响应，包含Agent处理后的文本内容
     */
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
