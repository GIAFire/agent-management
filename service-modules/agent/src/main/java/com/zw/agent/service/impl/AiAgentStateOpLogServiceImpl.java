package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentStateLogEntity;
import com.zw.agent.entity.AiAgentStateOpLogEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.mapper.AiAgentStateOpLogMapper;
import com.zw.agent.runtime.AgentRuntimeKeys;
import com.zw.agent.service.AiAgentStateOpLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.common.context.UserInfo;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.state.AgentState;
import io.agentscope.harness.agent.HarnessAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * <p>
 * AgentState操作日志表：记录状态加载、保存、压缩、清理等操作 服务实现类
 * </p>
 *
 * @author 智纬
 * @since 2026-07-04
 */
@RequiredArgsConstructor
@Service
public class AiAgentStateOpLogServiceImpl extends ServiceImpl<AiAgentStateOpLogMapper, AiAgentStateOpLogEntity> implements AiAgentStateOpLogService {
    private final AiAgentStateOpLogMapper agentStateOpLogMapper;

    @Override
    public int recordLoad(
            HarnessAgent agent,
            UserInfo userInfo,
            AgentConfigDTO config,
            RuntimeContext runtimeContext,
            Long sessionId,
            Long runId
    ) {
        AgentState agentState = agent.getDelegate().getAgentState(runtimeContext);

        AiAgentStateOpLogEntity op = new AiAgentStateOpLogEntity();
        op.setTenantId(userInfo.getTenantId());
        op.setSessionId(sessionId);
        op.setRunId(runId);
        op.setUserId(userInfo.getUserId());
        op.setOpType("LOAD");
        op.setStateBackend("REDIS");
        op.setStateKey(AgentRuntimeKeys.redisStateKey(
                userInfo.getUserId(),
                sessionId
        ));
        op.setSuccess(1);

        if (agentState != null) {
            String json = agentState.toJson();
            op.setAfterSizeBytes((long) json.getBytes(StandardCharsets.UTF_8).length);
            op.setAfterMessageCount(agentState.getContext().isEmpty() ? 0 : agentState.getContext().size());
        }

        op.setCreatedAt(LocalDateTime.now());
        return agentStateOpLogMapper.insert(op);
    }

    @Override
    public void recordSave(
            UserInfo userInfo,
            Long sessionId,
            Long runId,
            AiAgentStateLogEntity before,
            AiAgentStateLogEntity after,
            boolean success,
            String errorMessage
    ) {
        AiAgentStateOpLogEntity op = new AiAgentStateOpLogEntity();
        op.setTenantId(userInfo.getTenantId());
        op.setSessionId(sessionId);
        op.setRunId(runId);
        op.setUserId(userInfo.getUserId());
        op.setOpType("SAVE");
        op.setStateBackend("REDIS");
        op.setStateKey(AgentRuntimeKeys.redisStateKey(
                userInfo.getUserId(),
                sessionId));

        if (before != null) {
            op.setBeforeSizeBytes(before.getStateSizeBytes());
            op.setBeforeMessageCount(before.getContextMessageCount());
        }

        if (after != null) {
            op.setAfterSizeBytes(after.getStateSizeBytes());
            op.setAfterMessageCount(after.getContextMessageCount());
        }

        op.setSuccess(success ? 1 : 0);
        op.setErrorMessage(errorMessage);
        op.setCreatedAt(LocalDateTime.now());

        agentStateOpLogMapper.insert(op);
    }

    @Override
    public void recordCompact(
            UserInfo userInfo,
            Long sessionId,
            Long runId,
            AiAgentStateLogEntity before,
            AiAgentStateLogEntity after
    ) {
        AiAgentStateOpLogEntity op = new AiAgentStateOpLogEntity();
        op.setTenantId(userInfo.getTenantId());
        op.setSessionId(sessionId);
        op.setRunId(runId);
        op.setUserId(userInfo.getUserId());
        op.setOpType("COMPACT");
        op.setStateBackend("REDIS");
        op.setStateKey(AgentRuntimeKeys.redisStateKey(userInfo.getUserId(), sessionId));
        op.setBeforeSizeBytes(before == null ? null : before.getStateSizeBytes());
        op.setAfterSizeBytes(after == null ? null : after.getStateSizeBytes());
        op.setBeforeMessageCount(before == null ? null : before.getContextMessageCount());
        op.setAfterMessageCount(after == null ? null : after.getContextMessageCount());
        op.setSuccess(1);
        op.setCreatedAt(LocalDateTime.now());

        agentStateOpLogMapper.insert(op);
    }

    @Override
    public void recordSaveFailed(
            UserInfo userInfo,
            Long sessionId,
            Long runId,
            String errorMessage
    ) {
        AiAgentStateOpLogEntity op = new AiAgentStateOpLogEntity();
        op.setTenantId(userInfo.getTenantId());
        op.setSessionId(sessionId);
        op.setRunId(runId);
        op.setUserId(userInfo.getUserId());
        op.setOpType("SAVE");
        op.setStateBackend("REDIS");
        op.setStateKey(AgentRuntimeKeys.redisStateKey(userInfo.getUserId(), sessionId));
        op.setSuccess(0);
        op.setErrorMessage(errorMessage);
        op.setCreatedAt(LocalDateTime.now());

        agentStateOpLogMapper.insert(op);
    }
}
