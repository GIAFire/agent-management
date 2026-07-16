package com.zw.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zw.agent.entity.AiAgentSessionEntity;
import com.zw.agent.mapper.AiAgentSessionMapper;
import com.zw.agent.runtime.AgentRuntimeKeys;
import com.zw.agent.service.AiAgentSessionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.common.context.UserInfo;
import com.zw.common.support.EntityDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <p>
 * Agent 会话表：保存用户与 Agent 的一次连续对话 服务实现类
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Service
public class AiAgentSessionServiceImpl extends ServiceImpl<AiAgentSessionMapper, AiAgentSessionEntity> implements AiAgentSessionService {

    @Autowired
    private AiAgentSessionMapper agentSessionMapper;

    @Override
    public AiAgentSessionEntity createSession(UserInfo userInfo, Long agentId, Long agentConfigId, String title) {
        AiAgentSessionEntity sessionEntity = buildSession(userInfo, agentId, agentConfigId, title);
        agentSessionMapper.insert(EntityDefaults.create(sessionEntity));
        return sessionEntity;
    }

    @Override
    public AiAgentSessionEntity getOwnedSession(UserInfo userInfo, Long agentId, Long sessionId) {
        if (userInfo == null) {
            throw new IllegalArgumentException("User is not authenticated");
        }
        if (agentId == null) {
            throw new IllegalArgumentException("agentId must not be null");
        }
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId must not be null");
        }
        return agentSessionMapper.selectOne(new LambdaQueryWrapper<AiAgentSessionEntity>()
                .eq(AiAgentSessionEntity::getTenantId, userInfo.getTenantId())
                .eq(AiAgentSessionEntity::getUserId, userInfo.getUserId())
                .eq(AiAgentSessionEntity::getAgentId, agentId)
                .eq(AiAgentSessionEntity::getId, sessionId));
    }

    private AiAgentSessionEntity buildSession(UserInfo userInfo, Long agentId, Long agentConfigId, String title) {
        if (userInfo == null) {
            throw new IllegalArgumentException("User is not authenticated");
        }
        if (agentId == null) {
            throw new IllegalArgumentException("agentId must not be null");
        }
        String normalizedTitle = normalizeTitle(title);
        AiAgentSessionEntity sessionEntity = new AiAgentSessionEntity();
        sessionEntity.setTenantId(userInfo.getTenantId());
        sessionEntity.setUserId(userInfo.getUserId());
        sessionEntity.setAgentId(agentId);
        sessionEntity.setAgentConfigId(agentConfigId);
        sessionEntity.setRuntimeUserKey(AgentRuntimeKeys.userKey(userInfo.getTenantId(), userInfo.getUserId()));
        sessionEntity.setTitle(normalizedTitle);
        sessionEntity.setStatus(1);
        sessionEntity.setLastMessageAt(LocalDateTime.now());
        return sessionEntity;
    }

    private String normalizeTitle(String title) {
        String value = title == null || title.isBlank() ? "New chat" : title.trim();
        return value.length() > 15 ? value.substring(0, 15) + "..." : value;
    }
}
