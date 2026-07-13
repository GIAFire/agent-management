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
    private final AiAgentStateOpLogServiceImpl agentStateOpLogService;

    @Async
    @Override
    public CompletableFuture<Boolean> saveStateLog(UserInfo userInfo,
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
            return CompletableFuture.failedFuture(e);
        }
    }


    public Boolean updateState(
            HarnessAgent agent,
            RuntimeContext runtimeContext,
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId
    ) {
        AiAgentStateLogEntity updateBefore = agentStateLogMapper.selectByRuntimeKey(
                AgentRuntimeKeys.userKey(userInfo.getTenantId(), userInfo.getUserId()),
                AgentRuntimeKeys.sessionKey(sessionId));

        AgentState agentState = agent.getDelegate().getAgentState(runtimeContext);

        if (agentState == null) {
            agentStateOpLogService.recordSaveFailed(
                    userInfo,
                    sessionId,
                    runId,
                    "RuntimeContext AgentState is null"
            );
            return false;
        }
        String json = agentState.toJson();
        AiAgentStateLogEntity updateAfter = new AiAgentStateLogEntity();
        updateAfter.setTenantId(userInfo.getTenantId());
        updateAfter.setRuntimeUserKey(AgentRuntimeKeys.userKey(userInfo.getTenantId(), userInfo.getUserId()));
        updateAfter.setRuntimeSessionKey(AgentRuntimeKeys.sessionKey(sessionId));
        updateAfter.setStateSizeBytes((long) json.getBytes(StandardCharsets.UTF_8).length);
        updateAfter.setContextMessageCount(agentState.getContext().isEmpty() ? 0 : agentState.getContext().size());
        updateAfter.setSummaryExists(agentState.getSummary().isEmpty() ? 0 : 1);
        updateAfter.setSummaryPreview(agentState.getSummary());
        updateAfter.setLastSavedAt(LocalDateTime.now());

        if (!agentState.getSummary().isEmpty()) {
            updateAfter.setLastCompactedAt(LocalDateTime.now());
        }

        Boolean isUpdateByRuntime  = agentStateLogMapper.updateByRuntimeKey(updateAfter);

        agentStateOpLogService.recordSave(
                userInfo,
                sessionId,
                runId,
                updateBefore,
                updateAfter,
                isUpdateByRuntime,
                null
        );

        if (isUpdateByRuntime && isCompacted(updateBefore, updateAfter)) {
            agentStateOpLogService.recordCompact(
                    userInfo,
                    sessionId,
                    runId,
                    updateBefore,
                    updateAfter
            );
        }

        return isUpdateByRuntime;
    }

    private boolean isCompacted(AiAgentStateLogEntity before, AiAgentStateLogEntity after) {
        if (before == null || after == null) {
            return false;
        }

        boolean summaryCreated =
                (before.getSummaryExists() == null || before.getSummaryExists() == 0)
                        && after.getSummaryExists() != null
                        && after.getSummaryExists() == 1;

        boolean messageReduced =
                before.getContextMessageCount() != null
                        && after.getContextMessageCount() != null
                        && after.getContextMessageCount() < before.getContextMessageCount();

        return summaryCreated || messageReduced;
    }

}
