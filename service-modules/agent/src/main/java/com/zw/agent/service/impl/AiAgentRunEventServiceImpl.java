package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentRunEventEntity;
import com.zw.agent.event.AgentRuntimeEvent;
import com.zw.agent.mapper.AiAgentRunEventMapper;
import com.zw.agent.service.AiAgentRunEventService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.common.context.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <p>
 * Agent 运行事件表：保存流式 token、工具调用、权限请求等事件 服务实现类
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Service
public class AiAgentRunEventServiceImpl extends ServiceImpl<AiAgentRunEventMapper, AiAgentRunEventEntity> implements AiAgentRunEventService {

    @Autowired
    private AiAgentRunEventMapper agentRunEventMapper;
    @Override
    public int saveEvent(Long tenantId, Long runId, Long sessionId, int eventSeq, AgentRuntimeEvent runtimeEvent) {
        AiAgentRunEventEntity agentRunEventEntity = new AiAgentRunEventEntity();
        agentRunEventEntity.setRunId(runId);
        agentRunEventEntity.setSeq(eventSeq);
        agentRunEventEntity.setEventType(runtimeEvent.eventType());
        agentRunEventEntity.setTenantId(tenantId);
        agentRunEventEntity.setSessionId(sessionId);
        agentRunEventEntity.setCreatedAt(LocalDateTime.now());
        agentRunEventEntity.setCreatedBy(UserContext.get().getUserId());
        return agentRunEventMapper.insert(agentRunEventEntity);
    }
}
