package com.zhiran.agent.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiran.agent.entity.AiSkillAgentBindingEntity;
import com.zhiran.agent.service.AiSkillAgentBindingService;
import com.zhiran.common.context.UserContext;
import com.zhiran.common.entity.Result;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/skillAgentBinding")
@RequiredArgsConstructor
public class AiSkillAgentBindingController {

    private final AiSkillAgentBindingService bindingService;

    @GetMapping("/list")
    public Result<List<AiSkillAgentBindingEntity>> list(
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) Long agentConfigId
    ) {
        Long tenantId = UserContext.get().getTenantId();
        LambdaQueryWrapper<AiSkillAgentBindingEntity> query =
                new LambdaQueryWrapper<AiSkillAgentBindingEntity>()
                        .eq(AiSkillAgentBindingEntity::getTenantId, tenantId);
        if (agentId != null) {
            query.eq(AiSkillAgentBindingEntity::getAgentId, agentId);
        }
        if (agentConfigId != null) {
            query.eq(AiSkillAgentBindingEntity::getAgentConfigId, agentConfigId);
        }
        return Result.ok(bindingService.list(query));
    }
}
