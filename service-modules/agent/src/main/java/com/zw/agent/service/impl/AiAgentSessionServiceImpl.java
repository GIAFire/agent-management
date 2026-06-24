package com.zw.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zw.agent.entity.AiAgentSessionEntity;
import com.zw.agent.mapper.AiAgentSessionMapper;
import com.zw.agent.service.AiAgentSessionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.common.context.UserInfo;
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
    public AiAgentSessionEntity getOrCreateSession(UserInfo userInfo, Long agentId, Long agentConfigId, Long sessionId) {
        AiAgentSessionEntity sessionEntity = agentSessionMapper.selectOne(new LambdaQueryWrapper<AiAgentSessionEntity>()
                .eq(AiAgentSessionEntity::getTenantId, userInfo.getTenantId())
                .eq(AiAgentSessionEntity::getUserId, userInfo.getUserId())
                .eq(AiAgentSessionEntity::getId, sessionId));
//                .eq(AiAgentSessionEntity::getAgentId, agentId)
//                .eq(AiAgentSessionEntity::getAgentConfigId, agentConfigId)

        if (sessionEntity != null) {
            return sessionEntity;
        }
        sessionEntity = new AiAgentSessionEntity();
        sessionEntity.setTenantId(userInfo.getTenantId());
        sessionEntity.setUserId(userInfo.getUserId());
        sessionEntity.setAgentId(agentId);
        sessionEntity.setAgentConfigId(agentConfigId);
        sessionEntity.setTitle("New Session");
        sessionEntity.setLastMessageAt(LocalDateTime.now());
        agentSessionMapper.insert(sessionEntity);
        return sessionEntity;
    }
}
