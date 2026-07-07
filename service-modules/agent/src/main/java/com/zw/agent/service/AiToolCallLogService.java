package com.zw.agent.service;

import com.zw.agent.entity.AiToolCallLogEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.event.AgentRuntimeEvent;
import com.zw.common.context.UserInfo;

import java.util.Map;

/**
 * <p>
 * 工具调用审计表：记录Agent每一次工具调用的权限结果、参数、结果和耗时 服务类
 * </p>
 *
 * @author 智纬
 * @since 2026-06-28
 */
public interface AiToolCallLogService extends IService<AiToolCallLogEntity> {

    public void handleToolCallAuditEvent(
            String eventType,
            AgentRuntimeEvent runtimeEvent,
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId,
            Map<String, AiToolCallLogEntity> toolAuditMap
    );

}
