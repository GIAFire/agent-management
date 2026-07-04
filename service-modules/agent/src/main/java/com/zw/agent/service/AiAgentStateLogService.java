package com.zw.agent.service;

import com.zw.agent.entity.AiAgentStateLogEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.common.context.UserInfo;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.harness.agent.HarnessAgent;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * AgentState状态引用表：记录AgentScope运行时状态在外部存储中的位置和元数据 服务类
 * </p>
 *
 * @author 智纬
 * @since 2026-07-04
 */
public interface AiAgentStateLogService extends IService<AiAgentStateLogEntity> {


    CompletableFuture<Boolean> saveStateLog(UserInfo userInfo,
                                                 AgentConfigDTO config,
                                                 Long sessionId,
                                                 String userKey,
                                                 String sessionKey);

    Boolean updateState(
            HarnessAgent agent,
            RuntimeContext runtimeContext,
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId
    );

}
