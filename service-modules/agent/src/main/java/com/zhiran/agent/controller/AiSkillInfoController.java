package com.zhiran.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhiran.agent.entity.AiSkillInfoEntity;
import com.zhiran.agent.entity.DTO.AiSkillInfoSaveRequest;
import com.zhiran.agent.entity.DTO.SkillDetailResponse;
import com.zhiran.agent.entity.DTO.SkillListItemResponse;
import com.zhiran.agent.entity.DTO.SkillMetricsResponse;
import com.zhiran.agent.entity.DTO.SkillUseLogResponse;
import com.zhiran.agent.service.SkillManagementService;
import com.zhiran.common.entity.Result;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/skillInfo")
@RequiredArgsConstructor
public class AiSkillInfoController {

    private final SkillManagementService managementService;

    @GetMapping("/metrics")
    public Result<SkillMetricsResponse> metrics() {
        return Result.ok(managementService.metrics());
    }

    @GetMapping("/list")
    public Result<List<AiSkillInfoEntity>> listCandidates() {
        return Result.ok(managementService.listCandidates());
    }

    @GetMapping("/page")
    public Result<IPage<SkillListItemResponse>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Byte status,
            @RequestParam(required = false) String riskLevel
    ) {
        return Result.ok(managementService.pageSkills(
                current, size, keyword, category, status, riskLevel
        ));
    }

    @GetMapping("/{id}")
    public Result<SkillDetailResponse> getById(@PathVariable Long id) {
        return Result.ok(managementService.detail(id));
    }

    @PostMapping("/create")
    public Result<SkillDetailResponse> create(@RequestBody AiSkillInfoSaveRequest request) {
        return Result.ok(managementService.create(request));
    }

    @PostMapping("/update")
    public Result<SkillDetailResponse> update(@RequestBody AiSkillInfoSaveRequest request) {
        return Result.ok(managementService.update(request));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(managementService.delete(id));
    }

    @GetMapping("/logs/page")
    public Result<IPage<SkillUseLogResponse>> pageLogs(
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

    @GetMapping("/logs/recent")
    public Result<List<SkillUseLogResponse>> recentLogs(
            @RequestParam(defaultValue = "6") int limit,
            @RequestParam(required = false) Byte success
    ) {
        return Result.ok(managementService.recentLogs(limit, success));
    }
}
