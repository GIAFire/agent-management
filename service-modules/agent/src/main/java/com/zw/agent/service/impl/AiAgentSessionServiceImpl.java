package com.zw.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zw.agent.entity.AiAgentSessionEntity;
import com.zw.agent.mapper.AiAgentSessionMapper;
import com.zw.agent.service.AiAgentSessionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

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
    public AiAgentSessionEntity getOrCreateSession(Long tenantId, Long userId, Long agentId, Long agentConfigId, String sessionId) {
        AiAgentSessionEntity sessionEntity = agentSessionMapper.selectOne(new LambdaQueryWrapper<AiAgentSessionEntity>()
                .eq(AiAgentSessionEntity::getTenantId, tenantId)
                .eq(AiAgentSessionEntity::getUserId, userId)
                .eq(AiAgentSessionEntity::getAgentId, agentId)
                .eq(AiAgentSessionEntity::getAgentConfigId, agentConfigId)
                .eq(AiAgentSessionEntity::getSessionId, sessionId));

        if (sessionEntity != null) {
            return sessionEntity;
        }
        sessionEntity = new AiAgentSessionEntity();
        sessionEntity.setTenantId(tenantId);
        sessionEntity.setUserId(userId);
        sessionEntity.setAgentId(agentId);
        sessionEntity.setAgentConfigId(agentConfigId);
        sessionEntity.setSessionId(sessionId);
        sessionEntity.setTitle("New Session");
        sessionEntity.setLastMessageAt(LocalDateTime.now());
        agentSessionMapper.insert(sessionEntity);
        return sessionEntity;
    }
}
