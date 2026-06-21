package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentMessageEntity;
import com.zw.agent.mapper.AiAgentMessageMapper;
import com.zw.agent.service.AiAgentMessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.common.context.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <p>
 * Agent 消息表：保存用户输入、Agent 回复、工具消息等完整上下文 服务实现类
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Service
public class AiAgentMessageServiceImpl extends ServiceImpl<AiAgentMessageMapper, AiAgentMessageEntity> implements AiAgentMessageService {

    @Autowired
    private AiAgentMessageMapper agentMessageMapper;
    @Override
    public AiAgentMessageEntity saveUserMessage(Long tenantId, Long sessionId, String content) {
        AiAgentMessageEntity agentMessageEntity = new AiAgentMessageEntity();
        agentMessageEntity.setRole("USER");
        agentMessageEntity.setSenderName(UserContext.get().getUserName());
        agentMessageEntity.setTextContent(content);
        agentMessageEntity.setTenantId(tenantId);
        agentMessageEntity.setSessionId(sessionId);
        agentMessageEntity.setCreatedBy(UserContext.get().getUserId());
        agentMessageEntity.setCreatedAt(LocalDateTime.now());
        agentMessageMapper.insert(agentMessageEntity);
        return agentMessageEntity;
    }

    @Override
    public AiAgentMessageEntity saveAssistantMessage(Long tenantId, Long sessionId, Long runId, String msg) {
        AiAgentMessageEntity agentMessageEntity = new AiAgentMessageEntity();
        agentMessageEntity.setRole("ASSISTANT");
        agentMessageEntity.setSenderName("AI");
        agentMessageEntity.setTextContent(msg);
        agentMessageEntity.setTenantId(tenantId);
        agentMessageEntity.setSessionId(sessionId);
        agentMessageEntity.setRunId(runId);
        agentMessageEntity.setCreatedBy(UserContext.get().getUserId());
        agentMessageMapper.insert(agentMessageEntity);
        return agentMessageEntity;
    }
}
