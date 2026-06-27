package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentMessageEntity;
import com.zw.agent.entity.AiAgentRunEntity;
import com.zw.agent.mapper.AiAgentMessageMapper;
import com.zw.agent.mapper.AiAgentRunMapper;
import com.zw.agent.service.AiAgentMessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.agent.service.AiAgentRunService;
import com.zw.common.context.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Autowired
    private AiAgentRunMapper agentRunMapper;

    @Override
    public AiAgentMessageEntity saveUserMessage(UserInfo userInfo, Long sessionId, String content) {
        AiAgentMessageEntity agentMessageEntity = new AiAgentMessageEntity();
        agentMessageEntity.setRole("USER");
        agentMessageEntity.setSenderName(userInfo.getUserName());
        agentMessageEntity.setTextContent(content);
        agentMessageEntity.setTenantId(userInfo.getTenantId());
        agentMessageEntity.setSessionId(sessionId);
        agentMessageEntity.setCreatedBy(userInfo.getUserId());
        agentMessageEntity.setCreatedAt(LocalDateTime.now());
        agentMessageMapper.insert(agentMessageEntity);
        return agentMessageEntity;
    }

    @Transactional
    @Override
    public void saveAssistantMessage(UserInfo userInfo, Long sessionId, Long runId, String msg,String agentName, Integer usageToken, Double usageTime) {
        AiAgentMessageEntity agentMessageEntity = new AiAgentMessageEntity();
        agentMessageEntity.setRole("ASSISTANT");
        agentMessageEntity.setSenderName(agentName);
        agentMessageEntity.setTextContent(msg);
        agentMessageEntity.setTenantId(userInfo.getTenantId());
        agentMessageEntity.setSessionId(sessionId);
        agentMessageEntity.setRunId(runId);
        agentMessageEntity.setUsageToken(usageToken);
        agentMessageEntity.setUsageTime(usageTime);
        agentMessageEntity.setCreatedBy(userInfo.getUserId());
        agentMessageEntity.setCreatedAt(LocalDateTime.now());
        agentMessageMapper.insert(agentMessageEntity);

        AiAgentRunEntity aiAgentRunEntity = agentRunMapper.selectById(runId);
        aiAgentRunEntity.setOutputMessageId(agentMessageEntity.getId());
        aiAgentRunEntity.setStatus("SUCCESS");
        agentRunMapper.updateById(aiAgentRunEntity);
    }
}
