package com.zw.agent.service;

import com.zw.agent.entity.AiAgentSessionEntity;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * Agent 会话表：保存用户与 Agent 的一次连续对话 服务类
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
public interface AiAgentSessionService extends IService<AiAgentSessionEntity> {

    AiAgentSessionEntity getOrCreateSession(Long tenantId, Long userId, Long agentId, Long agentConfigId, String sessionId);
}
