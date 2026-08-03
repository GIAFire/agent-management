package com.zhiran.agent.service;

import com.zhiran.agent.entity.DTO.AgentConfigDTO;
import com.zhiran.agent.entity.message.AgentInterventionRequest;
import com.zhiran.agent.event.AgentStreamResponse;
import com.zhiran.common.context.UserInfo;
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

    Flux<ServerSentEvent<AgentStreamResponse>> userConfirmStream(AgentConfigDTO config,
                                                                 UserInfo userInfo,
                                                                 Long sessionId,
                                                                 AgentInterventionRequest request);

    Flux<ServerSentEvent<AgentStreamResponse>> externalExecutionStream(AgentConfigDTO config,
                                                                       UserInfo userInfo,
                                                                       Long sessionId,
                                                                       AgentInterventionRequest request);

}
