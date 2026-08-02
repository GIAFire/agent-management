package com.zw.agent.controller;

import com.zw.agent.entity.DTO.RunOverviewResponse;
import com.zw.agent.service.RunOverviewService;
import com.zw.common.entity.Result;
import java.time.LocalDate;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/overview")
@RequiredArgsConstructor
public class RunOverviewController {

    private final RunOverviewService overviewService;

    @GetMapping
    public Result<RunOverviewResponse> overview(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return handle(() -> overviewService.overview(startDate, endDate));
    }

    private <T> Result<T> handle(Supplier<T> supplier) {
        try {
            return Result.ok(supplier.get());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return Result.fail(exception.getMessage());
        }
    }
}
