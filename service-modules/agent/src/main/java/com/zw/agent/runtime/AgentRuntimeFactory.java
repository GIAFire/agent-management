package com.zw.agent.runtime;

import com.zw.agent.entity.DTO.AgentConfigDTO;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.credential.OpenAICredential;
import io.agentscope.core.formatter.openai.OpenAIChatFormatter;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.OpenAIChatModel;
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
    public Mono<String> call(AgentConfigDTO config, String runtimeUserKey, String sessionKey, String text) {
        /**
         * 构造模型实例
         */
        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .baseUrl(config.getBaseUrl())
                .stream(true)
                .formatter(new OpenAIChatFormatter())
                .build();
        /**
         * 获取Agent实例,其主要职责包括：
         * 1.接收输入消息或事件，调用工具完成任务
         * 2.管理上下文（会话历史保存在 AgentState.getContext() 中，可通过 AgentStateStore 自动持久化）
         * 3.在关键生命周期阶段提供中间件钩子，支持自定义逻辑
         * 4.自动管理并发和串行工具执行
         */
        String cacheKey = config.getTenantId() + ":" + config.getAgentId() + ":" + config.getAgentConfigId();
        HarnessAgent harnessAgent = cache.computeIfAbsent(cacheKey, key ->
                HarnessAgent.builder()
                        .name(config.getAgentName())
                        .sysPrompt(config.getSysPrompt())
                        .model(model)
//                        .workspace(Path.of(config.getWorkspaceConfigJson()))
//                        .compaction(
//                                CompactionConfig.builder()
//                                        .triggerMessages(config.getCompactionTriggerMessages())
//                                        .keepMessages(config.getCompactionKeepMessages())
//                                        .build()
//                        )
                        .build()
        );
        // 构建运行时上下文
        RuntimeContext context = RuntimeContext.builder()
                .userId(AgentRuntimeKeys.pathSafeSegment(runtimeUserKey, "anonymous"))
                .sessionId(AgentRuntimeKeys.sessionKey(sessionKey))
                .build();

        return harnessAgent.call(new UserMessage(text), context)
                .map(msg -> msg.getTextContent());
    }
}
