package com.zw.agent.factory.agentFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.event.AgentRuntimeEvent;
import com.zw.agent.factory.agentFactory.entity.AgentRuntimeStream;
import com.zw.agent.factory.compactionFactory.CompactionFactory;
import com.zw.agent.factory.modelFactory.ModelFactory;
import com.zw.agent.factory.permissionFactory.PermissionFactory;
import com.zw.agent.factory.runtimeContextFactory.RuntimeContextFactory;
import com.zw.agent.factory.subAgentFactory.SubAgentFactory;
import com.zw.agent.factory.toolResultFactory.ToolResultEvictionFactory;
import com.zw.agent.runtime.AgentRuntimeKeys;
import com.zw.agent.factory.toolkitFactory.TenantToolkitFactory;
import com.zw.agent.service.AiAgentStateLogService;
import com.zw.common.constant.CacheKeyBuilder;
import com.zw.common.context.UserInfo;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.*;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.ChatModelBase;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import io.agentscope.harness.agent.memory.compaction.ToolResultEvictionConfig;
import io.agentscope.harness.agent.subagent.SubagentDeclaration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Paths;
import java.util.*;


/**
 * Agent运行时工厂类，负责创建和管理HarnessAgent实例。
 * 提供基于租户ID、Agent ID和版本号的缓存机制，确保相同配置的Agent实例复用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentRuntimeFactory {

    private final Cache<String, HarnessAgent> agentCache;
    private final TenantToolkitFactory toolkitFactory;
    private final PermissionFactory permissionFactory;
    private final CompactionFactory compactionFactory;
    private final ToolResultEvictionFactory toolResultEvictionFactory;
    private final ModelFactory modelFactory;
    private final AiAgentStateLogService agentStateLogService;
    private final CacheKeyBuilder cacheKeyBuilder;
    private final RuntimeContextFactory runtimeContextFactory;
    private final SubAgentFactory subAgentFactory;
    private final String WORKPATH = ".agentscope/workspace";

    public HarnessAgent getOrCreateAgent(AgentConfigDTO config, UserInfo userInfo, Long sessionId) {
        String agentCacheKey = cacheKeyBuilder.buildAgentKey(
                config.getAgentId(),
                userInfo.getUserId(),
                config.getAgentConfigId(),
                sessionId);

        return agentCache.get(agentCacheKey, key -> {
            Toolkit toolkit = toolkitFactory.buildToolkit(config.getAgentId());
            PermissionContextState permissionContextState = permissionFactory.buildPermissionContext(config, userInfo, toolkit);
            CompactionConfig compactionConfig = compactionFactory.buildCompaction(config);
            ToolResultEvictionConfig toolResultEvictionConfig = toolResultEvictionFactory.buildToolResultEviction(config);
            ChatModelBase chatModelBase = modelFactory.buildModel(config);
            List<SubagentDeclaration> subAgentList = subAgentFactory.buildSubAgent(config);

            HarnessAgent.Builder agentBuilder = HarnessAgent.builder()
                    .name(config.getAgentName())
                    .sysPrompt(config.getSysPrompt())
                    .model(chatModelBase)
                    .toolkit(toolkit)
                    .permissionContext(permissionContextState)
                    .maxIters(config.getMaxIters())
                    .compaction(compactionConfig)
                    .workspace(Paths.get(config.getWorkspacePath() == null ? WORKPATH + config.getTenantId() : config.getWorkspacePath()))
                    .toolResultEviction(toolResultEvictionConfig);
            if (config.getMemoryEnable() == 0){
                agentBuilder
                        .disableMemoryTools()
                        .disableMemoryHooks();
            }
            if (config.getPlanModeEnabled() == 1){
                agentBuilder
                        .enablePlanMode(true)   // 装 PlanMode 三件套
                        .planFileDirectory(Optional.ofNullable(config.getPlanFileDirectory()).orElse("plans"));
            }
            if (config.getTaskListEnabled() == 1){
                // 开启 todo_write，让结构化任务列表写入 AgentState，后续事件监听才能同步进业务表。
                agentBuilder.enableTaskList(true);
            }
            if (config.getAllowShellInPlanMode() == 1){
                agentBuilder.allowShellInPlanMode();
            }
            if (!subAgentList.isEmpty()){
                agentBuilder.subagents(subAgentList);
            }

            return agentBuilder.build();
        });
    }

    /**
     * 调用Agent执行用户消息并获取响应。
     * 该方法会先通过配置获取或创建Agent实例，然后构建运行时上下文，
     * 最后异步执行Agent调用并返回文本格式的响应内容。
     *
     * @param config    Agent运行时配置，用于获取或创建Agent实例
     * @param sessionId 会话标识键，用于跟踪会话状态
     * @param text      用户输入的文本消息内容
     * @return Mono封装的字符串响应，包含Agent处理后的文本内容
     */

    public AgentRuntimeStream callStreamEvents(
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId,
            String text
    ) {
        String userKey = AgentRuntimeKeys.userKey(userInfo.getUserId());
        String sessionKey = AgentRuntimeKeys.sessionKey(sessionId);
        HarnessAgent harnessAgent = getOrCreateAgent(config, userInfo, sessionId);

        RuntimeContext runtimeContext = runtimeContextFactory.buildRuntimeContext(config, userInfo, sessionId, runId, userKey, sessionKey);

        UserMessage userMessage = new UserMessage(text);

        agentStateLogService.saveStateLog(userInfo, config, sessionId, userKey, sessionKey);
        Flux<AgentRuntimeEvent> runtimeEvents = harnessAgent
                .streamEvents(userMessage, runtimeContext)
                .map(this::toRuntimeEvent);

        return new AgentRuntimeStream(
                harnessAgent,
                runtimeContext,
                runtimeEvents,
                userKey,
                sessionKey
        );
    }

    public AgentRuntimeStream continueWithConfirmResults(
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId,
            List<ConfirmResult> confirmResults
    ) {
        HarnessAgent harnessAgent = getOrCreateAgent(config, userInfo, sessionId);
        String userKey = AgentRuntimeKeys.userKey(userInfo.getUserId());
        String sessionKey = AgentRuntimeKeys.sessionKey(sessionId);
        RuntimeContext runtimeContext = runtimeContextFactory.buildRuntimeContext(config, userInfo, sessionId, runId, userKey, sessionKey);

        Msg confirmMsg = Msg.builder()
                .name("user")
                .role(MsgRole.USER)
                .metadata(Map.of(Msg.METADATA_CONFIRM_RESULTS, confirmResults == null ? List.of() : confirmResults))
                .textContent("")
                .build();

        Flux<AgentRuntimeEvent> runtimeEvent = harnessAgent.streamEvents(List.of(confirmMsg), runtimeContext)
                .map(this::toRuntimeEvent);

        return new AgentRuntimeStream(harnessAgent, runtimeContext, runtimeEvent, userKey, sessionKey);
    }

    public AgentRuntimeStream continueWithExternalExecutionResults(
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId,
            List<ToolResultBlock> toolResults
    ) {
        HarnessAgent harnessAgent = getOrCreateAgent(config, userInfo, sessionId);
        String userKey = AgentRuntimeKeys.userKey(userInfo.getUserId());
        String sessionKey = AgentRuntimeKeys.sessionKey(sessionId);
        RuntimeContext runtimeContext = runtimeContextFactory.buildRuntimeContext(config, userInfo, sessionId, runId, userKey, sessionKey);

        List<ContentBlock> content = new ArrayList<>(toolResults == null ? List.of() : toolResults);
        Msg toolMsg = Msg.builder()
                .name("external")
                .role(MsgRole.TOOL)
                .content(content)
                .build();

        Flux<AgentRuntimeEvent> runtimeEvent = harnessAgent.streamEvents(List.of(toolMsg), runtimeContext)
                .map(this::toRuntimeEvent);

        return new AgentRuntimeStream(harnessAgent, runtimeContext, runtimeEvent, userKey, sessionKey);
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
}
