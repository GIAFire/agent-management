package com.zw.agent.controller;

import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.entity.message.AgentChatRequest;
import com.zw.agent.runtime.AgentFullConfigService;
import com.zw.agent.runtime.AgentRuntimeFactory;
import com.zw.common.context.UserContext;
import com.zw.common.context.UserInfo;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@RestController
@RequestMapping("/chat")
public class AgentChatController {

    private final AgentRuntimeFactory agentRuntimeFactory;
    private final AgentFullConfigService agentFullConfigService;


    @PostMapping("/messages")
    public Mono<AgentChatResponse> chat(@RequestBody AgentChatRequest request) {
        UserInfo userInfo = UserContext.get();
        // 获取运行时配置
        AgentConfigDTO config = agentFullConfigService.loadPublishedConfig(userInfo.getTenantId(), request.getAgentId());
        String runtimeUserKey = userInfo.getTenantId().toString()+userInfo.getUserId().toString();

        return agentRuntimeFactory
                .call(config, runtimeUserKey, request.getSessionKey(), request.getContent())
                .map(AgentChatResponse::new);
    }

    public record AgentChatResponse(String content) {}
}
