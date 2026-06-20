package com.zw.agent.controller;

import com.zw.agent.runtime.AgentConfigQueryService;
import com.zw.agent.runtime.AgentRuntimeConfig;
import com.zw.agent.runtime.AgentRuntimeFactory;
import com.zw.agent.service.impl.AgentServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private final AgentServiceImpl agentService;

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
        AgentRuntimeConfig config = agentConfigQueryService.loadPublishedConfig(tenantCode, agentId);

        String runtimeUserKey = tenantCode + ":" + userId;

        return agentRuntimeFactory
                .call(config, runtimeUserKey, sessionKey, request.content())
                .map(AgentChatResponse::new);
    }

    public record AgentChatRequest(String content) {
    }

    public record AgentChatResponse(String content) {
    }
}
