package com.zhiran.agent.service;

import com.zhiran.agent.entity.AiAgentSessionEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhiran.common.context.UserInfo;

/**
 * <p>
 * Agent 会话表：保存用户与 Agent 的一次连续对话 服务类
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
public interface AiAgentSessionService extends IService<AiAgentSessionEntity> {

    AiAgentSessionEntity createSession(UserInfo userInfo, Long agentId, Long agentConfigId, String title);

    AiAgentSessionEntity getOwnedSession(UserInfo userInfo, Long agentId, Long sessionId);
}
