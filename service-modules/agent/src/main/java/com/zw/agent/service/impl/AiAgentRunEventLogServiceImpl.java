package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentRunEventLogEntity;
import com.zw.agent.mapper.AiAgentRunEventLogMapper;
import com.zw.agent.service.AiAgentRunEventLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
public class AiAgentRunEventLogServiceImpl extends ServiceImpl<AiAgentRunEventLogMapper, AiAgentRunEventLogEntity> implements AiAgentRunEventLogService {

    @Autowired
    private AiAgentRunEventLogMapper agentRunEventMapper;
    @Override
    public int saveEvent(Long userId, Long tenantId, Long runId, Long sessionId,String runtimeEvent) {
        AiAgentRunEventLogEntity agentRunEventEntity = new AiAgentRunEventLogEntity();
        agentRunEventEntity.setRunId(runId);
        agentRunEventEntity.setEventType(runtimeEvent);
        agentRunEventEntity.setTenantId(tenantId);
        agentRunEventEntity.setSessionId(sessionId);
        agentRunEventEntity.setCreatedAt(LocalDateTime.now());
        agentRunEventEntity.setCreatedBy(userId);
        return agentRunEventMapper.insert(agentRunEventEntity);
    }
}
