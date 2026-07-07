package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentMessageLogEntity;
import com.zw.agent.entity.AiAgentRunLogEntity;
import com.zw.agent.mapper.AiAgentMessageLogMapper;
import com.zw.agent.mapper.AiAgentRunLogMapper;
import com.zw.agent.service.AiAgentMessageLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
public class AiAgentMessageLogLogServiceImpl extends ServiceImpl<AiAgentMessageLogMapper, AiAgentMessageLogEntity> implements AiAgentMessageLogService {

    @Autowired
    private AiAgentMessageLogMapper agentMessageMapper;
    @Autowired
    private AiAgentRunLogMapper agentRunMapper;

    @Override
    public AiAgentMessageLogEntity saveUserMessage(UserInfo userInfo, Long sessionId, String content) {
        AiAgentMessageLogEntity agentMessageEntity = new AiAgentMessageLogEntity();
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
        AiAgentMessageLogEntity agentMessageEntity = new AiAgentMessageLogEntity();
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

        AiAgentRunLogEntity aiAgentRunLogEntity = agentRunMapper.selectById(runId);
        aiAgentRunLogEntity.setOutputMessageId(agentMessageEntity.getId());
        aiAgentRunLogEntity.setStatus("SUCCESS");
        agentRunMapper.updateById(aiAgentRunLogEntity);
    }
}
