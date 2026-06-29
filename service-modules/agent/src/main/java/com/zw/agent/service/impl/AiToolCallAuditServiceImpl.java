package com.zw.agent.service.impl;

import com.zw.agent.entity.AiToolCallAuditEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.event.AgentRuntimeEvent;
import com.zw.agent.mapper.AiToolCallAuditMapper;
import com.zw.agent.service.AiToolCallAuditService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.common.context.UserInfo;
import io.agentscope.core.event.AgentEventType;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.event.ToolResultEndEvent;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * <p>
 * 工具调用审计表：记录Agent每一次工具调用的权限结果、参数、结果和耗时 服务实现类
 * </p>
 *
 * @author 智纬
 * @since 2026-06-28
 */
@Service
public class AiToolCallAuditServiceImpl extends ServiceImpl<AiToolCallAuditMapper, AiToolCallAuditEntity> implements AiToolCallAuditService {

    public void handleToolCallAuditEvent(
            String eventType,
            AgentRuntimeEvent runtimeEvent,
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId,
            Map<String, AiToolCallAuditEntity> toolAuditMap
    ) {
        if (AgentEventType.TOOL_CALL_START.getValue().equals(eventType)) {
            ToolCallStartEvent rawEvent = (ToolCallStartEvent) runtimeEvent.getRawEvent();

            AiToolCallAuditEntity auditEntity = new AiToolCallAuditEntity();
            auditEntity.setStartedAt(LocalDateTime.now());
            auditEntity.setToolName(rawEvent.getToolCallName());
            auditEntity.setToolCallId(rawEvent.getToolCallId());
            auditEntity.setReplyId(rawEvent.getReplyId());
            auditEntity.setTenantId(userInfo.getTenantId());
            auditEntity.setSessionId(sessionId);
            auditEntity.setRunId(runId);
            auditEntity.setAgentId(config.getAgentId());
            auditEntity.setAgentConfigId(config.getAgentConfigId());
            auditEntity.setUserId(userInfo.getUserId());

            toolAuditMap.put(rawEvent.getToolCallId(), auditEntity);
            return;
        }

        if (AgentEventType.TOOL_RESULT_END.getValue().equals(eventType)) {
            ToolResultEndEvent rawEvent = (ToolResultEndEvent) runtimeEvent.getRawEvent();

            AiToolCallAuditEntity auditEntity = toolAuditMap.get(rawEvent.getToolCallId());
            if (auditEntity != null) {
                auditEntity.setEndedAt(LocalDateTime.now());
                auditEntity.setSuccessStatus(rawEvent.getState().getValue());
            }
        }
    }

}
