package com.zw.agent.controller;

import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.entity.message.AgentChatRequest;
import com.zw.agent.runtime.AgentFullConfigService;
import com.zw.agent.runtime.AgentRuntimeFactory;
import com.zw.common.context.UserContext;
import com.zw.common.context.UserInfo;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@RestController
@RequestMapping("/chat")
public class AgentChatController {

    private final AgentRuntimeFactory agentRuntimeFactory;
    private final AgentFullConfigService agentFullConfigService;


    @PostMapping(value = "/chatStream",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AgentChatResponse>> chatStream(@RequestBody AgentChatRequest request) {
        UserInfo userInfo = UserContext.get();

        // 获取运行时配置
        AgentConfigDTO config = agentFullConfigService.loadPublishedConfig(
                userInfo.getTenantId(),
                request.getAgentId()
        );

        String tenantUserId = userInfo.getTenantId() + "-" + userInfo.getUserId();

        return agentRuntimeFactory
                .callStream(config, tenantUserId, request.getSessionId(), request.getContent())
                .map(delta -> ServerSentEvent.<AgentChatResponse>builder()
                        .event("message")
                        .data(new AgentChatResponse(delta))
                        .build()
                )
                .concatWithValues(
                        ServerSentEvent.<AgentChatResponse>builder()
                                .event("done")
                                .data(new AgentChatResponse("[DONE]"))
                                .build()
                )
                .onErrorResume(e -> Flux.just(
                        ServerSentEvent.<AgentChatResponse>builder()
                                .event("error")
                                .data(new AgentChatResponse("请求失败：" + e.getMessage()))
                                .build()
                ));
    }

    @PostMapping("/chatBlock")
    public Mono<AgentChatResponse> chatBlock(@RequestBody AgentChatRequest request) {
        UserInfo userInfo = UserContext.get();
        // 获取运行时配置
        AgentConfigDTO config = agentFullConfigService.loadPublishedConfig(userInfo.getTenantId(), request.getAgentId());
        String tenantUserId = userInfo.getTenantId().toString()+"-"+userInfo.getUserId().toString();

        return agentRuntimeFactory
                .call(config, tenantUserId, request.getSessionId(), request.getContent())
                .map(AgentChatResponse::new);
    }

    public record AgentChatResponse(String content) {}
}
