package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zw.agent.entity.DTO.AgentDetailResponse;
import com.zw.agent.entity.DTO.AgentListItemResponse;
import com.zw.agent.entity.DTO.AgentMetricsResponse;
import com.zw.agent.entity.DTO.AgentRunLogResponse;
import com.zw.agent.entity.DTO.AgentSaveRequest;
import com.zw.agent.service.AgentManagementService;
import com.zw.common.entity.Result;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AiAgentController {

    private final AgentManagementService managementService;

    @GetMapping("/metrics")
    public Result<AgentMetricsResponse> metrics() {
        return handle(managementService::metrics);
    }

    @GetMapping("/page")
    public Result<IPage<AgentListItemResponse>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword
    ) {
        return handle(() -> managementService.page(current, size, keyword));
    }

    @GetMapping("/{id}")
    public Result<AgentDetailResponse> detail(@PathVariable Long id) {
        return handle(() -> managementService.detail(id));
    }

    @PostMapping
    public Result<AgentDetailResponse> create(@RequestBody AgentSaveRequest request) {
        return handle(() -> managementService.create(request));
    }

    @PutMapping("/{id}")
    public Result<AgentDetailResponse> update(
            @PathVariable Long id,
            @RequestBody AgentSaveRequest request
    ) {
        return handle(() -> managementService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return handle(() -> managementService.delete(id));
    }

    @GetMapping("/{id}/runs")
    public Result<IPage<AgentRunLogResponse>> runs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return handle(() -> managementService.runs(
                id, current, size, status, start, end));
    }

    private <T> Result<T> handle(Supplier<T> supplier) {
        try {
            return Result.ok(supplier.get());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return Result.fail(exception.getMessage());
        }
    }
}
