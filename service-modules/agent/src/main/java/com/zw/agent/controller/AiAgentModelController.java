package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zw.agent.entity.DTO.ModelAnalyticsResponse;
import com.zw.agent.entity.DTO.ModelCallLogResponse;
import com.zw.agent.entity.DTO.ModelCandidateResponse;
import com.zw.agent.entity.DTO.ModelDetailResponse;
import com.zw.agent.entity.DTO.ModelListItemResponse;
import com.zw.agent.entity.DTO.ModelMetricsResponse;
import com.zw.agent.entity.DTO.ModelSaveRequest;
import com.zw.agent.entity.DTO.ModelTestResponse;
import com.zw.agent.service.ModelManagementService;
import com.zw.common.entity.Result;
import jakarta.servlet.http.HttpServletResponse;
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
@RequestMapping("/modelConfig")
@RequiredArgsConstructor
public class AiAgentModelController {

    private final ModelManagementService managementService;

    @GetMapping("/metrics")
    public Result<ModelMetricsResponse> metrics() {
        return handle(managementService::metrics);
    }

    @GetMapping("/analytics")
    public Result<ModelAnalyticsResponse> analytics(
            @RequestParam(defaultValue = "7") int days
    ) {
        return handle(() -> managementService.analytics(days));
    }

    @GetMapping("/page")
    public Result<IPage<ModelListItemResponse>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String protocol,
            @RequestParam(required = false) Integer status
    ) {
        return handle(() -> managementService.page(
                current,
                size,
                keyword,
                provider,
                protocol,
                status
        ));
    }

    @GetMapping("/list")
    public Result<List<ModelCandidateResponse>> list() {
        return handle(managementService::listCandidates);
    }

    @GetMapping("/{id}")
    public Result<ModelDetailResponse> detail(
            @PathVariable Long id,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        return handle(() -> managementService.detail(id));
    }

    @PostMapping("/create")
    public Result<ModelListItemResponse> create(
            @RequestBody ModelSaveRequest request
    ) {
        return handle(() -> managementService.create(request));
    }

    @PostMapping("/update")
    public Result<ModelListItemResponse> update(
            @RequestBody ModelSaveRequest request
    ) {
        return handle(() -> managementService.update(request));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return handle(() -> managementService.delete(id));
    }

    @PostMapping("/test")
    public Result<ModelTestResponse> test(@RequestBody ModelSaveRequest request) {
        return handle(() -> managementService.test(request));
    }

    @GetMapping("/logs/page")
    public Result<IPage<ModelCallLogResponse>> logs(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long modelConfigId,
            @RequestParam(required = false) String callSource,
            @RequestParam(required = false) String status
    ) {
        return handle(() -> managementService.pageLogs(
                current,
                size,
                modelConfigId,
                callSource,
                status
        ));
    }

    private <T> Result<T> handle(java.util.function.Supplier<T> supplier) {
        try {
            return Result.ok(supplier.get());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return Result.fail(exception.getMessage());
        }
    }
}
