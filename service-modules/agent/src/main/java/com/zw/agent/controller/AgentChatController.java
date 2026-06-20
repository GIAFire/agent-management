package com.zw.agent.controller;

import com.zw.agent.runtime.AgentConfigQueryService;
import com.zw.agent.runtime.AgentRuntimeConfig;
import com.zw.agent.runtime.AgentRuntimeFactory;
import com.zw.agent.runtime.AgentRuntimeKeys;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@RestController
@RequestMapping("/chat")
public class AgentChatController {

    private final AgentRuntimeFactory agentRuntimeFactory;
    private final AgentConfigQueryService agentConfigQueryService;


    @PostMapping("/{agentId}/sessions/{sessionKey}/messages")
    public Mono<AgentChatResponse> chat(
            @RequestHeader("X-Tenant-Code") String tenantCode,
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long agentId,
            @PathVariable String sessionKey,
            @RequestBody AgentChatRequest request
    ) {
        // 获取运行时配置
        AgentRuntimeConfig config = agentConfigQueryService.loadPublishedConfig(tenantCode, agentId);
        String runtimeUserKey = AgentRuntimeKeys.userKey(tenantCode, userId);

        return agentRuntimeFactory
                .call(config, runtimeUserKey, sessionKey, request.content())
                .map(AgentChatResponse::new);
    }

    public record AgentChatRequest(String content) {}

    public record AgentChatResponse(String content) {}
}
