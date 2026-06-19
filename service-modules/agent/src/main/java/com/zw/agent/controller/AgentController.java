package com.zw.agent.controller;

import com.zw.agent.entity.AgentEntity;
import com.zw.agent.service.impl.AgentServiceImpl;
import com.zw.entity.Result;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private final AgentServiceImpl agentService;

    @GetMapping("/{agentId}")
    public Result getAgent(@PathVariable String agentId) {
        AgentEntity byId = agentService.getById(agentId);
        return Result.ok(byId);
    }
}
