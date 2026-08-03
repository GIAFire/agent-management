package com.zhiran.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhiran.agent.entity.DTO.SkillUseLogResponse;
import com.zhiran.agent.service.SkillManagementService;
import com.zhiran.common.entity.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/skillLog")
@RequiredArgsConstructor
public class AiSkillLogController {

    private final SkillManagementService managementService;

    @GetMapping("/page")
    public Result<IPage<SkillUseLogResponse>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long skillId,
            @RequestParam(required = false) Byte success,
            @RequestParam(required = false) String operation
    ) {
        return Result.ok(managementService.pageLogs(
                current, size, skillId, success, operation
        ));
    }
}
