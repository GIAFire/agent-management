package com.zw.agent.service;

import com.zw.agent.entity.AiAgentMessageLogEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zw.common.context.UserInfo;

/**
 * <p>
 * Agent 消息表：保存用户输入、Agent 回复、工具消息等完整上下文 服务类
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
public interface AiAgentMessageLogService extends IService<AiAgentMessageLogEntity> {

    AiAgentMessageLogEntity saveUserMessage(UserInfo userInfo, Long sessionId, String content);

    void saveAssistantMessage(UserInfo userInfo, Long sessionId, Long runId, String msg,String agentName, Integer usageToken, Double usageTime);
}
