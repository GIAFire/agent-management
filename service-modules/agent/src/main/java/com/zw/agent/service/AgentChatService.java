package com.zw.agent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zw.agent.controller.AgentChatController;
import com.zw.agent.entity.AiAgentEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.event.AgentStreamResponse;
import com.zw.common.context.UserInfo;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * <p>
 * Agent 定义表：保存一个可视化 Agent 的基础身份信息 服务类
 * </p>
 *
 * @author
 * @since 2026-06-20
 */
public interface AgentChatService {

    Flux<ServerSentEvent<AgentStreamResponse>> chatStream(AgentConfigDTO config,
                                                          UserInfo userInfo,
                                                          Long sessionId,
                                                          String text,
                                                          Long runId);

}
