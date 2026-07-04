package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentStateLogEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.mapper.AiAgentStateLogMapper;
import com.zw.agent.runtime.AgentRuntimeKeys;
import com.zw.agent.service.AiAgentStateLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.common.context.UserInfo;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.state.AgentState;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.harness.agent.HarnessAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * AgentState状态引用表：记录AgentScope运行时状态在外部存储中的位置和元数据 服务实现类
 * </p>
 *
 * @author 智纬
 * @since 2026-07-04
 */
@RequiredArgsConstructor
@Service
public class AiAgentStateLogServiceImpl extends ServiceImpl<AiAgentStateLogMapper, AiAgentStateLogEntity> implements AiAgentStateLogService {

    private final AiAgentStateLogMapper agentStateLogMapper;

    @Async
    @Override
    public CompletableFuture<Boolean> saveStateLogAsync(UserInfo userInfo,
                                                        AgentConfigDTO config,
                                                        Long sessionId,
                                                        String userKey,
                                                        String sessionKey) {
        try {
            AiAgentStateLogEntity stateLogEntity = new AiAgentStateLogEntity();
            stateLogEntity.setTenantId(userInfo.getTenantId());
            stateLogEntity.setAgentId(config.getAgentId());
            stateLogEntity.setAgentConfigId(config.getAgentConfigId());
            stateLogEntity.setSessionId(sessionId);
            stateLogEntity.setUserId(userInfo.getUserId());
            stateLogEntity.setRuntimeUserKey(userKey);
            stateLogEntity.setRuntimeSessionKey(sessionKey);
            stateLogEntity.setStateBackend("REDIS");
            stateLogEntity.setStateKey(AgentRuntimeKeys.redisStateKey(userKey, sessionKey));
            stateLogEntity.setLastLoadedAt(LocalDateTime.now());
            stateLogEntity.setExpireAt(LocalDateTime.now().plusDays(30));
            this.saveOrUpdate(stateLogEntity);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            // 返回失败的 Future，让调用方可以处理异常
            return CompletableFuture.failedFuture(e);
        }
    }


    public Boolean updateState(
            HarnessAgent agent,
            RuntimeContext runtimeContext,
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId
    ) {
        AgentState agentState = runtimeContext.getAgentState();
        AgentState agentState2 = agent.getAgentState();
        if (agentState == null){
            agentState = agentState2;
        }
        String json = agentState.toJson();

        AiAgentStateLogEntity update = new AiAgentStateLogEntity();
        update.setTenantId(userInfo.getTenantId());
        update.setRuntimeUserKey(AgentRuntimeKeys.userKey(userInfo.getTenantId(), userInfo.getUserId()));
        update.setRuntimeSessionKey(AgentRuntimeKeys.sessionKey(sessionId));
        update.setStateSizeBytes((long) json.getBytes(StandardCharsets.UTF_8).length);
        update.setContextMessageCount(agentState.getContext() == null ? 0 : agentState.getContext().size());
        update.setSummaryExists((byte) (agentState.getSummary() != null ? 1 : 0));
        update.setSummaryPreview(agentState.getSummary());
        update.setLastSavedAt(LocalDateTime.now());

        if (agentState.getSummary() != null) {
            update.setLastCompactedAt(LocalDateTime.now());
        }

        return agentStateLogMapper.updateByRuntimeKey(update);
    }

}
