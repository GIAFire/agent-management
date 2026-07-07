package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentRunLogEntity;
import com.zw.agent.mapper.AiAgentRunLogMapper;
import com.zw.agent.service.AiAgentRunLogService;
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
public class AiAgentRunLogServiceImpl extends ServiceImpl<AiAgentRunLogMapper, AiAgentRunLogEntity> implements AiAgentRunLogService {
    @Autowired
    private AiAgentRunLogMapper agentRunMapper;

    @Override
    public AiAgentRunLogEntity createRunningRun(UserInfo userInfo, Long agentId, Long agentConfigId, Long sessionId, Long messageId) {
        AiAgentRunLogEntity agentRunEntity = new AiAgentRunLogEntity();
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
        AiAgentRunLogEntity aiAgentRunLogEntity = agentRunMapper.selectById(runId);
        aiAgentRunLogEntity.setOutputMessageId(messageId);
        aiAgentRunLogEntity.setStatus("SUCCESS");
        agentRunMapper.updateById(aiAgentRunLogEntity);
    }

    @Override
    public void markWaiting(Long runId, String status) {
        AiAgentRunLogEntity aiAgentRunLogEntity = agentRunMapper.selectById(runId);
        aiAgentRunLogEntity.setStatus(status);
        agentRunMapper.updateById(aiAgentRunLogEntity);
    }

    @Override
    public void markFailed(Long runId, String agentRunFailed, String message) {
        AiAgentRunLogEntity aiAgentRunLogEntity = agentRunMapper.selectById(runId);
        aiAgentRunLogEntity.setStatus("FAILED");
        aiAgentRunLogEntity.setErrorCode(agentRunFailed);
        aiAgentRunLogEntity.setErrorMessage(message);
        agentRunMapper.updateById(aiAgentRunLogEntity);
    }
}
