package com.zw.agent.runtime;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.zw.agent.entity.AiToolRolePermissionEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.event.AgentRuntimeEvent;
import com.zw.agent.service.AiToolRolePermissionService;
import com.zw.agent.tools.toolkitFactory.TenantToolkitFactory;
import com.zw.common.RedisService;
import com.zw.common.context.UserContext;
import com.zw.common.context.UserInfo;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.*;
import io.agentscope.core.formatter.openai.OpenAIChatFormatter;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionMode;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.HarnessAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;


/**
 * Agent运行时工厂类，负责创建和管理HarnessAgent实例。
 * 提供基于租户ID、Agent ID和版本号的缓存机制，确保相同配置的Agent实例复用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentRuntimeFactory {

    private final Cache<String, HarnessAgent> agentCache;

    private final Cache<String, Toolkit> toolkitCache;

    private final Cache<String, PermissionContextState> permissionContextStateCache;

    private final TenantToolkitFactory toolkitFactory;

    private final RedisService redisService;
    private final AiToolRolePermissionService toolRolePermissionService;

    public HarnessAgent getOrCreateAgent(AgentConfigDTO config) {
        UserInfo userInfo = UserContext.get();
        String agentCacheKey = "agent:" + userInfo.getUserId() + ":" + config.getAgentId() + ":" + config.getAgentConfigId();
        String toolkitCacheKey = "toolkit:" + config.getTenantId();
        String toolPermissionCacheKey = "toolPermission:" + config.getTenantId() + ":" + userInfo.getRoleCode();

        // 构造工具
        Toolkit toolkit = toolkitCache.get(toolkitCacheKey, key -> {
            Toolkit toolkitBuild = toolkitFactory.buildToolkit(config.getTenantId());

            redisService.setIfAbsent(toolkitCacheKey, "1", 30L, TimeUnit.DAYS);
            return toolkitBuild;
        });

        // 构造权限
        PermissionContextState permissionContextState = permissionContextStateCache.get(toolPermissionCacheKey, key -> {
            PermissionContextState build = buildPermissionContext(config, userInfo, toolkit);
            redisService.setIfAbsent(toolPermissionCacheKey, "1", 30L, TimeUnit.DAYS);
            return build;
        });


        // 构造模型配置
        return agentCache.get(agentCacheKey, key -> {

            OpenAIChatModel model = OpenAIChatModel.builder()
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelName())
                    .baseUrl(config.getBaseUrl())
                    .stream(config.getIsStream())
                    .formatter(new OpenAIChatFormatter())
                    .build();

            // 构造Agent实例和Agent配置
            HarnessAgent harnessAgent = HarnessAgent.builder()
                    .name(config.getAgentName())
                    .sysPrompt(config.getSysPrompt())
                    .model(model)
                    .toolkit(toolkit)
                    .permissionContext(permissionContextState)
                    .maxIters(100)
                    .build();

            redisService.setIfAbsent(key, "1", 30L, TimeUnit.DAYS);

            return harnessAgent;
        });
    }

    private PermissionContextState buildPermissionContext(
            AgentConfigDTO config,
            UserInfo userInfo,
            Toolkit toolkit
    ) {
        String roleCode = String.valueOf(userInfo.getRoleCode());

        PermissionContextState.Builder builder = PermissionContextState.builder()
                .mode(PermissionMode.valueOf(config.getPermissionMode()));

        Set<String> toolNames = toolkit.getToolNames();

        for (String toolName : toolNames) {
            AiToolRolePermissionEntity toolRolePermission = toolRolePermissionService.getOne(new LambdaQueryWrapper<AiToolRolePermissionEntity>()
                    .eq(AiToolRolePermissionEntity::getToolName, toolName)
                    .eq(AiToolRolePermissionEntity::getRoleCode, roleCode)
                    .eq(AiToolRolePermissionEntity::getTenantId, userInfo.getTenantId())
                    .eq(AiToolRolePermissionEntity::getStatus, (byte) 1));

            if (toolRolePermission == null) {
                PermissionRule rule =
                        new PermissionRule(
                                toolName,
                                null,
                                PermissionBehavior.DENY,
                                "userSettings"
                        );
                builder.addDenyRule(toolName, rule);
            } else {
                String behavior = toolRolePermission.getBehavior() == null
                        ? ""
                        : toolRolePermission.getBehavior().trim().toUpperCase();
                PermissionRule rule =
                        new PermissionRule(
                                toolName,
                                toolRolePermission.getRuleContent(),
                                PermissionBehavior.fromString(behavior),
                                "userSettings"
                        );

                switch (behavior) {
                    case "ALLOW" -> builder.addAllowRule(toolName, rule);
                    case "DENY" -> builder.addDenyRule(toolName, rule);
                    case "ASK" -> builder.addAskRule(toolName, rule);
                    default -> {
                        // PASSTHROUGH 一般不建议作为显式配置规则使用
                    }
                }
            }
        }

        return builder.build();
    }

    /**
     * 调用Agent执行用户消息并获取响应。
     * 该方法会先通过配置获取或创建Agent实例，然后构建运行时上下文，
     * 最后异步执行Agent调用并返回文本格式的响应内容。
     *
     * @param config       Agent运行时配置，用于获取或创建Agent实例
     * @param tenantUserId 运行时用户标识键，用于构建运行时上下文
     * @param sessionId    会话标识键，用于跟踪会话状态
     * @param text         用户输入的文本消息内容
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
        // 获取用户消息
        UserMessage userMessage = new UserMessage(text);

        // 获取Agent事件流
        Flux<AgentEvent> agentEventFlux = harnessAgent.streamEvents(userMessage, context);
        return agentEventFlux
                .map(this::toRuntimeEvent);
    }

    public Flux<AgentRuntimeEvent> continueWithConfirmResults(
            AgentConfigDTO config,
            String tenantUserId,
            Long sessionId,
            List<ConfirmResult> confirmResults
    ) {
        HarnessAgent harnessAgent = getOrCreateAgent(config);
        RuntimeContext context = buildRuntimeContext(tenantUserId, sessionId);
        Msg confirmMsg = Msg.builder()
                .name("user")
                .role(MsgRole.USER)
                .metadata(Map.of(Msg.METADATA_CONFIRM_RESULTS, confirmResults == null ? List.of() : confirmResults))
                .textContent("")
                .build();

        return harnessAgent.streamEvents(List.of(confirmMsg), context)
                .map(this::toRuntimeEvent);
    }

    public Flux<AgentRuntimeEvent> continueWithExternalExecutionResults(
            AgentConfigDTO config,
            String tenantUserId,
            Long sessionId,
            List<ToolResultBlock> toolResults
    ) {
        HarnessAgent harnessAgent = getOrCreateAgent(config);
        RuntimeContext context = buildRuntimeContext(tenantUserId, sessionId);
        List<ContentBlock> content = new ArrayList<>(toolResults == null ? List.of() : toolResults);
        Msg toolMsg = Msg.builder()
                .name("external")
                .role(MsgRole.TOOL)
                .content(content)
                .build();

        return harnessAgent.streamEvents(List.of(toolMsg), context)
                .map(this::toRuntimeEvent);
    }

    private RuntimeContext buildRuntimeContext(String tenantUserId, Long sessionId) {
        return RuntimeContext.builder()
                .userId(tenantUserId)
                .sessionId(AgentRuntimeKeys.sessionKey(String.valueOf(sessionId)))
                .build();
    }

    private AgentRuntimeEvent toRuntimeEvent(AgentEvent event) {
        if (event instanceof AgentStartEvent startEvent) {
            return new AgentRuntimeEvent(
                    startEvent.getReplyId(),
                    AgentEventType.AGENT_START.getValue(),
                    null,
                    startEvent
            );
        } else if (event instanceof AgentEndEvent endEvent) {
            return new AgentRuntimeEvent(
                    endEvent.getReplyId(),
                    AgentEventType.AGENT_END.getValue(),
                    null,
                    endEvent
            );
        } else if (event instanceof ExceedMaxItersEvent exceedMaxItersEvent) {
            return new AgentRuntimeEvent(
                    exceedMaxItersEvent.getReplyId(),
                    AgentEventType.EXCEED_MAX_ITERS.getValue(),
                    null,
                    exceedMaxItersEvent
            );
        } else if (event instanceof RequestStopEvent requestStopEvent) {
            return new AgentRuntimeEvent(
                    null,
                    AgentEventType.REQUEST_STOP.getValue(),
                    null,
                    requestStopEvent
            );

            // 文本流事件
        } else if (event instanceof TextBlockStartEvent textBlockStartEvent) {
            return new AgentRuntimeEvent(
                    textBlockStartEvent.getReplyId(),
                    AgentEventType.TEXT_BLOCK_START.getValue(),
                    null,
                    textBlockStartEvent
            );
        } else if (event instanceof TextBlockDeltaEvent textBlockDeltaEvent) {
            return new AgentRuntimeEvent(
                    textBlockDeltaEvent.getReplyId(),
                    AgentEventType.TEXT_BLOCK_DELTA.getValue(),
                    textBlockDeltaEvent.getDelta(),
                    textBlockDeltaEvent
            );
        } else if (event instanceof TextBlockEndEvent textBlockEndEvent) {
            return new AgentRuntimeEvent(
                    textBlockEndEvent.getReplyId(),
                    AgentEventType.TEXT_BLOCK_END.getValue(),
                    null,
                    textBlockEndEvent
            );
            // 思考流事件
        } else if (event instanceof ThinkingBlockStartEvent thinkingBlockStartEvent) {
            return new AgentRuntimeEvent(
                    thinkingBlockStartEvent.getReplyId(),
                    AgentEventType.THINKING_BLOCK_START.getValue(),
                    null,
                    thinkingBlockStartEvent
            );
        } else if (event instanceof ThinkingBlockDeltaEvent thinkingBlockDeltaEvent) {
            return new AgentRuntimeEvent(
                    thinkingBlockDeltaEvent.getReplyId(),
                    AgentEventType.THINKING_BLOCK_DELTA.getValue(),
                    thinkingBlockDeltaEvent.getDelta(),
                    thinkingBlockDeltaEvent
            );
        } else if (event instanceof ThinkingBlockEndEvent thinkingBlockEndEvent) {
            return new AgentRuntimeEvent(
                    thinkingBlockEndEvent.getReplyId(),
                    AgentEventType.THINKING_BLOCK_END.getValue(),
                    null,
                    thinkingBlockEndEvent
            );
            // 工具调用流事件
        } else if (event instanceof ToolCallStartEvent toolCallStartEvent) {
            return new AgentRuntimeEvent(
                    toolCallStartEvent.getReplyId(),
                    AgentEventType.TOOL_CALL_START.getValue(),
                    toolCallStartEvent.getToolCallName(),
                    toolCallStartEvent
            );
        } else if (event instanceof ToolCallDeltaEvent toolCallDeltaEvent) {
            return new AgentRuntimeEvent(
                    toolCallDeltaEvent.getReplyId(),
                    AgentEventType.TOOL_CALL_DELTA.getValue(),
                    toolCallDeltaEvent.getDelta(),
                    toolCallDeltaEvent
            );
        } else if (event instanceof ToolCallEndEvent toolCallEndEvent) {
            return new AgentRuntimeEvent(
                    toolCallEndEvent.getReplyId(),
                    AgentEventType.TOOL_CALL_END.getValue(),
                    null,
                    toolCallEndEvent
            );
            // 工具结果流事件
        } else if (event instanceof ToolResultStartEvent toolResultStartEvent) {
            return new AgentRuntimeEvent(
                    toolResultStartEvent.getReplyId(),
                    AgentEventType.TOOL_RESULT_START.getValue(),
                    null,
                    toolResultStartEvent
            );
        } else if (event instanceof ToolResultTextDeltaEvent toolResultTextDeltaEvent) {
            return new AgentRuntimeEvent(
                    toolResultTextDeltaEvent.getReplyId(),
                    AgentEventType.TOOL_RESULT_TEXT_DELTA.getValue(),
                    toolResultTextDeltaEvent.getDelta(),
                    toolResultTextDeltaEvent
            );
        } else if (event instanceof ToolResultDataDeltaEvent toolResultDataDeltaEvent) {
            return new AgentRuntimeEvent(
                    toolResultDataDeltaEvent.getReplyId(),
                    AgentEventType.TOOL_RESULT_DATA_DELTA.getValue(),
                    null,
                    toolResultDataDeltaEvent
            );
        } else if (event instanceof ToolResultEndEvent toolResultEndEvent) {
            return new AgentRuntimeEvent(
                    toolResultEndEvent.getReplyId(),
                    AgentEventType.TOOL_RESULT_END.getValue(),
                    null,
                    toolResultEndEvent
            );
            // 模型调用事件
        } else if (event instanceof ModelCallStartEvent modelCallStartEvent) {
            return new AgentRuntimeEvent(
                    modelCallStartEvent.getReplyId(),
                    AgentEventType.MODEL_CALL_START.getValue(),
                    null,
                    modelCallStartEvent
            );
        } else if (event instanceof ModelCallEndEvent modelCallEndEvent) {
            return new AgentRuntimeEvent(
                    modelCallEndEvent.getReplyId(),
                    AgentEventType.MODEL_CALL_END.getValue(),
                    null,
                    modelCallEndEvent
            );
            // 人工介入事件
        } else if (event instanceof RequireUserConfirmEvent requireUserConfirmEvent) {
            return new AgentRuntimeEvent(
                    requireUserConfirmEvent.getReplyId(),
                    AgentEventType.REQUIRE_USER_CONFIRM.getValue(),
                    null,
                    requireUserConfirmEvent
            );
        } else if (event instanceof RequireExternalExecutionEvent requireExternalExecutionEvent) {
            return new AgentRuntimeEvent(
                    requireExternalExecutionEvent.getReplyId(),
                    AgentEventType.REQUIRE_EXTERNAL_EXECUTION.getValue(),
                    null,
                    requireExternalExecutionEvent
            );
        } else if (event instanceof UserConfirmResultEvent userConfirmResultEvent) {
            return new AgentRuntimeEvent(
                    userConfirmResultEvent.getReplyId(),
                    AgentEventType.USER_CONFIRM_RESULT.getValue(),
                    null,
                    userConfirmResultEvent
            );
        } else if (event instanceof ExternalExecutionResultEvent externalExecutionResultEvent) {
            return new AgentRuntimeEvent(
                    externalExecutionResultEvent.getReplyId(),
                    AgentEventType.EXTERNAL_EXECUTION_RESULT.getValue(),
                    null,
                    externalExecutionResultEvent
            );
            // 子Agent事件
        } else if (event instanceof SubagentExposedEvent subagentExposedEvent) {
            return new AgentRuntimeEvent(
                    null,
                    AgentEventType.SUBAGENT_EXPOSED.getValue(),
                    null,
                    subagentExposedEvent
            );
        }

        return new AgentRuntimeEvent(
                event.getId(),
                event.getType().name(),
                null,
                event
        );
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
        String AgentCacheKey = config.getTenantId() + ":" + config.getAgentId() + ":" + config.getAgentConfigId();
        HarnessAgent harnessAgent = agentCache.get(AgentCacheKey, key ->
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

    public void evictAgent(Long tenantId, Long agentId, Long agentConfigId) {
        String AgentCacheKey = tenantId + ":" + agentId + ":" + agentConfigId;
        agentCache.invalidate(AgentCacheKey);
    }
}
