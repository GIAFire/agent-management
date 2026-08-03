package com.zhiran.agent.service;

import com.zhiran.agent.entity.AiAgentRunEventLogEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhiran.common.context.UserInfo;

/**
 * <p>
 * Agent 运行事件表：保存流式 token、工具调用、权限请求等事件 服务类
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
public interface AiAgentRunEventLogService extends IService<AiAgentRunEventLogEntity> {

    int saveEvent(UserInfo userInfo, Long runId, Long sessionId, String runtimeEvent);
}
