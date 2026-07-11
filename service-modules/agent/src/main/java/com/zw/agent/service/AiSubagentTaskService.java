package com.zw.agent.service;

import com.zw.agent.entity.AiSubagentTaskEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zw.agent.entity.AiToolCallLogEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.common.context.UserInfo;

/**
 * <p>
 * 子Agent任务表：记录agent_spawn/agent_send产生的同步或后台委派任务 服务类
 * </p>
 *
 * @author 智纬
 * @since 2026-07-11
 */
public interface AiSubagentTaskService extends IService<AiSubagentTaskEntity> {

    void recordDelegationTask(
            AiToolCallLogEntity toolCallLog,
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId
    );
}
