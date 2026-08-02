package com.zw.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiAgentEntity;
import com.zw.agent.entity.AiAgentRunLogEntity;
import com.zw.agent.entity.DTO.AgentRecentRunResponse;
import com.zw.agent.entity.DTO.AgentRunSummary;
import com.zw.agent.entity.DTO.RunOverviewQuickAgentRow;
import com.zw.agent.entity.DTO.RunOverviewRecentInteractionRow;
import com.zw.agent.entity.DTO.RunOverviewResponse;
import com.zw.agent.entity.DTO.RunOverviewTrendRow;
import com.zw.agent.mapper.AiAgentMapper;
import com.zw.agent.mapper.AiAgentRunLogMapper;
import com.zw.agent.mapper.RunOverviewMapper;
import com.zw.common.context.UserContext;
import com.zw.common.context.UserInfo;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RunOverviewService {

    private static final int DEFAULT_TREND_DAYS = 7;
    private static final int MAX_TREND_DAYS = 90;
    private static final int RECENT_RUN_LIMIT = 7;
    private static final int RECENT_INTERACTION_LIMIT = 7;
    private static final int QUICK_AGENT_LIMIT = 5;

    private final AiAgentMapper agentMapper;
    private final AiAgentRunLogMapper runLogMapper;
    private final RunOverviewMapper overviewMapper;

    public RunOverviewResponse overview(LocalDate requestedStart, LocalDate requestedEnd) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        DateRange range = resolveRange(requestedStart, requestedEnd, today);
        UserInfo user = currentUser();
        Long tenantId = user.getTenantId();

        RunOverviewResponse.Metrics metrics = metrics(tenantId, now);
        RunOverviewResponse.Trend trend = trend(tenantId, range, now);
        List<AgentRecentRunResponse> recentRuns = recentRuns(tenantId);
        List<RunOverviewResponse.RecentInteraction> recentInteractions =
                recentInteractions(tenantId, user.getUserId());
        List<RunOverviewResponse.QuickAgent> quickAgents = quickAgents(tenantId, now);

        return new RunOverviewResponse(
                metrics,
                trend,
                recentRuns,
                recentInteractions,
                quickAgents
        );
    }

    public IPage<RunOverviewResponse.RecentInteraction> pageRecentInteractions(
            long current,
            long size,
            String keyword
    ) {
        UserInfo user = currentUser();
        long safeCurrent = Math.max(1L, current);
        int safeSize = (int) Math.min(50L, Math.max(1L, size));
        String safeKeyword = normalizeKeyword(keyword);
        long total = overviewMapper.countRecentInteractions(
                user.getTenantId(),
                user.getUserId(),
                safeKeyword
        );
        Page<RunOverviewResponse.RecentInteraction> page =
                new Page<>(safeCurrent, safeSize, total);
        if (total == 0L) {
            page.setRecords(List.of());
            return page;
        }
        long offset = (safeCurrent - 1L) * safeSize;
        List<RunOverviewRecentInteractionRow> rows = overviewMapper.selectRecentInteractionsPage(
                user.getTenantId(),
                user.getUserId(),
                safeKeyword,
                offset,
                safeSize
        );
        page.setRecords(toRecentInteractions(rows));
        return page;
    }

    private RunOverviewResponse.Metrics metrics(Long tenantId, LocalDateTime now) {
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime yesterdaySameTime = now.minusDays(1);
        LocalDateTime currentEnd = now.plusNanos(1);
        LocalDateTime previousEnd = yesterdaySameTime.plusNanos(1);

        long totalAgents = agentMapper.selectCount(currentAgents(tenantId));
        long newToday = agentMapper.selectCount(currentAgents(tenantId)
                .ge(AiAgentEntity::getCreatedAt, todayStart)
                .lt(AiAgentEntity::getCreatedAt, currentEnd));
        AgentRunSummary today = summary(tenantId, todayStart, currentEnd);
        AgentRunSummary yesterday = summary(tenantId, yesterdayStart, previousEnd);
        Double todaySuccessRate = successRate(today);
        Double yesterdaySuccessRate = successRate(yesterday);
        Double todayAverage = round(today.getAverageDurationMs());
        Double yesterdayAverage = round(yesterday.getAverageDurationMs());

        return new RunOverviewResponse.Metrics(
                totalAgents,
                newToday,
                value(today.getTotalRuns()),
                percentChange(value(today.getTotalRuns()), value(yesterday.getTotalRuns())),
                todaySuccessRate,
                difference(todaySuccessRate, yesterdaySuccessRate),
                todayAverage,
                difference(todayAverage, yesterdayAverage)
        );
    }

    private RunOverviewResponse.Trend trend(
            Long tenantId,
            DateRange range,
            LocalDateTime now
    ) {
        LocalDateTime start = range.startDate().atStartOfDay();
        LocalDateTime end = range.endDate().equals(now.toLocalDate())
                ? now.plusNanos(1)
                : range.endDate().plusDays(1).atStartOfDay();
        long days = ChronoUnit.DAYS.between(range.startDate(), range.endDate()) + 1;
        LocalDateTime previousEnd = start;
        LocalDateTime previousStart = previousEnd.minusDays(days);

        AgentRunSummary current = summary(tenantId, start, end);
        AgentRunSummary previous = summary(tenantId, previousStart, previousEnd);
        Map<LocalDate, RunOverviewTrendRow> rows = overviewMapper
                .selectTrend(tenantId, start, end)
                .stream()
                .collect(Collectors.toMap(
                        RunOverviewTrendRow::getRunDate,
                        Function.identity(),
                        (left, right) -> left
                ));
        List<RunOverviewResponse.TrendPoint> points = range.startDate()
                .datesUntil(range.endDate().plusDays(1))
                .map(date -> {
                    RunOverviewTrendRow row = rows.get(date);
                    return new RunOverviewResponse.TrendPoint(
                            date,
                            row == null ? 0 : value(row.getRunCount()),
                            row == null ? 0 : value(row.getSuccessCount())
                    );
                })
                .toList();

        Double currentSuccessRate = successRate(current);
        Double previousSuccessRate = successRate(previous);
        Double currentAverage = round(current.getAverageDurationMs());
        Double previousAverage = round(previous.getAverageDurationMs());
        RunOverviewResponse.TrendSummary trendSummary =
                new RunOverviewResponse.TrendSummary(
                        value(current.getTotalRuns()),
                        percentChange(
                                value(current.getTotalRuns()),
                                value(previous.getTotalRuns())
                        ),
                        value(current.getSuccessRuns()),
                        percentChange(
                                value(current.getSuccessRuns()),
                                value(previous.getSuccessRuns())
                        ),
                        currentSuccessRate,
                        difference(currentSuccessRate, previousSuccessRate),
                        currentAverage,
                        difference(currentAverage, previousAverage)
                );
        return new RunOverviewResponse.Trend(
                range.startDate(),
                range.endDate(),
                points,
                trendSummary
        );
    }

    private List<AgentRecentRunResponse> recentRuns(Long tenantId) {
        List<AiAgentRunLogEntity> runs = runLogMapper.selectPage(
                new Page<>(1, RECENT_RUN_LIMIT),
                new LambdaQueryWrapper<AiAgentRunLogEntity>()
                        .eq(AiAgentRunLogEntity::getTenantId, tenantId)
                        .eq(AiAgentRunLogEntity::getDeleted, 0)
                        .orderByDesc(AiAgentRunLogEntity::getStartedAt)
                        .orderByDesc(AiAgentRunLogEntity::getId)
        ).getRecords();
        Set<Long> agentIds = runs.stream()
                .map(AiAgentRunLogEntity::getAgentId)
                .collect(Collectors.toCollection(HashSet::new));
        Map<Long, String> agentNames = agentIds.isEmpty()
                ? Map.of()
                : agentMapper.selectList(currentAgents(tenantId)
                        .in(AiAgentEntity::getId, agentIds))
                .stream()
                .collect(Collectors.toMap(
                        AiAgentEntity::getId,
                        AiAgentEntity::getAgentName,
                        (left, right) -> left,
                        HashMap::new
                ));
        return runs.stream().map(run -> new AgentRecentRunResponse(
                run.getId(),
                run.getAgentId(),
                agentNames.getOrDefault(run.getAgentId(), "已删除智能体"),
                run.getStatus(),
                run.getErrorCode(),
                run.getErrorMessage(),
                run.getStartedAt(),
                run.getEndedAt(),
                durationMs(run.getStartedAt(), run.getEndedAt())
        )).toList();
    }

    private List<RunOverviewResponse.QuickAgent> quickAgents(
            Long tenantId,
            LocalDateTime now
    ) {
        LocalDateTime start = now.toLocalDate().minusDays(29).atStartOfDay();
        return overviewMapper.selectQuickAgents(
                        tenantId,
                        start,
                        now.plusNanos(1),
                        QUICK_AGENT_LIMIT
                ).stream()
                .map(this::toQuickAgent)
                .toList();
    }

    private List<RunOverviewResponse.RecentInteraction> recentInteractions(
            Long tenantId,
            Long userId
    ) {
        List<RunOverviewRecentInteractionRow> rows = overviewMapper.selectRecentInteractions(
                tenantId,
                userId,
                RECENT_INTERACTION_LIMIT
        );
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return toRecentInteractions(rows);
    }

    private List<RunOverviewResponse.RecentInteraction> toRecentInteractions(
            List<RunOverviewRecentInteractionRow> rows
    ) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream().map(this::toRecentInteraction).toList();
    }

    private RunOverviewResponse.RecentInteraction toRecentInteraction(
            RunOverviewRecentInteractionRow row
    ) {
        boolean agentAvailable = row.getAgentId() != null
                && row.getAgentName() != null
                && !row.getAgentName().isBlank();
        return new RunOverviewResponse.RecentInteraction(
                row.getRunId(),
                row.getSessionId(),
                row.getAgentId(),
                row.getAgentCode(),
                agentAvailable ? row.getAgentName() : "已删除智能体",
                row.getUserMessage(),
                row.getAssistantMessage(),
                row.getStatus(),
                row.getStartedAt(),
                agentAvailable
        );
    }

    private RunOverviewResponse.QuickAgent toQuickAgent(RunOverviewQuickAgentRow row) {
        return new RunOverviewResponse.QuickAgent(
                row.getId(),
                row.getAgentCode(),
                row.getAgentName(),
                row.getDescription(),
                row.getModelId(),
                row.getModelConfigName(),
                row.getProviderName(),
                row.getModelName(),
                value(row.getRunCount30Days())
        );
    }

    private DateRange resolveRange(
            LocalDate requestedStart,
            LocalDate requestedEnd,
            LocalDate today
    ) {
        if (requestedStart == null && requestedEnd == null) {
            return new DateRange(today.minusDays(DEFAULT_TREND_DAYS - 1L), today);
        }
        if (requestedStart == null || requestedEnd == null) {
            throw new IllegalArgumentException("开始日期和结束日期必须同时提供");
        }
        if (requestedStart.isAfter(requestedEnd)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }
        if (requestedEnd.isAfter(today)) {
            throw new IllegalArgumentException("结束日期不能晚于今天");
        }
        long days = ChronoUnit.DAYS.between(requestedStart, requestedEnd) + 1;
        if (days > MAX_TREND_DAYS) {
            throw new IllegalArgumentException("运行趋势最多查询 90 天");
        }
        return new DateRange(requestedStart, requestedEnd);
    }

    private AgentRunSummary summary(
            Long tenantId,
            LocalDateTime start,
            LocalDateTime end
    ) {
        AgentRunSummary result = runLogMapper.selectSummary(tenantId, start, end, null);
        return result == null ? new AgentRunSummary() : result;
    }

    private Double successRate(AgentRunSummary summary) {
        long finished = value(summary.getSuccessRuns())
                + value(summary.getFailedRuns())
                + value(summary.getCancelledRuns());
        if (finished == 0) {
            return null;
        }
        return round(value(summary.getSuccessRuns()) * 100D / finished);
    }

    private Double percentChange(long current, long previous) {
        if (previous == 0) {
            return current == 0 ? 0D : 100D;
        }
        return round((current - previous) * 100D / previous);
    }

    private Double difference(Double current, Double previous) {
        if (current == null || previous == null) {
            return null;
        }
        return round(current - previous);
    }

    private Double round(Double number) {
        if (number == null) {
            return null;
        }
        return Math.round(number * 100D) / 100D;
    }

    private long value(Long number) {
        return number == null ? 0L : number;
    }

    private Long durationMs(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return null;
        }
        return Math.max(0L, Duration.between(start, end).toMillis());
    }

    private LambdaQueryWrapper<AiAgentEntity> currentAgents(Long tenantId) {
        return new LambdaQueryWrapper<AiAgentEntity>()
                .eq(AiAgentEntity::getTenantId, tenantId)
                .eq(AiAgentEntity::getDeleted, 0);
    }

    private UserInfo currentUser() {
        UserInfo user = UserContext.get();
        if (user == null || user.getTenantId() == null || user.getUserId() == null) {
            throw new IllegalStateException("缺少认证用户上下文");
        }
        return user;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String value = keyword.trim();
        return value.length() > 100 ? value.substring(0, 100) : value;
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}
