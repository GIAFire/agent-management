package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AgentEntity;
import com.zw.agent.runtime.AgentConfigQueryService;
import com.zw.agent.runtime.AgentRuntimeConfig;
import com.zw.agent.runtime.AgentRuntimeFactory;
import com.zw.agent.service.AgentService;
import com.zw.common.entity.Result;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private final AgentService agentService;

    private final AgentRuntimeFactory agentRuntimeFactory;
    private final AgentConfigQueryService agentConfigQueryService;

    @GetMapping("/list")
    public Result<List<AgentEntity>> list() {
        return Result.ok(agentService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AgentEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(agentService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AgentEntity> getById(@PathVariable Long id) {
        return Result.ok(agentService.getById(id));
    }

    @PostMapping
    public Result<Boolean> create(@RequestBody AgentEntity entity) {
        return Result.ok(agentService.save(entity));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody AgentEntity entity) {
        return Result.ok(agentService.updateById(entity));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(agentService.removeById(id));
    }

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
