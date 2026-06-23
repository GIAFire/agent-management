package com.zw.agent.runtime;

import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.event.AgentRuntimeEvent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.credential.OpenAICredential;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentEventType;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.formatter.openai.OpenAIChatFormatter;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
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
     * 调用Agent执行用户消息并获取响应。
     * 该方法会先通过配置获取或创建Agent实例，然后构建运行时上下文，
     * 最后异步执行Agent调用并返回文本格式的响应内容。
     *
     * @param config Agent运行时配置，用于获取或创建Agent实例
     * @param tenantUserId 运行时用户标识键，用于构建运行时上下文
     * @param sessionId 会话标识键，用于跟踪会话状态
     * @param text 用户输入的文本消息内容
     * @return Mono封装的字符串响应，包含Agent处理后的文本内容
     */

    public Flux<AgentRuntimeEvent> callStreamEvents(
            AgentConfigDTO config,
            String tenantUserId,
            Long sessionId,
            String text
    ) {
        // 通过Agent配置,获取或创建Agent实例
        HarnessAgent harnessAgent = getOrCreateAgent(config);

        // 通过租户ID-用户ID,还有sessionId,构建运行时上下文
        RuntimeContext context = RuntimeContext.builder()
                .userId(tenantUserId)
                .sessionId(AgentRuntimeKeys.sessionKey(String.valueOf(sessionId)))
                .build();
        text += "我叫李四,我的角色是管理员2";
        // 获取用户消息
        UserMessage userMessage = new UserMessage(text);

        // 获取Agent事件流
        Flux<AgentEvent> agentEventFlux = harnessAgent.streamEvents(userMessage, context);
        return agentEventFlux
                .map(event -> {
                    if (event.getType() == AgentEventType.TEXT_BLOCK_DELTA) {
                        TextBlockDeltaEvent textEvent = (TextBlockDeltaEvent) event;
                        return new AgentRuntimeEvent(
                                event.getType().name(),
                                "message",
                                textEvent.getDelta(),
                                event
                        );
                    }

                    return new AgentRuntimeEvent(
                            event.getType().name(),
                            "agent_event",
                            null,
                            event
                    );
                });
    }
    public Mono<String> call(AgentConfigDTO config, String TenantUserId, Long sessionId, String text) {
        /**
         * 构造模型实例
         */
        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .baseUrl(config.getBaseUrl())
                .stream(config.getIsStream())
                .formatter(new OpenAIChatFormatter())
                .build();
        String cacheKey = config.getTenantId() + ":" + config.getAgentId() + ":" + config.getAgentConfigId();
        HarnessAgent harnessAgent = cache.computeIfAbsent(cacheKey, key ->
                HarnessAgent.builder()
                        .name(config.getAgentName())
                        .sysPrompt(config.getSysPrompt())
                        .model(model)
                        .build());
        // 构建运行时上下文
        RuntimeContext context = RuntimeContext.builder()
                .userId(AgentRuntimeKeys.pathSafeSegment(TenantUserId, "anonymous"))
                .sessionId(AgentRuntimeKeys.sessionKey(String.valueOf(sessionId)))
                .build();

        return harnessAgent.call(new UserMessage(text), context)
                .map(msg -> msg.getTextContent());
    }

    public HarnessAgent getOrCreateAgent(AgentConfigDTO config) {
        String cacheKey = config.getTenantId() + ":" + config.getAgentId() + ":" + config.getAgentConfigId();

        return cache.computeIfAbsent(cacheKey, key -> {
            OpenAIChatModel model = OpenAIChatModel.builder()
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelName())
                    .baseUrl(config.getBaseUrl())
                    .stream(config.getIsStream())
                    .formatter(new OpenAIChatFormatter())
                    .build();

            return HarnessAgent.builder()
                    .name(config.getAgentName())
                    .sysPrompt(config.getSysPrompt())
                    .model(model)
                    .build();
        });
    }


    public void evictAgent(Long tenantId, Long agentId, Long agentConfigId) {
        String cacheKey = tenantId + ":" + agentId + ":" + agentConfigId;
        cache.remove(cacheKey);
    }
}
