package com.zw.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.constant.enumeration.HeaderSourceType;
import com.zw.agent.constant.enumeration.ModelProtocol;
import com.zw.agent.entity.AiAgentModelEntity;
import com.zw.agent.entity.AiHttpHeaderEntity;
import com.zw.agent.entity.AiModelCallLogEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.entity.DTO.ModelAnalyticsResponse;
import com.zw.agent.entity.DTO.ModelCallLogResponse;
import com.zw.agent.entity.DTO.ModelCallSummary;
import com.zw.agent.entity.DTO.ModelCandidateResponse;
import com.zw.agent.entity.DTO.ModelDetailResponse;
import com.zw.agent.entity.DTO.ModelHeaderInput;
import com.zw.agent.entity.DTO.ModelHeaderResponse;
import com.zw.agent.entity.DTO.ModelListItemResponse;
import com.zw.agent.entity.DTO.ModelMetricsResponse;
import com.zw.agent.entity.DTO.ModelSaveRequest;
import com.zw.agent.entity.DTO.ModelTestResponse;
import com.zw.agent.entity.DTO.ModelTrendRow;
import com.zw.agent.factory.agentFactory.AgentRuntimeFactory;
import com.zw.agent.factory.modelFactory.ModelFactory;
import com.zw.agent.mapper.AiAgentModelMapper;
import com.zw.agent.mapper.AiHttpHeaderMapper;
import com.zw.agent.mapper.AiModelCallLogMapper;
import com.zw.agent.runtime.model.ModelAuditDescriptor;
import com.zw.common.context.UserContext;
import com.zw.common.context.UserInfo;
import com.zw.common.support.EntityDefaults;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.ChatUsage;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ModelManagementService {

    private static final int ENABLED = 1;
    private static final long DEFAULT_TIMEOUT_MS = 60_000L;
    private static final int DEFAULT_MAX_ATTEMPTS = 1;

    private final AiAgentModelService modelService;
    private final AiAgentModelMapper modelMapper;
    private final AiHttpHeaderService headerService;
    private final AiHttpHeaderMapper headerMapper;
    private final AiModelCallLogService callLogService;
    private final AiModelCallLogMapper callLogMapper;
    private final ModelFactory modelFactory;
    private final ModelCallAuditService auditService;
    private final AgentRuntimeFactory agentRuntimeFactory;

    public ModelMetricsResponse metrics() {
        Long tenantId = tenantId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime yesterdaySameTime = now.minusDays(1);

        long total = modelService.count(new LambdaQueryWrapper<AiAgentModelEntity>()
                .eq(AiAgentModelEntity::getTenantId, tenantId));
        long enabled = modelService.count(new LambdaQueryWrapper<AiAgentModelEntity>()
                .eq(AiAgentModelEntity::getTenantId, tenantId)
                .eq(AiAgentModelEntity::getStatus, ENABLED));
        ModelCallSummary today = summary(tenantId, todayStart, now.plusNanos(1));
        ModelCallSummary yesterday = summary(
                tenantId,
                yesterdayStart,
                yesterdaySameTime.plusNanos(1)
        );
        long finished = value(today.getSuccessCalls()) + value(today.getFailedCalls());
        Double successRate = finished == 0
                ? null
                : round(value(today.getSuccessCalls()) * 100D / finished);
        Double todayAverage = today.getAverageDurationMs() == null
                ? null
                : round(today.getAverageDurationMs());
        Double averageChange = todayAverage == null
                || yesterday.getAverageDurationMs() == null
                ? null
                : round(todayAverage - yesterday.getAverageDurationMs());

        return new ModelMetricsResponse(
                total,
                enabled,
                value(today.getTotalCalls()),
                percentChange(value(today.getTotalCalls()), value(yesterday.getTotalCalls())),
                successRate,
                value(today.getFailedCalls()),
                todayAverage,
                averageChange
        );
    }

    public ModelAnalyticsResponse analytics(int days) {
        if (days != 7 && days != 30) {
            throw new IllegalArgumentException("days 仅支持 7 或 30");
        }
        Long tenantId = tenantId();
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1L);
        List<ModelTrendRow> rows = callLogMapper.selectBusinessTrend(
                tenantId,
                startDate.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );
        Map<LocalDate, Long> callCounts = new HashMap<>();
        for (ModelTrendRow row : rows) {
            callCounts.put(row.getCallDate(), value(row.getCallCount()));
        }
        List<ModelAnalyticsResponse.TrendPoint> trend = new ArrayList<>(days);
        for (int offset = 0; offset < days; offset++) {
            LocalDate date = startDate.plusDays(offset);
            trend.add(new ModelAnalyticsResponse.TrendPoint(
                    date,
                    callCounts.getOrDefault(date, 0L)
            ));
        }

        List<AiAgentModelEntity> models = modelService.list(
                new LambdaQueryWrapper<AiAgentModelEntity>()
                        .eq(AiAgentModelEntity::getTenantId, tenantId)
        );
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (AiAgentModelEntity model : models) {
            String provider = StringUtils.hasText(model.getProviderName())
                    ? model.getProviderName()
                    : "未填写";
            distribution.merge(provider, 1L, Long::sum);
        }
        long modelTotal = models.size();
        List<ModelAnalyticsResponse.ProviderDistribution> providerDistribution =
                distribution.entrySet().stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .map(entry -> new ModelAnalyticsResponse.ProviderDistribution(
                                entry.getKey(),
                                entry.getValue(),
                                modelTotal == 0
                                        ? 0D
                                        : round(entry.getValue() * 100D / modelTotal)
                        ))
                        .toList();
        return new ModelAnalyticsResponse(days, trend, providerDistribution);
    }

    public IPage<ModelListItemResponse> page(
            long current,
            long size,
            String keyword,
            String provider,
            String protocol,
            Integer status
    ) {
        Long tenantId = tenantId();
        LambdaQueryWrapper<AiAgentModelEntity> query =
                new LambdaQueryWrapper<AiAgentModelEntity>()
                        .eq(AiAgentModelEntity::getTenantId, tenantId)
                        .eq(status != null, AiAgentModelEntity::getStatus, status)
                        .eq(StringUtils.hasText(provider),
                                AiAgentModelEntity::getProviderName,
                                trim(provider))
                        .eq(StringUtils.hasText(protocol),
                                AiAgentModelEntity::getProtocol,
                                parseProtocol(protocol))
                        .and(StringUtils.hasText(keyword), wrapper -> wrapper
                                .like(AiAgentModelEntity::getConfigName, trim(keyword))
                                .or()
                                .like(AiAgentModelEntity::getProviderName, trim(keyword))
                                .or()
                                .like(AiAgentModelEntity::getModelName, trim(keyword)))
                        .orderByDesc(AiAgentModelEntity::getUpdatedAt)
                        .orderByDesc(AiAgentModelEntity::getId);
        Page<AiAgentModelEntity> modelPage = modelService.page(
                new Page<>(normalizePage(current), normalizeSize(size)),
                query
        );
        if (modelPage.getRecords().isEmpty()) {
            return modelPage.convert(model -> toListItem(model, 0L, null));
        }

        List<Long> modelIds = modelPage.getRecords().stream()
                .map(AiAgentModelEntity::getId)
                .toList();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<AiModelCallLogEntity> todayLogs = callLogService.list(
                new LambdaQueryWrapper<AiModelCallLogEntity>()
                        .eq(AiModelCallLogEntity::getTenantId, tenantId)
                        .in(AiModelCallLogEntity::getModelConfigId, modelIds)
                        .eq(AiModelCallLogEntity::getCallSource,
                                ModelCallAuditService.SOURCE_AGENT_RUN)
                        .ge(AiModelCallLogEntity::getStartedAt, todayStart)
        );
        Map<Long, Long> todayCalls = new HashMap<>();
        Map<Long, AiModelCallLogEntity> recentTests = new HashMap<>();
        for (AiModelCallLogEntity log : todayLogs) {
            if (log.getModelConfigId() == null) {
                continue;
            }
            todayCalls.merge(log.getModelConfigId(), 1L, Long::sum);
        }
        for (AiModelCallLogEntity log
                : callLogMapper.selectLatestManualTests(tenantId, modelIds)) {
            recentTests.put(log.getModelConfigId(), log);
        }
        return modelPage.convert(model -> toListItem(
                model,
                todayCalls.getOrDefault(model.getId(), 0L),
                recentTests.get(model.getId())
        ));
    }

    public List<ModelCandidateResponse> listCandidates() {
        return modelService.list(new LambdaQueryWrapper<AiAgentModelEntity>()
                        .eq(AiAgentModelEntity::getTenantId, tenantId())
                        .eq(AiAgentModelEntity::getStatus, ENABLED)
                        .orderByAsc(AiAgentModelEntity::getConfigName))
                .stream()
                .map(this::toCandidate)
                .toList();
    }

    public ModelDetailResponse detail(Long id) {
        AiAgentModelEntity model = requireModel(id);
        List<ModelHeaderResponse> headers = headerService.getHeaderList(
                        model.getId(),
                        tenantId(),
                        HeaderSourceType.MODEL
                ).stream()
                .map(header -> new ModelHeaderResponse(
                        header.getId(),
                        header.getHeaderName(),
                        header.getHeaderValue()
                ))
                .toList();
        return new ModelDetailResponse(
                model.getId(),
                model.getConfigName(),
                model.getProviderName(),
                model.getProtocol().getCode(),
                model.getBaseURL(),
                model.getApiKey(),
                model.getDescription(),
                model.getModelName(),
                model.getStreaming(),
                model.getThinking(),
                model.getTemperature(),
                model.getTopP(),
                model.getMaxTokens(),
                model.getTimeoutMs(),
                model.getThinkingBudget(),
                model.getMaxAttempts(),
                model.getStatus(),
                headers,
                model.getCreatedAt(),
                model.getUpdatedAt()
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public ModelListItemResponse create(ModelSaveRequest request) {
        AiAgentModelEntity model = toEntity(request, null);
        validateModel(model, null);
        model.setTenantId(tenantId());
        modelService.save(EntityDefaults.create(model));
        syncHeaders(model.getId(), request.getHeaders(), true);
        invalidateAgentsAfterCommit();
        return toListItem(model, 0L, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public ModelListItemResponse update(ModelSaveRequest request) {
        if (request == null || request.getId() == null) {
            throw new IllegalArgumentException("模型配置 ID 不能为空");
        }
        AiAgentModelEntity stored = requireModel(request.getId());
        AiAgentModelEntity model = toEntity(request, stored);
        validateModel(model, stored.getId());
        model.setId(stored.getId());
        model.setTenantId(tenantId());
        modelService.updateById(EntityDefaults.update(model));
        syncHeaders(model.getId(), request.getHeaders(), false);
        invalidateAgentsAfterCommit();
        return toListItem(model, countTodayCalls(model.getId()), recentTest(model.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        AiAgentModelEntity model = requireModel(id);
        Long tenantId = tenantId();
        long references = modelMapper.countAgentConfigReferences(model.getId(), tenantId);
        long logs = callLogService.count(new LambdaQueryWrapper<AiModelCallLogEntity>()
                .eq(AiModelCallLogEntity::getTenantId, tenantId)
                .eq(AiModelCallLogEntity::getModelConfigId, model.getId()));
        if (references == 0 && logs == 0) {
            headerMapper.hardDeleteModelHeaders(model.getId(), tenantId);
            if (modelMapper.hardDeleteById(model.getId(), tenantId) != 1) {
                throw new IllegalStateException("模型配置删除失败");
            }
        } else {
            modelService.removeById(model.getId());
            headerService.remove(new LambdaQueryWrapper<AiHttpHeaderEntity>()
                    .eq(AiHttpHeaderEntity::getTenantId, tenantId)
                    .eq(AiHttpHeaderEntity::getSourceId, model.getId())
                    .eq(AiHttpHeaderEntity::getSource, HeaderSourceType.MODEL));
        }
        invalidateAgentsAfterCommit();
        return true;
    }

    public ModelTestResponse test(ModelSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("测试配置不能为空");
        }
        AiAgentModelEntity stored = request.getId() == null
                ? null
                : requireModel(request.getId());
        AiAgentModelEntity model = toEntity(request, stored);
        validateModel(model, stored == null ? null : stored.getId());
        if (stored != null) {
            model.setId(stored.getId());
        }
        model.setTenantId(tenantId());
        List<AiHttpHeaderEntity> headers = testHeaders(request, stored);
        AgentConfigDTO config = toAgentConfig(model);
        List<String> secrets = secretValues(model, headers);
        ModelAuditDescriptor descriptor = new ModelAuditDescriptor(
                currentUser(),
                model.getId(),
                null,
                null,
                model.getConfigName(),
                model.getProviderName(),
                model.getProtocol().getCode(),
                model.getModelName(),
                "manual-test",
                secrets
        );
        AiModelCallLogEntity audit = auditService.start(
                descriptor,
                null,
                null,
                ModelCallAuditService.SOURCE_MANUAL_TEST
        );
        long startedNanos = System.nanoTime();
        AtomicReference<ChatUsage> usage = new AtomicReference<>();
        StringBuilder reply = new StringBuilder();
        try {
            Model runtimeModel = modelFactory.buildModel(config, headers);
            GenerateOptions options = GenerateOptions.builder()
                    .stream(false)
                    .maxTokens(Math.min(model.getMaxTokens(), 16))
//                    .maxCompletionTokens(Math.min(model.getMaxTokens(), 16))
                    .build();
            runtimeModel.stream(
                            List.of(new UserMessage("请只回复 OK")),
                            List.of(),
                            options
                    )
                    .doOnNext(response -> collectResponse(response, usage, reply))
                    .blockLast(testWait(model));
            auditService.success(descriptor, audit, usage.get());
            return new ModelTestResponse(
                    true,
                    elapsedMs(startedNanos),
                    model.getModelName(),
                    limit(reply.toString(), 200),
                    null,
                    null
            );
        } catch (RuntimeException error) {
            auditService.failed(descriptor, audit, error);
            return new ModelTestResponse(
                    false,
                    elapsedMs(startedNanos),
                    model.getModelName(),
                    null,
                    error.getClass().getSimpleName(),
                    limit(redact(safeMessage(error), secrets), 1000)
            );
        }
    }

    public IPage<ModelCallLogResponse> pageLogs(
            long current,
            long size,
            Long modelConfigId,
            String callSource,
            String status
    ) {
        Long tenantId = tenantId();
        Page<AiModelCallLogEntity> page = callLogService.page(
                new Page<>(normalizePage(current), normalizeSize(size)),
                new LambdaQueryWrapper<AiModelCallLogEntity>()
                        .eq(AiModelCallLogEntity::getTenantId, tenantId)
                        .eq(modelConfigId != null,
                                AiModelCallLogEntity::getModelConfigId,
                                modelConfigId)
                        .eq(StringUtils.hasText(callSource),
                                AiModelCallLogEntity::getCallSource,
                                trim(callSource).toUpperCase(Locale.ROOT))
                        .eq(StringUtils.hasText(status),
                                AiModelCallLogEntity::getStatus,
                                trim(status).toUpperCase(Locale.ROOT))
                        .orderByDesc(AiModelCallLogEntity::getStartedAt)
                        .orderByDesc(AiModelCallLogEntity::getId)
        );
        return page.convert(this::toLogResponse);
    }

    private AiAgentModelEntity toEntity(
            ModelSaveRequest request,
            AiAgentModelEntity stored
    ) {
        if (request == null) {
            throw new IllegalArgumentException("模型配置不能为空");
        }
        AiAgentModelEntity model = stored == null
                ? new AiAgentModelEntity()
                : copyModel(stored);
        setIfNotNull(request.getConfigName(), model::setConfigName);
        setIfNotNull(request.getProviderName(), model::setProviderName);
        if (request.getProtocol() != null) {
            model.setProtocol(parseProtocol(request.getProtocol()));
        }
        setIfNotNull(request.getBaseURL(), model::setBaseURL);
        if (Boolean.TRUE.equals(request.getRemoveApiKey())) {
            model.setApiKey(null);
        } else {
            setIfNotNull(request.getApiKey(), model::setApiKey);
        }
        setIfNotNull(request.getDescription(), model::setDescription);
        setIfNotNull(request.getModelName(), model::setModelName);
        setIfNotNull(request.getStreaming(), model::setStreaming);
        setIfNotNull(request.getThinking(), model::setThinking);
        setIfNotNull(request.getTemperature(), model::setTemperature);
        setIfNotNull(request.getTopP(), model::setTopP);
        setIfNotNull(request.getMaxTokens(), model::setMaxTokens);
        setIfNotNull(request.getTimeoutMs(), model::setTimeoutMs);
        setIfNotNull(request.getThinkingBudget(), model::setThinkingBudget);
        setIfNotNull(request.getMaxAttempts(), model::setMaxAttempts);
        setIfNotNull(request.getStatus(), model::setStatus);

        if (stored == null) {
            model.setStreaming(defaultInt(model.getStreaming(), 1))
                    .setThinking(defaultInt(model.getThinking(), 0))
                    .setTemperature(defaultDecimal(model.getTemperature(), new BigDecimal("0.7")))
                    .setTopP(defaultDecimal(model.getTopP(), BigDecimal.ONE))
                    .setMaxTokens(defaultInt(model.getMaxTokens(), 4096))
                    .setTimeoutMs(model.getTimeoutMs() == null
                            ? DEFAULT_TIMEOUT_MS
                            : model.getTimeoutMs())
                    .setMaxAttempts(defaultInt(model.getMaxAttempts(), DEFAULT_MAX_ATTEMPTS))
                    .setStatus(defaultInt(model.getStatus(), ENABLED));
        }
        if (!Integer.valueOf(ENABLED).equals(model.getThinking())) {
            model.setThinkingBudget(null);
        }
        model.setFallbackModelConfigId(null);
        return model;
    }

    private void validateModel(AiAgentModelEntity model, Long ignoredId) {
        model.setConfigName(required(model.getConfigName(), "配置名称"));
        model.setProviderName(required(model.getProviderName(), "模型供应商"));
        model.setBaseURL(required(model.getBaseURL(), "Base URL"));
        model.setModelName(required(model.getModelName(), "模型名称"));
        if (model.getProtocol() == null) {
            throw new IllegalArgumentException("接口协议不能为空");
        }
        validateUrl(model.getBaseURL());
        validateFlag(model.getStreaming(), "流式输出");
        validateFlag(model.getThinking(), "思考模式");
        validateFlag(model.getStatus(), "状态");
        if (model.getTemperature() == null
                || model.getTemperature().compareTo(BigDecimal.ZERO) < 0
                || model.getTemperature().compareTo(new BigDecimal("2")) > 0) {
            throw new IllegalArgumentException("温度必须在 0 到 2 之间");
        }
        if (model.getTopP() == null
                || model.getTopP().compareTo(BigDecimal.ZERO) < 0
                || model.getTopP().compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Top P 必须在 0 到 1 之间");
        }
        if (model.getMaxTokens() == null || model.getMaxTokens() < 1) {
            throw new IllegalArgumentException("最大输出 Token 必须大于 0");
        }
        if (model.getTimeoutMs() == null
                || model.getTimeoutMs() < 1_000
                || model.getTimeoutMs() > 600_000) {
            throw new IllegalArgumentException("超时时间必须在 1000 到 600000 毫秒之间");
        }
        if (model.getMaxAttempts() == null
                || model.getMaxAttempts() < 1
                || model.getMaxAttempts() > 10) {
            throw new IllegalArgumentException("最大尝试次数必须在 1 到 10 之间");
        }
        if (Integer.valueOf(ENABLED).equals(model.getThinking())
                && model.getThinkingBudget() != null
                && model.getThinkingBudget() < 1) {
            throw new IllegalArgumentException("思考预算必须大于 0");
        }
        Long tenantId = tenantId();
        long duplicate = modelService.count(new LambdaQueryWrapper<AiAgentModelEntity>()
                .eq(AiAgentModelEntity::getTenantId, tenantId)
                .eq(AiAgentModelEntity::getConfigName, model.getConfigName())
                .ne(ignoredId != null, AiAgentModelEntity::getId, ignoredId));
        if (duplicate > 0) {
            throw new IllegalArgumentException("当前租户下配置名称已存在");
        }
    }

    private void syncHeaders(
            Long modelId,
            List<ModelHeaderInput> inputs,
            boolean create
    ) {
        if (inputs == null) {
            return;
        }
        Long tenantId = tenantId();
        List<AiHttpHeaderEntity> existing = headerService.getHeaderList(
                modelId,
                tenantId,
                HeaderSourceType.MODEL
        );
        Map<Long, AiHttpHeaderEntity> existingById = new HashMap<>();
        existing.forEach(header -> existingById.put(header.getId(), header));
        Set<String> names = new HashSet<>();

        for (ModelHeaderInput input : inputs) {
            if (input == null || Boolean.TRUE.equals(input.getRemove())) {
                continue;
            }
            String name = validateHeaderName(input.getHeaderName());
            String normalized = name.toLowerCase(Locale.ROOT);
            if (!names.add(normalized)) {
                throw new IllegalArgumentException("Header 名称不能重复（忽略大小写）");
            }
            if (input.getId() != null && !existingById.containsKey(input.getId())) {
                throw new IllegalArgumentException("Header 不属于当前模型配置");
            }
            if (input.getId() == null && !StringUtils.hasText(input.getHeaderValue())) {
                throw new IllegalArgumentException("Header 值不能为空");
            }
            if (input.getId() != null
                    && input.getHeaderValue() != null
                    && !StringUtils.hasText(input.getHeaderValue())) {
                throw new IllegalArgumentException("Header 值不能为空");
            }
            validateHeaderValue(input.getHeaderValue());
        }

        for (ModelHeaderInput input : inputs) {
            if (input == null) {
                continue;
            }
            if (Boolean.TRUE.equals(input.getRemove())) {
                if (input.getId() != null) {
                    AiHttpHeaderEntity existingHeader = existingById.get(input.getId());
                    if (existingHeader == null) {
                        throw new IllegalArgumentException("Header 不属于当前模型配置");
                    }
                    headerService.removeById(existingHeader.getId());
                }
                continue;
            }
            String name = validateHeaderName(input.getHeaderName());
            if (input.getId() == null) {
                AiHttpHeaderEntity header = new AiHttpHeaderEntity()
                        .setSourceId(modelId)
                        .setSource(HeaderSourceType.MODEL)
                        .setHeaderName(name)
                        .setHeaderValue(input.getHeaderValue());
                header.setTenantId(tenantId);
                headerService.save(EntityDefaults.create(header));
                continue;
            }
            AiHttpHeaderEntity header = existingById.get(input.getId())
                    .setHeaderName(name);
            if (input.getHeaderValue() != null) {
                header.setHeaderValue(input.getHeaderValue());
            }
            headerService.updateById(EntityDefaults.update(header));
        }

        if (create && !existing.isEmpty()) {
            throw new IllegalStateException("新建模型时不应存在历史 Header");
        }
    }

    private List<AiHttpHeaderEntity> testHeaders(
            ModelSaveRequest request,
            AiAgentModelEntity stored
    ) {
        if (request.getHeaders() == null) {
            if (stored == null) {
                return List.of();
            }
            return headerService.getHeaderList(
                    stored.getId(),
                    tenantId(),
                    HeaderSourceType.MODEL
            );
        }
        Set<String> names = new HashSet<>();
        List<AiHttpHeaderEntity> headers = new ArrayList<>();
        for (ModelHeaderInput input : request.getHeaders()) {
            if (input == null || Boolean.TRUE.equals(input.getRemove())) {
                continue;
            }
            String name = validateHeaderName(input.getHeaderName());
            if (!names.add(name.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Header 名称不能重复（忽略大小写）");
            }
            String value = input.getHeaderValue();
            if (value == null && input.getId() != null && stored != null) {
                value = headerService.getHeaderList(
                                stored.getId(),
                                tenantId(),
                                HeaderSourceType.MODEL
                        ).stream()
                        .filter(header -> Objects.equals(header.getId(), input.getId()))
                        .map(AiHttpHeaderEntity::getHeaderValue)
                        .findFirst()
                        .orElse(null);
            }
            if (!StringUtils.hasText(value)) {
                throw new IllegalArgumentException("Header 值不能为空");
            }
            validateHeaderValue(value);
            AiHttpHeaderEntity header = new AiHttpHeaderEntity()
                    .setSource(stored == null ? HeaderSourceType.MODEL : HeaderSourceType.MODEL)
                    .setSourceId(stored == null ? null : stored.getId())
                    .setHeaderName(name)
                    .setHeaderValue(value);
            header.setTenantId(tenantId());
            headers.add(header);
        }
        return headers;
    }

    private AgentConfigDTO toAgentConfig(AiAgentModelEntity model) {
        AgentConfigDTO config = new AgentConfigDTO();
        config.setTenantId(model.getTenantId());
        config.setModelId(model.getId());
        config.setModelConfigName(model.getConfigName());
        config.setProviderName(model.getProviderName());
        config.setProtocol(model.getProtocol());
        config.setBaseUrl(model.getBaseURL());
        config.setApiKey(model.getApiKey());
        config.setModelName(model.getModelName());
        config.setStreaming(Integer.valueOf(ENABLED).equals(model.getStreaming()));
        config.setThinking(Integer.valueOf(ENABLED).equals(model.getThinking()));
        config.setTemperature(model.getTemperature().doubleValue());
        config.setTopP(model.getTopP().doubleValue());
        config.setMaxTokens(model.getMaxTokens());
        config.setTimeoutMs(model.getTimeoutMs());
        config.setThinkingBudget(model.getThinkingBudget());
        config.setMaxAttempts(model.getMaxAttempts());
        config.setModelStatus(model.getStatus());
        return config;
    }

    private ModelListItemResponse toListItem(
            AiAgentModelEntity model,
            long todayCalls,
            AiModelCallLogEntity recentTest
    ) {
        return new ModelListItemResponse(
                model.getId(),
                model.getConfigName(),
                model.getProviderName(),
                model.getProtocol().getCode(),
                model.getDescription(),
                model.getModelName(),
                model.getStreaming(),
                model.getThinking(),
                model.getTemperature(),
                model.getTopP(),
                model.getMaxTokens(),
                model.getTimeoutMs(),
                model.getThinkingBudget(),
                model.getMaxAttempts(),
                model.getStatus(),
                todayCalls,
                recentTest == null ? null : recentTest.getStatus(),
                recentTest == null ? null : recentTest.getDurationMs(),
                recentTest == null ? null : recentTest.getStartedAt(),
                model.getUpdatedAt()
        );
    }

    private ModelCandidateResponse toCandidate(AiAgentModelEntity model) {
        return new ModelCandidateResponse(
                model.getId(),
                model.getConfigName(),
                model.getProviderName(),
                model.getProtocol().getCode(),
                model.getModelName(),
                model.getMaxTokens(),
                model.getStatus()
        );
    }

    private ModelCallLogResponse toLogResponse(AiModelCallLogEntity log) {
        return new ModelCallLogResponse(
                log.getId(),
                log.getModelConfigId(),
                log.getRunId(),
                log.getSessionId(),
                log.getAgentId(),
                log.getAgentConfigId(),
                log.getCallSource(),
                log.getSourcePath(),
                log.getStatus(),
                log.getConfigNameSnapshot(),
                log.getProviderNameSnapshot(),
                log.getProtocolSnapshot(),
                log.getModelNameSnapshot(),
                log.getInputTokens(),
                log.getOutputTokens(),
                log.getCachedTokens(),
                log.getTotalTokens(),
                log.getDurationMs(),
                log.getErrorCode(),
                log.getErrorMessage(),
                log.getStartedAt(),
                log.getEndedAt()
        );
    }

    private AiAgentModelEntity requireModel(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("模型配置 ID 不能为空");
        }
        AiAgentModelEntity model = modelService.getOne(
                new LambdaQueryWrapper<AiAgentModelEntity>()
                        .eq(AiAgentModelEntity::getTenantId, tenantId())
                        .eq(AiAgentModelEntity::getId, id)
        );
        if (model == null) {
            throw new IllegalArgumentException("模型配置不存在或已删除");
        }
        return model;
    }

    private AiAgentModelEntity copyModel(AiAgentModelEntity source) {
        AiAgentModelEntity target = new AiAgentModelEntity()
                .setId(source.getId())
                .setConfigName(source.getConfigName())
                .setProviderName(source.getProviderName())
                .setProtocol(source.getProtocol())
                .setBaseURL(source.getBaseURL())
                .setApiKey(source.getApiKey())
                .setDescription(source.getDescription())
                .setModelName(source.getModelName())
                .setStreaming(source.getStreaming())
                .setThinking(source.getThinking())
                .setTemperature(source.getTemperature())
                .setTopP(source.getTopP())
                .setMaxTokens(source.getMaxTokens())
                .setTimeoutMs(source.getTimeoutMs())
                .setThinkingBudget(source.getThinkingBudget())
                .setMaxAttempts(source.getMaxAttempts())
                .setFallbackModelConfigId(null)
                .setStatus(source.getStatus());
        target.setTenantId(source.getTenantId());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        target.setVersion(source.getVersion());
        return target;
    }

    private long countTodayCalls(Long modelId) {
        return callLogService.count(new LambdaQueryWrapper<AiModelCallLogEntity>()
                .eq(AiModelCallLogEntity::getTenantId, tenantId())
                .eq(AiModelCallLogEntity::getModelConfigId, modelId)
                .eq(AiModelCallLogEntity::getCallSource,
                        ModelCallAuditService.SOURCE_AGENT_RUN)
                .ge(AiModelCallLogEntity::getStartedAt, LocalDate.now().atStartOfDay()));
    }

    private AiModelCallLogEntity recentTest(Long modelId) {
        return callLogService.getOne(
                new LambdaQueryWrapper<AiModelCallLogEntity>()
                        .eq(AiModelCallLogEntity::getTenantId, tenantId())
                        .eq(AiModelCallLogEntity::getModelConfigId, modelId)
                        .eq(AiModelCallLogEntity::getCallSource,
                                ModelCallAuditService.SOURCE_MANUAL_TEST)
                        .orderByDesc(AiModelCallLogEntity::getStartedAt)
                        .last("LIMIT 1"),
                false
        );
    }

    private ModelCallSummary summary(
            Long tenantId,
            LocalDateTime start,
            LocalDateTime end
    ) {
        ModelCallSummary summary = callLogMapper.selectBusinessSummary(tenantId, start, end);
        return summary == null ? new ModelCallSummary() : summary;
    }

    private void collectResponse(
            ChatResponse response,
            AtomicReference<ChatUsage> usage,
            StringBuilder reply
    ) {
        if (response == null) {
            return;
        }
        if (response.getUsage() != null) {
            usage.set(response.getUsage());
        }
        if (response.getContent() == null) {
            return;
        }
        for (ContentBlock block : response.getContent()) {
            if (block instanceof TextBlock textBlock) {
                reply.append(textBlock.getText());
            }
        }
    }

    private Duration testWait(AiAgentModelEntity model) {
        long attempts = Math.max(1, model.getMaxAttempts());
        long timeout;
        try {
            timeout = Math.multiplyExact(model.getTimeoutMs(), attempts);
            timeout = Math.addExact(timeout, 5_000L);
        } catch (ArithmeticException ignored) {
            timeout = 605_000L;
        }
        return Duration.ofMillis(Math.min(timeout, 605_000L));
    }

    private List<String> secretValues(
            AiAgentModelEntity model,
            List<AiHttpHeaderEntity> headers
    ) {
        List<String> secrets = new ArrayList<>();
        if (StringUtils.hasText(model.getApiKey())) {
            secrets.add(model.getApiKey());
        }
        for (AiHttpHeaderEntity header : headers) {
            if (StringUtils.hasText(header.getHeaderValue())) {
                secrets.add(header.getHeaderValue());
            }
        }
        return List.copyOf(secrets);
    }

    private void validateUrl(String value) {
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            if (uri.getHost() == null
                    || (!"http".equalsIgnoreCase(scheme)
                    && !"https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("Base URL 必须是合法的 HTTP 或 HTTPS 地址");
        }
    }

    private String validateHeaderName(String value) {
        String name = required(value, "Header 名称");
        if (!name.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+")) {
            throw new IllegalArgumentException("Header 名称格式不合法");
        }
        return name;
    }

    private void validateHeaderValue(String value) {
        if (value != null && (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0)) {
            throw new IllegalArgumentException("Header 值不能包含换行符");
        }
    }

    private void validateFlag(Integer value, String label) {
        if (value == null || (value != 0 && value != 1)) {
            throw new IllegalArgumentException(label + "只能为 0 或 1");
        }
    }

    private String required(String value, String label) {
        String result = trim(value);
        if (!StringUtils.hasText(result)) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        return result;
    }

    private ModelProtocol parseProtocol(String protocol) {
        return ModelProtocol.fromCode(protocol);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private UserInfo currentUser() {
        UserInfo user = UserContext.get();
        if (user == null || user.getTenantId() == null) {
            throw new IllegalStateException("缺少已认证的租户上下文");
        }
        return user;
    }

    private Long tenantId() {
        return currentUser().getTenantId();
    }

    private long normalizePage(long current) {
        return Math.max(current, 1L);
    }

    private long normalizeSize(long size) {
        return Math.min(Math.max(size, 1L), 100L);
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }

    private Double percentChange(long current, long previous) {
        if (previous == 0) {
            return current == 0 ? 0D : 100D;
        }
        return round((current - previous) * 100D / previous);
    }

    private double round(double value) {
        return Math.round(value * 10D) / 10D;
    }

    private long elapsedMs(long startedNanos) {
        return Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
    }

    private String safeMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() == null
                ? current.getClass().getSimpleName()
                : current.getMessage();
    }

    private String redact(String value, List<String> secrets) {
        String result = value == null ? "" : value;
        for (String secret : secrets) {
            if (StringUtils.hasText(secret)) {
                result = result.replace(secret, "***");
            }
        }
        return result;
    }

    private String limit(String value, int length) {
        if (value == null || value.length() <= length) {
            return value;
        }
        return value.substring(0, length);
    }

    private int defaultInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private BigDecimal defaultDecimal(BigDecimal value, BigDecimal defaultValue) {
        return value == null ? defaultValue : value;
    }

    private <T> void setIfNotNull(T value, Consumer<T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }

    private void invalidateAgentsAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            agentRuntimeFactory.invalidateAllAgents();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        agentRuntimeFactory.invalidateAllAgents();
                    }
                }
        );
    }
}
