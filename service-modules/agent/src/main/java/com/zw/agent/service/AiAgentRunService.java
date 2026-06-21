package com.zw.agent.service;

import com.zw.agent.entity.AiAgentRunEntity;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * Agent 运行表：一次用户请求对应一次 AgentScope call 或 streamEvents 执行 服务类
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
public interface AiAgentRunService extends IService<AiAgentRunEntity> {

    AiAgentRunEntity createRunningRun(Long tenantId, Long agentId, Long agentConfigId, Long sessionId, Long messageId);

    void markSuccess(Long runId, Long messageId);

    void markFailed(Long runId, String agentRunFailed, String message);
}
