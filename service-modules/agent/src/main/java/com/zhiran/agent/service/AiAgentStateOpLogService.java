package com.zhiran.agent.service;

import com.zhiran.agent.entity.AiAgentStateLogEntity;
import com.zhiran.agent.entity.AiAgentStateOpLogEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhiran.agent.entity.DTO.AgentConfigDTO;
import com.zhiran.common.context.UserInfo;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.harness.agent.HarnessAgent;

/**
 * <p>
 * AgentState操作日志表：记录状态加载、保存、压缩、清理等操作 服务类
 * </p>
 *
 * @author zhiRan
 * @since 2026-07-04
 */
public interface AiAgentStateOpLogService extends IService<AiAgentStateOpLogEntity> {

    int recordLoad(HarnessAgent agent, UserInfo userInfo, AgentConfigDTO config, RuntimeContext runtimeContext, Long sessionId, Long runId);

    void recordSave(
            UserInfo userInfo,
            Long sessionId,
            Long runId,
            AiAgentStateLogEntity before,
            AiAgentStateLogEntity after,
            boolean success,
            String errorMessage
    );

    void recordCompact(
            UserInfo userInfo,
            Long sessionId,
            Long runId,
            AiAgentStateLogEntity before,
            AiAgentStateLogEntity after
    );

    void recordSaveFailed(
            UserInfo userInfo,
            Long sessionId,
            Long runId,
            String errorMessage
    );
}
