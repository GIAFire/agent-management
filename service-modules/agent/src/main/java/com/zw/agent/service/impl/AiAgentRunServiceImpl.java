package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentRunEntity;
import com.zw.agent.mapper.AiAgentRunMapper;
import com.zw.agent.service.AiAgentRunService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.common.context.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Agent 运行表：一次用户请求对应一次 AgentScope call 或 streamEvents 执行 服务实现类
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Service
public class AiAgentRunServiceImpl extends ServiceImpl<AiAgentRunMapper, AiAgentRunEntity> implements AiAgentRunService {
    @Autowired
    private AiAgentRunMapper agentRunMapper;

    @Override
    public AiAgentRunEntity createRunningRun(UserInfo userInfo, Long agentId, Long agentConfigId, Long sessionId, Long messageId) {
        AiAgentRunEntity agentRunEntity = new AiAgentRunEntity();
        agentRunEntity.setAgentId(agentId);
        agentRunEntity.setAgentConfigId(agentConfigId);
        agentRunEntity.setSessionId(sessionId);
        agentRunEntity.setInputMessageId(messageId);
        agentRunEntity.setStatus("RUNNING");
        agentRunEntity.setTenantId(userInfo.getTenantId());
        agentRunMapper.insert(agentRunEntity);
        return agentRunEntity;
    }

    @Override
    public void markSuccess(Long runId, Long messageId) {
        AiAgentRunEntity aiAgentRunEntity = agentRunMapper.selectById(runId);
        aiAgentRunEntity.setOutputMessageId(messageId);
        aiAgentRunEntity.setStatus("SUCCESS");
        agentRunMapper.updateById(aiAgentRunEntity);
    }

    @Override
    public void markFailed(Long runId, String agentRunFailed, String message) {
        AiAgentRunEntity aiAgentRunEntity = agentRunMapper.selectById(runId);
        aiAgentRunEntity.setStatus("FAILED");
        aiAgentRunEntity.setErrorCode(agentRunFailed);
        aiAgentRunEntity.setErrorMessage(message);
        agentRunMapper.updateById(aiAgentRunEntity);
    }
}
