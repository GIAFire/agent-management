package com.zhiran.agent.service;

import com.zhiran.agent.entity.AiAgentRunLogEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhiran.common.context.UserInfo;

/**
 * <p>
 * Agent 运行表：一次用户请求对应一次 AgentScope call 或 streamEvents 执行 服务类
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
public interface AiAgentRunLogService extends IService<AiAgentRunLogEntity> {

    AiAgentRunLogEntity createRunningRun(UserInfo userInfo, Long agentId, Long agentConfigId, Long sessionId, Long messageId);

    void markSuccess(Long runId, Long messageId);

    void markWaiting(Long runId, String status);

    void markFailed(Long runId, String agentRunFailed, String message);

    void markCancelled(Long runId);
}
