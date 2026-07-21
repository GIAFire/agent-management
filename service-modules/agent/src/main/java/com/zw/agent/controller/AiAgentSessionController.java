package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiAgentSessionEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.entity.message.AiAgentSessionCreateRequest;
import com.zw.agent.service.AiAgentService;
import com.zw.agent.service.AiAgentSessionService;
import com.zw.common.context.UserContext;
import com.zw.common.context.UserInfo;
import com.zw.common.entity.Result;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * Agent 会话表：保存用户与 Agent 的一次连续对话 前端控制器
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@RestController
@RequestMapping("/agentSession")
@AllArgsConstructor
public class AiAgentSessionController {
    private final AiAgentSessionService aiAgentSessionService;
    private final AiAgentService aiAgentService;

    @GetMapping("/list")
    public Result<List<AiAgentSessionEntity>> list() {
        return Result.ok(aiAgentSessionService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiAgentSessionEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) String keyword
    ) {
        UserInfo userInfo = UserContext.get();
        LambdaQueryWrapper<AiAgentSessionEntity> query = new LambdaQueryWrapper<>();
        query.eq(AiAgentSessionEntity::getTenantId, userInfo.getTenantId())
                .eq(AiAgentSessionEntity::getUserId, userInfo.getUserId());
        if (agentId != null) {
            query.eq(AiAgentSessionEntity::getAgentId, agentId);
        }
        if (StringUtils.hasText(keyword)) {
            query.like(AiAgentSessionEntity::getTitle, keyword.trim());
        }
        query.orderByDesc(AiAgentSessionEntity::getLastMessageAt)
                .orderByDesc(AiAgentSessionEntity::getUpdatedAt)
                .orderByDesc(AiAgentSessionEntity::getCreatedAt);
        return Result.ok(aiAgentSessionService.page(new Page<>(current, size), query));
    }

    @GetMapping("/{id}")
    public Result<AiAgentSessionEntity> getById(@PathVariable Long id) {
        return Result.ok(aiAgentSessionService.getById(id));
    }

    @PostMapping("/create")
    public Result<AiAgentSessionEntity> create(@RequestBody AiAgentSessionCreateRequest request) {
        if (request == null || request.getAgentId() == null) {
            return Result.fail("agentId must not be null");
        }
        UserInfo userInfo = UserContext.get();
        AgentConfigDTO agentConfig = aiAgentService.getAgentConfigById(request.getAgentId(),userInfo);
        return Result.ok(aiAgentSessionService.createSession(
                userInfo,
                request.getAgentId(),
                agentConfig.getAgentConfigId(),
                request.getTitle()
        ));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiAgentSessionEntity entity) {
        return Result.ok(aiAgentSessionService.updateById(entity));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiAgentSessionService.removeById(id));
    }

}
