package com.zhiran.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhiran.agent.entity.DTO.SysPromptAnalyticsResponse;
import com.zhiran.agent.entity.DTO.SysPromptDetailResponse;
import com.zhiran.agent.entity.DTO.SysPromptListItemResponse;
import com.zhiran.agent.entity.DTO.SysPromptMetricsResponse;
import com.zhiran.agent.entity.DTO.SysPromptOptionResponse;
import com.zhiran.agent.entity.DTO.SysPromptSaveRequest;
import com.zhiran.agent.service.SysPromptManagementService;
import com.zhiran.common.entity.Result;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.function.Supplier;
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
@RequestMapping("/sysPrompt")
@RequiredArgsConstructor
public class AiAgentSysPromptController {

    private final SysPromptManagementService managementService;

    @GetMapping("/metrics")
    public Result<SysPromptMetricsResponse> metrics() {
        return handle(managementService::metrics);
    }

    @GetMapping("/analytics")
    public Result<SysPromptAnalyticsResponse> analytics(
            @RequestParam(defaultValue = "5") int limit
    ) {
        return handle(() -> managementService.analytics(limit));
    }

    @GetMapping("/page")
    public Result<IPage<SysPromptListItemResponse>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "8") long size,
            @RequestParam(required = false) String keyword
    ) {
        return handle(() -> managementService.page(current, size, keyword));
    }

    @GetMapping("/list")
    public Result<List<SysPromptOptionResponse>> list() {
        return handle(managementService::listOptions);
    }

    @GetMapping("/{id}")
    public Result<SysPromptDetailResponse> detail(
            @PathVariable Long id
    ) {
        return Result.ok(managementService.detail(id));
    }

    @PostMapping("/create")
    public Result<SysPromptDetailResponse> create(
            @RequestBody SysPromptSaveRequest request
    ) {
        return handle(() -> managementService.create(request));
    }

    @PostMapping("/update")
    public Result<SysPromptDetailResponse> update(
            @RequestBody SysPromptSaveRequest request
    ) {
        return handle(() -> managementService.update(request));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return handle(() -> managementService.delete(id));
    }

    private <T> Result<T> handle(Supplier<T> supplier) {
        try {
            return Result.ok(supplier.get());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return Result.fail(exception.getMessage());
        }
    }
}
