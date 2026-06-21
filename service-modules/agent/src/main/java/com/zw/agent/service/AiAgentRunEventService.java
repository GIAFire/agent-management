package com.zw.agent.service;

import com.zw.agent.entity.AiAgentRunEventEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zw.agent.event.AgentRuntimeEvent;

/**
 * <p>
 * Agent 运行事件表：保存流式 token、工具调用、权限请求等事件 服务类
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
public interface AiAgentRunEventService extends IService<AiAgentRunEventEntity> {

    int saveEvent(Long tenantId, Long runId, Long sessionId, int eventSeq, AgentRuntimeEvent runtimeEvent);
}
