package com.zw.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.constant.enumeration.HeaderSourceType;
import com.zw.agent.entity.AiAgentEntity;
import com.zw.agent.entity.AiHttpHeaderEntity;
import com.zw.agent.entity.AiSubagentAgentBindingEntity;
import com.zw.agent.entity.AiSubagentEntity;
import com.zw.agent.entity.AiSubagentInstanceEntity;
import com.zw.agent.entity.AiSubagentTaskEntity;
import com.zw.agent.entity.DTO.SubagentAgentOptionResponse;
import com.zw.agent.entity.DTO.SubagentHeaderInput;
import com.zw.agent.entity.DTO.SubagentListItemResponse;
import com.zw.agent.entity.DTO.SubagentMetricsResponse;
import com.zw.agent.entity.DTO.SubagentSaveRequest;
import com.zw.agent.entity.DTO.SubagentTaskResponse;
import com.zw.agent.mapper.AiHttpHeaderMapper;
import com.zw.agent.mapper.AiSubagentMapper;
import com.zw.common.context.UserContext;
import com.zw.common.context.UserInfo;
import com.zw.common.support.EntityDefaults;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SubagentManagementService {

    private static final byte LOCAL = 1;
    private static final byte REMOTE = 2;
    private static final byte AGENT_PROTOCOL = 1;
    private static final Set<String> FINISHED_STATUSES =
            Set.of("COMPLETED", "FAILED", "CANCELLED", "TIMEOUT");
    private static final Set<String> EXCEPTION_STATUSES =
            Set.of("FAILED", "CANCELLED", "TIMEOUT");
    private static final int TASK_SUMMARY_LIMIT = 240;

    private final AiSubagentService subagentService;
    private final AiAgentService agentService;
    private final AiHttpHeaderService headerService;
    private final AiSubagentTaskService taskService;
    private final AiSubagentInstanceService instanceService;
    private final AiSubagentAgentBindingService bindingService;
    private final AiSubagentMapper subagentMapper;
    private final AiHttpHeaderMapper headerMapper;

    public SubagentMetricsResponse metrics() {
        Long tenantId = currentTenantId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime yesterdaySameTime = now.minusDays(1);

        long total = subagentService.count(new LambdaQueryWrapper<AiSubagentEntity>()
                .eq(AiSubagentEntity::getTenantId, tenantId));
        long enabled = subagentService.count(new LambdaQueryWrapper<AiSubagentEntity>()
                .eq(AiSubagentEntity::getTenantId, tenantId)
                .eq(AiSubagentEntity::getEnabled, (byte) 1));
        List<AiSubagentTaskEntity> todayTasks = tasksBetween(tenantId, todayStart, now);
        long yesterdayDelegations = taskService.count(new LambdaQueryWrapper<AiSubagentTaskEntity>()
                .eq(AiSubagentTaskEntity::getTenantId, tenantId)
                .ge(AiSubagentTaskEntity::getStartedAt, yesterdayStart)
                .lt(AiSubagentTaskEntity::getStartedAt, yesterdaySameTime));
        long completed = todayTasks.stream()
                .filter(task -> "COMPLETED".equals(normalizeStatus(task.getStatus())))
                .count();
        long unsuccessful = todayTasks.stream()
                .filter(task -> EXCEPTION_STATUSES.contains(normalizeStatus(task.getStatus())))
                .count();
        long finished = completed + unsuccessful;
        List<Long> durations = todayTasks.stream()
                .filter(task -> FINISHED_STATUSES.contains(normalizeStatus(task.getStatus())))
                .map(AiSubagentTaskEntity::getDurationMs)
                .filter(Objects::nonNull)
                .toList();
        Double averageDuration = durations.isEmpty()
                ? null
                : durations.stream().mapToLong(Long::longValue).average().orElse(0);
        Double change = yesterdayDelegations == 0
                ? null
                : percentage(todayTasks.size() - yesterdayDelegations, yesterdayDelegations);
        Double successRate = finished == 0 ? null : percentage(completed, finished);
        return new SubagentMetricsResponse(
                total,
                enabled,
                todayTasks.size(),
                change,
                successRate,
                unsuccessful,
                averageDuration
        );
    }

    public IPage<SubagentListItemResponse> page(
            long current,
            long size,
            String keyword,
            Byte sourceType,
            Byte enabled,
            Boolean sourceAvailable
    ) {
        Long tenantId = currentTenantId();
        List<AiAgentEntity> activeAgents = agentService.list(new LambdaQueryWrapper<AiAgentEntity>()
                .eq(AiAgentEntity::getTenantId, tenantId)
                .eq(AiAgentEntity::getStatus, 1));
        Set<Long> activeAgentIds = activeAgents.stream().map(AiAgentEntity::getId).collect(Collectors.toSet());

        LambdaQueryWrapper<AiSubagentEntity> query = new LambdaQueryWrapper<AiSubagentEntity>()
                .eq(AiSubagentEntity::getTenantId, tenantId);
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            query.and(wrapper -> wrapper
                    .like(AiSubagentEntity::getSubagentName, value)
                    .or()
                    .like(AiSubagentEntity::getSubagentCode, value)
                    .or()
                    .like(AiSubagentEntity::getDescription, value));
        }
        if (sourceType != null) {
            query.eq(AiSubagentEntity::getSourceType, sourceType);
        }
        if (enabled != null) {
            query.eq(AiSubagentEntity::getEnabled, enabled);
        }
        if (sourceAvailable != null) {
            applyAvailabilityFilter(query, sourceAvailable, activeAgentIds);
        }
        query.orderByDesc(AiSubagentEntity::getUpdatedAt)
                .orderByDesc(AiSubagentEntity::getCreatedAt);

        IPage<AiSubagentEntity> entityPage =
                subagentService.page(new Page<>(sanitizePage(current), sanitizeSize(size)), query);
        List<SubagentListItemResponse> records =
                toListItems(entityPage.getRecords(), tenantId, activeAgents);
        Page<SubagentListItemResponse> response =
                new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        response.setRecords(records);
        return response;
    }

    public List<SubagentAgentOptionResponse> localAgentOptions() {
        Long tenantId = currentTenantId();
        Set<Long> registeredIds = subagentService.list(new LambdaQueryWrapper<AiSubagentEntity>()
                        .eq(AiSubagentEntity::getTenantId, tenantId)
                        .eq(AiSubagentEntity::getSourceType, LOCAL)
                        .isNotNull(AiSubagentEntity::getLocalAgentId))
                .stream()
                .map(AiSubagentEntity::getLocalAgentId)
                .collect(Collectors.toSet());
        LambdaQueryWrapper<AiAgentEntity> query = new LambdaQueryWrapper<AiAgentEntity>()
                .eq(AiAgentEntity::getTenantId, tenantId)
                .eq(AiAgentEntity::getStatus, 1);
        if (!registeredIds.isEmpty()) {
            query.notIn(AiAgentEntity::getId, registeredIds);
        }
        query.orderByAsc(AiAgentEntity::getAgentName);
        return agentService.list(query).stream()
                .map(agent -> new SubagentAgentOptionResponse(
                        agent.getId(),
                        agent.getAgentName(),
                        agent.getDescription()
                ))
                .toList();
    }

    public List<SubagentTaskResponse> recentTasks(int limit, boolean exceptionsOnly) {
        Long tenantId = currentTenantId();
        LambdaQueryWrapper<AiSubagentTaskEntity> query =
                new LambdaQueryWrapper<AiSubagentTaskEntity>()
                        .eq(AiSubagentTaskEntity::getTenantId, tenantId);
        if (exceptionsOnly) {
            query.in(AiSubagentTaskEntity::getStatus, EXCEPTION_STATUSES);
        }
        query.orderByDesc(AiSubagentTaskEntity::getStartedAt)
                .orderByDesc(AiSubagentTaskEntity::getCreatedAt)
                .last("LIMIT " + Math.min(50, Math.max(1, limit)));
        return toTaskResponses(taskService.list(query), tenantId);
    }

    @Transactional
    public SubagentListItemResponse create(SubagentSaveRequest request) {
        Long tenantId = currentTenantId();
        validateCommon(request, tenantId, null);
        AiSubagentEntity entity = new AiSubagentEntity();
        applyMutableFields(entity, request);
        entity.setTenantId(tenantId);
        entity.setSourceType(request.getSourceType());
        if (request.getSourceType() == LOCAL) {
            AiAgentEntity source = requireAvailableAgent(request.getLocalAgentId(), tenantId);
            ensureLocalAgentNotRegistered(source.getId(), tenantId, null);
            entity.setLocalAgentId(source.getId());
            entity.setRemoteUrl(null);
            entity.setProtocolType(null);
        } else {
            entity.setLocalAgentId(null);
            entity.setRemoteUrl(validateRemoteUrl(request.getRemoteUrl()));
            entity.setProtocolType(AGENT_PROTOCOL);
        }
        subagentService.save(EntityDefaults.create(entity));
        if (entity.getSourceType() == REMOTE) {
            syncHeaders(entity.getId(), tenantId, request.getHeaders());
        }
        return toListItems(List.of(entity), tenantId, availableAgents(tenantId)).getFirst();
    }

    @Transactional
    public SubagentListItemResponse update(SubagentSaveRequest request) {
        if (request == null || request.getId() == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        Long tenantId = currentTenantId();
        AiSubagentEntity existing = requireSubagent(request.getId(), tenantId);
        validateCommon(request, tenantId, existing.getId());
        if (request.getSourceType() == null || !request.getSourceType().equals(existing.getSourceType())) {
            throw new IllegalArgumentException("sourceType cannot be changed");
        }
        if (existing.getSourceType() == LOCAL
                && !Objects.equals(existing.getLocalAgentId(), request.getLocalAgentId())) {
            throw new IllegalArgumentException("localAgentId cannot be changed");
        }
        applyMutableFields(existing, request);
        if (existing.getSourceType() == REMOTE) {
            existing.setRemoteUrl(validateRemoteUrl(request.getRemoteUrl()));
            existing.setProtocolType(AGENT_PROTOCOL);
        }
        subagentService.updateById(EntityDefaults.update(existing));
        if (existing.getSourceType() == REMOTE) {
            syncHeaders(existing.getId(), tenantId, request.getHeaders());
        }
        return toListItems(List.of(existing), tenantId, availableAgents(tenantId)).getFirst();
    }

    @Transactional
    public boolean delete(Long id) {
        Long tenantId = currentTenantId();
        AiSubagentEntity existing = requireSubagent(id, tenantId);
        long bindingCount = bindingService.count(new LambdaQueryWrapper<AiSubagentAgentBindingEntity>()
                .eq(AiSubagentAgentBindingEntity::getTenantId, tenantId)
                .eq(AiSubagentAgentBindingEntity::getSubagentId, id));
        long taskCount = taskService.count(new LambdaQueryWrapper<AiSubagentTaskEntity>()
                .eq(AiSubagentTaskEntity::getTenantId, tenantId)
                .eq(AiSubagentTaskEntity::getSubagentId, id));
        long instanceCount = instanceService.count(new LambdaQueryWrapper<AiSubagentInstanceEntity>()
                .eq(AiSubagentInstanceEntity::getTenantId, tenantId)
                .eq(AiSubagentInstanceEntity::getSubagentId, id));
        if (bindingCount > 0 || taskCount > 0 || instanceCount > 0) {
            existing.setEnabled((byte) 0);
            subagentService.updateById(EntityDefaults.update(existing));
            bindingService.update(new LambdaUpdateWrapper<AiSubagentAgentBindingEntity>()
                    .eq(AiSubagentAgentBindingEntity::getTenantId, tenantId)
                    .eq(AiSubagentAgentBindingEntity::getSubagentId, id)
                    .set(AiSubagentAgentBindingEntity::getEnabled, (byte) 0));
            return subagentService.removeById(id);
        }
        headerMapper.hardDeleteRemoteSubagentHeaders(id, tenantId);
        return subagentMapper.hardDeleteById(id, tenantId) > 0;
    }

    private void validateCommon(SubagentSaveRequest request, Long tenantId, Long currentId) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (!StringUtils.hasText(request.getSubagentCode())
                || !StringUtils.hasText(request.getSubagentName())
                || !StringUtils.hasText(request.getDescription())) {
            throw new IllegalArgumentException("subagentCode, subagentName and description must not be blank");
        }
        if (request.getSourceType() == null
                || (request.getSourceType() != LOCAL && request.getSourceType() != REMOTE)) {
            throw new IllegalArgumentException("sourceType must be 1 (local) or 2 (remote)");
        }
        if (request.getEnabled() != null
                && request.getEnabled() != (byte) 0
                && request.getEnabled() != (byte) 1) {
            throw new IllegalArgumentException("enabled must be 0 or 1");
        }
        LambdaQueryWrapper<AiSubagentEntity> duplicate = new LambdaQueryWrapper<AiSubagentEntity>()
                .eq(AiSubagentEntity::getTenantId, tenantId)
                .apply("LOWER(subagent_code) = {0}", request.getSubagentCode().trim().toLowerCase(Locale.ROOT));
        if (currentId != null) {
            duplicate.ne(AiSubagentEntity::getId, currentId);
        }
        if (subagentService.count(duplicate) > 0) {
            throw new IllegalArgumentException("subagentCode already exists in the current tenant");
        }
    }

    private void applyMutableFields(AiSubagentEntity entity, SubagentSaveRequest request) {
        entity.setSubagentCode(request.getSubagentCode().trim());
        entity.setSubagentName(request.getSubagentName().trim());
        entity.setDescription(request.getDescription().trim());
        entity.setEnabled(request.getEnabled() == null ? (byte) 1 : request.getEnabled());
        entity.setRemark(trimToNull(request.getRemark()));
    }

    private AiSubagentEntity requireSubagent(Long id, Long tenantId) {
        AiSubagentEntity entity = subagentService.getOne(
                new LambdaQueryWrapper<AiSubagentEntity>()
                        .eq(AiSubagentEntity::getTenantId, tenantId)
                        .eq(AiSubagentEntity::getId, id),
                false
        );
        if (entity == null) {
            throw new IllegalArgumentException("Subagent does not exist in the current tenant");
        }
        return entity;
    }

    private AiAgentEntity requireAvailableAgent(Long id, Long tenantId) {
        if (id == null) {
            throw new IllegalArgumentException("localAgentId must not be null for a local subagent");
        }
        AiAgentEntity agent = agentService.getOne(new LambdaQueryWrapper<AiAgentEntity>()
                .eq(AiAgentEntity::getTenantId, tenantId)
                .eq(AiAgentEntity::getId, id)
                .eq(AiAgentEntity::getStatus, 1), false);
        if (agent == null) {
            throw new IllegalArgumentException("The selected local agent is not available");
        }
        return agent;
    }

    private void ensureLocalAgentNotRegistered(Long agentId, Long tenantId, Long currentId) {
        LambdaQueryWrapper<AiSubagentEntity> query = new LambdaQueryWrapper<AiSubagentEntity>()
                .eq(AiSubagentEntity::getTenantId, tenantId)
                .eq(AiSubagentEntity::getSourceType, LOCAL)
                .eq(AiSubagentEntity::getLocalAgentId, agentId);
        if (currentId != null) {
            query.ne(AiSubagentEntity::getId, currentId);
        }
        if (subagentService.count(query) > 0) {
            throw new IllegalArgumentException("The selected agent is already registered as a local subagent");
        }
    }

    private String validateRemoteUrl(String value) {
        try {
            URI uri = URI.create(value == null ? "" : value.trim());
            String scheme = uri.getScheme();
            if (uri.getHost() == null
                    || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException();
            }
            return uri.toString();
        } catch (Exception exception) {
            throw new IllegalArgumentException("remoteUrl must be a valid HTTP or HTTPS URL");
        }
    }

    private void syncHeaders(Long subagentId, Long tenantId, List<SubagentHeaderInput> inputs) {
        List<AiHttpHeaderEntity> existing = headerService.list(new LambdaQueryWrapper<AiHttpHeaderEntity>()
                .eq(AiHttpHeaderEntity::getTenantId, tenantId)
                .eq(AiHttpHeaderEntity::getSourceId, subagentId)
                .eq(AiHttpHeaderEntity::getSource, HeaderSourceType.REMOTE_SUB_AGENT));
        Map<Long, AiHttpHeaderEntity> existingById = existing.stream()
                .collect(Collectors.toMap(AiHttpHeaderEntity::getId, Function.identity()));
        List<SubagentHeaderInput> submitted =
                inputs == null ? List.of() : inputs.stream().filter(Objects::nonNull).toList();
        Set<Long> submittedIds = submitted.stream()
                .map(SubagentHeaderInput::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<String> names = existing.stream()
                .filter(header -> !submittedIds.contains(header.getId()))
                .map(header -> header.getHeaderName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(HashSet::new));
        for (SubagentHeaderInput input : submitted) {
            if (Boolean.TRUE.equals(input.getRemove())) {
                AiHttpHeaderEntity target = existingById.get(input.getId());
                if (target != null) {
                    headerService.removeById(target.getId());
                }
                continue;
            }
            String name = validateHeaderName(input.getHeaderName());
            if (!names.add(name.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Header names must be unique");
            }
            AiHttpHeaderEntity target = input.getId() == null ? null : existingById.get(input.getId());
            if (input.getId() != null && target == null) {
                throw new IllegalArgumentException("Header does not belong to this subagent");
            }
            if (target == null) {
                if (!StringUtils.hasText(input.getHeaderValue())) {
                    throw new IllegalArgumentException("A new Header must have a value");
                }
                target = new AiHttpHeaderEntity()
                        .setSourceId(subagentId)
                        .setSource(HeaderSourceType.REMOTE_SUB_AGENT)
                        .setHeaderName(name)
                        .setHeaderValue(input.getHeaderValue());
                target.setTenantId(tenantId);
                headerService.save(EntityDefaults.create(target));
            } else {
                target.setHeaderName(name);
                if (StringUtils.hasText(input.getHeaderValue())) {
                    target.setHeaderValue(input.getHeaderValue());
                }
                headerService.updateById(EntityDefaults.update(target));
            }
        }
    }

    private String validateHeaderName(String value) {
        String name = value == null ? "" : value.trim();
        if (name.isEmpty() || name.contains(":") || name.contains("\r") || name.contains("\n")) {
            throw new IllegalArgumentException("Header name is invalid");
        }
        return name;
    }

    private List<SubagentListItemResponse> toListItems(
            List<AiSubagentEntity> entities,
            Long tenantId,
            List<AiAgentEntity> activeAgents
    ) {
        if (entities.isEmpty()) {
            return List.of();
        }
        List<Long> ids = entities.stream().map(AiSubagentEntity::getId).toList();
        Map<Long, AiAgentEntity> activeAgentMap = activeAgents.stream()
                .collect(Collectors.toMap(AiAgentEntity::getId, Function.identity()));
        Map<Long, List<AiHttpHeaderEntity>> headers = headerService.list(
                        new LambdaQueryWrapper<AiHttpHeaderEntity>()
                                .eq(AiHttpHeaderEntity::getTenantId, tenantId)
                                .in(AiHttpHeaderEntity::getSourceId, ids)
                                .eq(AiHttpHeaderEntity::getSource, HeaderSourceType.REMOTE_SUB_AGENT))
                .stream()
                .collect(Collectors.groupingBy(AiHttpHeaderEntity::getSourceId));
        Map<Long, Long> parentCounts = bindingService.list(
                        new LambdaQueryWrapper<AiSubagentAgentBindingEntity>()
                                .eq(AiSubagentAgentBindingEntity::getTenantId, tenantId)
                                .in(AiSubagentAgentBindingEntity::getSubagentId, ids)
                                .eq(AiSubagentAgentBindingEntity::getEnabled, (byte) 1))
                .stream()
                .collect(Collectors.groupingBy(
                        AiSubagentAgentBindingEntity::getSubagentId,
                        Collectors.mapping(
                                AiSubagentAgentBindingEntity::getAgentId,
                                Collectors.collectingAndThen(Collectors.toSet(), set -> (long) set.size())
                        )
                ));
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<AiSubagentTaskEntity> todayTasks = taskService.list(
                new LambdaQueryWrapper<AiSubagentTaskEntity>()
                        .eq(AiSubagentTaskEntity::getTenantId, tenantId)
                        .in(AiSubagentTaskEntity::getSubagentId, ids)
                        .ge(AiSubagentTaskEntity::getStartedAt, todayStart)
                        .lt(AiSubagentTaskEntity::getStartedAt, LocalDateTime.now()));
        Map<Long, List<AiSubagentTaskEntity>> tasksBySubagent =
                todayTasks.stream().filter(task -> task.getSubagentId() != null)
                        .collect(Collectors.groupingBy(AiSubagentTaskEntity::getSubagentId));

        return entities.stream().map(entity -> {
            AiAgentEntity source = activeAgentMap.get(entity.getLocalAgentId());
            boolean available = entity.getSourceType() == REMOTE || source != null;
            String sourceName = entity.getSourceType() == REMOTE
                    ? "Agent Protocol"
                    : source == null ? "来源不可用" : source.getAgentName();
            List<AiSubagentTaskEntity> subagentTasks =
                    tasksBySubagent.getOrDefault(entity.getId(), List.of());
            long completed = subagentTasks.stream()
                    .filter(task -> "COMPLETED".equals(normalizeStatus(task.getStatus())))
                    .count();
            long finished = subagentTasks.stream()
                    .filter(task -> FINISHED_STATUSES.contains(normalizeStatus(task.getStatus())))
                    .count();
            Double successRate = finished == 0 ? null : percentage(completed, finished);
            List<SubagentListItemResponse.HeaderSummary> headerSummaries =
                    headers.getOrDefault(entity.getId(), List.of()).stream()
                            .map(header -> new SubagentListItemResponse.HeaderSummary(
                                    header.getId(),
                                    header.getHeaderName(),
                                    StringUtils.hasText(header.getHeaderValue())
                            ))
                            .toList();
            return new SubagentListItemResponse(
                    entity.getId(),
                    entity.getSubagentCode(),
                    entity.getSubagentName(),
                    entity.getDescription(),
                    entity.getSourceType(),
                    entity.getLocalAgentId(),
                    entity.getRemoteUrl(),
                    entity.getProtocolType(),
                    entity.getEnabled(),
                    entity.getRemark(),
                    sourceName,
                    available,
                    subagentTasks.size(),
                    parentCounts.getOrDefault(entity.getId(), 0L),
                    successRate,
                    headerSummaries,
                    entity.getUpdatedAt()
            );
        }).toList();
    }

    private List<SubagentTaskResponse> toTaskResponses(
            List<AiSubagentTaskEntity> tasks,
            Long tenantId
    ) {
        Set<Long> subagentIds = tasks.stream()
                .map(AiSubagentTaskEntity::getSubagentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> agentIds = tasks.stream()
                .map(AiSubagentTaskEntity::getParentAgentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> subagentNames = subagentIds.isEmpty()
                ? Map.of()
                : subagentMapper.selectNamesIncludingDeleted(tenantId, subagentIds)
                        .stream()
                        .collect(Collectors.toMap(AiSubagentEntity::getId, AiSubagentEntity::getSubagentName));
        Map<Long, String> agentNames = agentIds.isEmpty()
                ? Map.of()
                : agentService.list(new LambdaQueryWrapper<AiAgentEntity>()
                                .eq(AiAgentEntity::getTenantId, tenantId)
                                .in(AiAgentEntity::getId, agentIds))
                        .stream()
                        .collect(Collectors.toMap(AiAgentEntity::getId, AiAgentEntity::getAgentName));
        return tasks.stream()
                .map(task -> new SubagentTaskResponse(
                        task.getId(),
                        task.getSubagentId(),
                        subagentNames.getOrDefault(task.getSubagentId(), task.getSubagentKey()),
                        task.getParentAgentId(),
                        agentNames.getOrDefault(task.getParentAgentId(), "智能体 #" + task.getParentAgentId()),
                        truncate(task.getTaskInput()),
                        normalizeStatus(task.getStatus()),
                        task.getStartedAt(),
                        task.getDurationMs(),
                        truncate(task.getErrorMessage())
                ))
                .toList();
    }

    private void applyAvailabilityFilter(
            LambdaQueryWrapper<AiSubagentEntity> query,
            boolean available,
            Collection<Long> activeAgentIds
    ) {
        if (available) {
            query.and(wrapper -> {
                wrapper.eq(AiSubagentEntity::getSourceType, REMOTE);
                if (!activeAgentIds.isEmpty()) {
                    wrapper.or(nested -> nested
                            .eq(AiSubagentEntity::getSourceType, LOCAL)
                            .in(AiSubagentEntity::getLocalAgentId, activeAgentIds));
                }
            });
        } else {
            query.eq(AiSubagentEntity::getSourceType, LOCAL)
                    .and(wrapper -> {
                        wrapper.isNull(AiSubagentEntity::getLocalAgentId);
                        if (!activeAgentIds.isEmpty()) {
                            wrapper.or().notIn(AiSubagentEntity::getLocalAgentId, activeAgentIds);
                        }
                    });
        }
    }

    private List<AiSubagentTaskEntity> tasksBetween(
            Long tenantId,
            LocalDateTime start,
            LocalDateTime end
    ) {
        return taskService.list(new LambdaQueryWrapper<AiSubagentTaskEntity>()
                .eq(AiSubagentTaskEntity::getTenantId, tenantId)
                .ge(AiSubagentTaskEntity::getStartedAt, start)
                .lt(AiSubagentTaskEntity::getStartedAt, end));
    }

    private List<AiAgentEntity> availableAgents(Long tenantId) {
        return agentService.list(new LambdaQueryWrapper<AiAgentEntity>()
                .eq(AiAgentEntity::getTenantId, tenantId)
                .eq(AiAgentEntity::getStatus, 1));
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }

    private Double percentage(long numerator, long denominator) {
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String truncate(String value) {
        if (value == null || value.length() <= TASK_SUMMARY_LIMIT) {
            return value;
        }
        return value.substring(0, TASK_SUMMARY_LIMIT) + "…";
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private long sanitizePage(long value) {
        return Math.max(1, value);
    }

    private long sanitizeSize(long value) {
        return Math.min(100, Math.max(1, value));
    }

    private Long currentTenantId() {
        UserInfo userInfo = UserContext.get();
        if (userInfo == null || userInfo.getTenantId() == null) {
            throw new IllegalStateException("Authenticated tenant context is required");
        }
        return userInfo.getTenantId();
    }
}
