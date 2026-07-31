package com.zw.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.constant.enumeration.StateStoreType;
import com.zw.agent.entity.AiAgentConfigEntity;
import com.zw.agent.entity.AiAgentEntity;
import com.zw.agent.entity.AiAgentModelEntity;
import com.zw.agent.entity.AiAgentRunLogEntity;
import com.zw.agent.entity.AiAgentSysPromptEntity;
import com.zw.agent.entity.AiAgentToolEntity;
import com.zw.agent.entity.AiKnowledgeAgentBindingEntity;
import com.zw.agent.entity.AiKnowledgeBaseEntity;
import com.zw.agent.entity.AiSkillAgentBindingEntity;
import com.zw.agent.entity.AiSkillInfoEntity;
import com.zw.agent.entity.AiSubagentAgentBindingEntity;
import com.zw.agent.entity.AiSubagentEntity;
import com.zw.agent.entity.AiToolInfoConfigEntity;
import com.zw.agent.entity.DTO.AgentBoundResourceResponse;
import com.zw.agent.entity.DTO.AgentDetailResponse;
import com.zw.agent.entity.DTO.AgentListItemResponse;
import com.zw.agent.entity.DTO.AgentMetricsResponse;
import com.zw.agent.entity.DTO.AgentRunLogResponse;
import com.zw.agent.entity.DTO.AgentRunSummary;
import com.zw.agent.entity.DTO.AgentSaveRequest;
import com.zw.agent.factory.agentFactory.AgentRuntimeFactory;
import com.zw.agent.mapper.AiAgentConfigMapper;
import com.zw.agent.mapper.AiAgentMapper;
import com.zw.agent.mapper.AiAgentModelMapper;
import com.zw.agent.mapper.AiAgentRunLogMapper;
import com.zw.agent.mapper.AiAgentSysPromptMapper;
import com.zw.agent.mapper.AiAgentToolMapper;
import com.zw.agent.mapper.AiKnowledgeAgentBindingMapper;
import com.zw.agent.mapper.AiKnowledgeBaseMapper;
import com.zw.agent.mapper.AiModelCallLogMapper;
import com.zw.agent.mapper.AiSkillAgentBindingMapper;
import com.zw.agent.mapper.AiSkillInfoMapper;
import com.zw.agent.mapper.AiSubagentAgentBindingMapper;
import com.zw.agent.mapper.AiSubagentMapper;
import com.zw.agent.mapper.AiToolInfoConfigMapper;
import com.zw.common.context.UserContext;
import com.zw.common.context.UserInfo;
import com.zw.common.support.EntityDefaults;
import io.agentscope.core.permission.PermissionMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AgentManagementService {

    private static final String AGENT_TYPE = "HARNESS";
    private static final String DEFAULT_PERMISSION_MODE = "DEFAULT";
    private static final Set<String> RUN_STATUSES =
            Set.of("RUNNING", "SUCCESS", "FAILED", "CANCELLED");
    private static final String AGENT_CODE_PATTERN =
            "^[a-z0-9](?:[a-z0-9-]{0,62}[a-z0-9])$";

    private final AiAgentMapper agentMapper;
    private final AiAgentConfigMapper configMapper;
    private final AiAgentModelMapper modelMapper;
    private final AiAgentSysPromptMapper promptMapper;
    private final AiAgentToolMapper agentToolMapper;
    private final AiToolInfoConfigMapper toolMapper;
    private final AiSkillAgentBindingMapper skillBindingMapper;
    private final AiSkillInfoMapper skillMapper;
    private final AiKnowledgeAgentBindingMapper knowledgeBindingMapper;
    private final AiKnowledgeBaseMapper knowledgeBaseMapper;
    private final AiSubagentAgentBindingMapper subagentBindingMapper;
    private final AiSubagentMapper subagentMapper;
    private final AiAgentRunLogMapper runLogMapper;
    private final AiModelCallLogMapper modelCallLogMapper;
    private final RedisConnectionFactory redisConnectionFactory;
    private final AgentRuntimeFactory agentRuntimeFactory;

    public AgentMetricsResponse metrics() {
        Long tenantId = tenantId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime yesterdaySameTime = now.minusDays(1);

        long total = agentMapper.selectCount(activeAgents(tenantId));
        long newToday = agentMapper.selectCount(activeAgents(tenantId)
                .ge(AiAgentEntity::getCreatedAt, todayStart)
                .lt(AiAgentEntity::getCreatedAt, now.plusNanos(1)));
        long todayTokens = value(modelCallLogMapper.sumAgentRunTokens(
                tenantId, todayStart, now.plusNanos(1)));
        long yesterdayTokens = value(modelCallLogMapper.sumAgentRunTokens(
                tenantId, yesterdayStart, yesterdaySameTime.plusNanos(1)));
        AgentRunSummary today = summary(tenantId, todayStart, now.plusNanos(1), null);
        AgentRunSummary yesterday = summary(
                tenantId, yesterdayStart, yesterdaySameTime.plusNanos(1), null);

        return new AgentMetricsResponse(
                total,
                newToday,
                todayTokens,
                percentChange(todayTokens, yesterdayTokens),
                value(today.getTotalRuns()),
                percentChange(value(today.getTotalRuns()), value(yesterday.getTotalRuns())),
                successRate(today),
                difference(successRate(today), successRate(yesterday)),
                round(today.getAverageDurationMs())
        );
    }

    public IPage<AgentListItemResponse> page(
            long current,
            long size,
            String keyword
    ) {
        validatePage(current, size);
        Long tenantId = tenantId();
        LambdaQueryWrapper<AiAgentEntity> query = activeAgents(tenantId);
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            query.and(wrapper -> wrapper
                    .like(AiAgentEntity::getAgentCode, value)
                    .or()
                    .like(AiAgentEntity::getAgentName, value)
                    .or()
                    .like(AiAgentEntity::getDescription, value));
        }
        query.orderByDesc(AiAgentEntity::getUpdatedAt)
                .orderByDesc(AiAgentEntity::getId);
        IPage<AiAgentEntity> source = agentMapper.selectPage(
                new Page<>(current, Math.min(size, 100)),
                query
        );
        List<AiAgentEntity> agents = source.getRecords();
        Page<AgentListItemResponse> result =
                new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        if (agents.isEmpty()) {
            result.setRecords(List.of());
            return result;
        }

        List<Long> agentIds = agents.stream().map(AiAgentEntity::getId).toList();
        Map<Long, AiAgentConfigEntity> configs = configMapper.selectList(
                        new LambdaQueryWrapper<AiAgentConfigEntity>()
                                .eq(AiAgentConfigEntity::getTenantId, tenantId)
                                .eq(AiAgentConfigEntity::getDeleted, 0)
                                .in(AiAgentConfigEntity::getAgentId, agentIds))
                .stream()
                .collect(Collectors.toMap(
                        AiAgentConfigEntity::getAgentId,
                        Function.identity(),
                        (left, right) -> left.getId() > right.getId() ? left : right
                ));
        Set<Long> modelIds = configs.values().stream()
                .map(AiAgentConfigEntity::getModelId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AiAgentModelEntity> models = selectModels(modelIds, tenantId);
        Map<Long, Long> subagentCounts = activeSubagentCounts(agentIds, tenantId);
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Map<Long, AgentRunSummary> runSummaries =
                runLogMapper.selectAgentSummaries(
                                tenantId, agentIds, todayStart, LocalDateTime.now().plusNanos(1))
                        .stream()
                        .collect(Collectors.toMap(AgentRunSummary::getAgentId, Function.identity()));

        result.setRecords(agents.stream().map(agent -> {
            AiAgentConfigEntity config = configs.get(agent.getId());
            AiAgentModelEntity model = config == null ? null : models.get(config.getModelId());
            AgentRunSummary run = runSummaries.get(agent.getId());
            return new AgentListItemResponse(
                    agent.getId(),
                    agent.getAgentCode(),
                    agent.getAgentName(),
                    agent.getDescription(),
                    model == null ? null : model.getId(),
                    model == null ? null : model.getConfigName(),
                    model == null ? null : model.getProviderName(),
                    model == null || model.getProtocol() == null
                            ? null : model.getProtocol().getCode(),
                    model == null ? null : model.getModelName(),
                    subagentCounts.getOrDefault(agent.getId(), 0L),
                    run == null ? 0L : value(run.getTotalRuns()),
                    successRate(run),
                    agent.getCreatedAt(),
                    agent.getUpdatedAt()
            );
        }).toList());
        return result;
    }

    public AgentDetailResponse detail(Long id) {
        Long tenantId = tenantId();
        AiAgentEntity agent = requireAgent(id, tenantId);
        AiAgentConfigEntity config = requireConfig(agent.getId(), tenantId);
        AiAgentModelEntity model = config.getModelId() == null
                ? null : modelMapper.selectOne(new LambdaQueryWrapper<AiAgentModelEntity>()
                .eq(AiAgentModelEntity::getTenantId, tenantId)
                .eq(AiAgentModelEntity::getDeleted, 0)
                .eq(AiAgentModelEntity::getStatus, 1)
                .eq(AiAgentModelEntity::getId, config.getModelId()));
        AiAgentSysPromptEntity prompt = config.getSysPromptId() == null
                ? null : promptMapper.selectOne(new LambdaQueryWrapper<AiAgentSysPromptEntity>()
                .eq(AiAgentSysPromptEntity::getTenantId, tenantId)
                .eq(AiAgentSysPromptEntity::getId, config.getSysPromptId()));
        boolean promptAvailable = prompt != null
                && Integer.valueOf(0).equals(prompt.getDeleted())
                && Byte.valueOf((byte) 1).equals(prompt.getStatus());

        return AgentDetailResponse.builder()
                .id(agent.getId())
                .agentCode(agent.getAgentCode())
                .agentName(agent.getAgentName())
                .description(agent.getDescription())
                .agentVersion(agent.getVersion())
                .agentConfigId(config.getId())
                .configVersion(config.getVersion())
                .modelId(model == null ? null : model.getId())
                .modelConfigName(model == null ? null : model.getConfigName())
                .providerName(model == null ? null : model.getProviderName())
                .protocol(model == null || model.getProtocol() == null
                        ? null : model.getProtocol().getCode())
                .modelName(model == null ? null : model.getModelName())
                .sysPromptId(config.getSysPromptId())
                .sysPromptName(prompt == null ? null : prompt.getPromptName())
                .sysPrompt(promptAvailable ? prompt.getSysPrompt() : null)
                .systemPromptAvailable(promptAvailable)
                .maxIters(config.getMaxIters())
                .permissionMode(config.getPermissionMode())
                .compactionEnabled(config.getCompactionEnabled())
                .triggerMessages(config.getTriggerMessages())
                .keepMessages(config.getKeepMessages())
                .triggerTokens(config.getTriggerTokens())
                .keepTokens(config.getKeepTokens())
                .toolResultEvictionEnabled(config.getToolResultEvictionEnabled())
                .memoryEnable(config.getMemoryEnable())
                .planModeEnabled(config.getPlanModeEnabled())
                .planFileDirectory(config.getPlanFileDirectory())
                .taskListEnabled(config.getTaskListEnabled())
                .allowShellInPlanMode(config.getAllowShellInPlanMode())
                .stateStoreType(config.getStateStoreType())
                .tools(toolBindings(agent.getId(), config.getId(), tenantId))
                .skills(skillBindings(agent.getId(), config.getId(), tenantId))
                .knowledgeBases(knowledgeBindings(agent.getId(), config.getId(), tenantId))
                .subagents(subagentBindings(agent.getId(), config.getId(), tenantId))
                .createdAt(agent.getCreatedAt())
                .updatedAt(agent.getUpdatedAt())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentDetailResponse create(AgentSaveRequest request) {
        validateIdentity(request, true);
        Long tenantId = tenantId();
        ensureUniqueCode(request.getAgentCode().trim(), tenantId);
        AiAgentModelEntity model = validateModel(request.getModelId(), tenantId);
        validatePrompt(request.getSysPromptId(), tenantId);
        StateStoreType stateStoreType = resolveStateStoreType(request.getStateStoreType());
        if (stateStoreType == StateStoreType.REDIS) {
            validateRedis();
        }
        validatePermissionMode(request.getPermissionMode());

        AiAgentEntity agent = new AiAgentEntity()
                .setAgentCode(request.getAgentCode().trim())
                .setAgentName(request.getAgentName().trim())
                .setDescription(trimToNull(request.getDescription()))
                .setAgentType(AGENT_TYPE);
        agent.setTenantId(tenantId);
        if (agentMapper.insert(EntityDefaults.create(agent)) != 1) {
            throw new IllegalStateException("智能体创建失败");
        }

        AiAgentConfigEntity config = new AiAgentConfigEntity()
                .setAgentId(agent.getId())
                .setSysPromptId(request.getSysPromptId())
                .setModelId(model == null ? null : model.getId())
                .setMaxIters(defaultInt(request.getMaxIters(), 10))
                .setPermissionMode(defaultText(request.getPermissionMode(), DEFAULT_PERMISSION_MODE))
                .setCompactionEnabled(defaultInt(request.getCompactionEnabled(), 1))
                .setTriggerMessages(defaultInt(request.getTriggerMessages(), 30))
                .setKeepMessages(defaultInt(request.getKeepMessages(), 10))
                .setTriggerTokens(defaultInt(request.getTriggerTokens(), 6000))
                .setKeepTokens(defaultInt(request.getKeepTokens(), 1000))
                .setToolResultEvictionEnabled(
                        defaultInt(request.getToolResultEvictionEnabled(), 1))
                .setMemoryEnable(defaultInt(request.getMemoryEnable(), 1))
                .setPlanModeEnabled(defaultInt(request.getPlanModeEnabled(), 1))
                .setPlanFileDirectory(defaultText(request.getPlanFileDirectory(), "plans"))
                .setTaskListEnabled(defaultInt(request.getTaskListEnabled(), 1))
                .setAllowShellInPlanMode(defaultInt(request.getAllowShellInPlanMode(), 0))
                .setStateStoreType(stateStoreType);
        validateConfig(config);
        config.setTenantId(tenantId);
        if (configMapper.insert(EntityDefaults.create(config)) != 1) {
            throw new IllegalStateException("智能体配置创建失败");
        }

        syncBindings(request, agent.getId(), config.getId(), tenantId);
        invalidateAgentsAfterCommit();
        return detail(agent.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentDetailResponse update(Long id, AgentSaveRequest request) {
        validateIdentity(request, false);
        Long tenantId = tenantId();
        AiAgentEntity agent = requireAgent(id, tenantId);
        AiAgentConfigEntity config = requireConfig(id, tenantId);
        validateVersions(request, agent, config);
        if (StringUtils.hasText(request.getAgentCode())
                && !agent.getAgentCode().equals(request.getAgentCode().trim())) {
            throw new IllegalArgumentException("智能体编码创建后不能修改");
        }
        AiAgentModelEntity model = validateModel(request.getModelId(), tenantId);
        validatePrompt(request.getSysPromptId(), tenantId);
        StateStoreType stateStoreType = resolveStateStoreType(request.getStateStoreType());
        if (stateStoreType == StateStoreType.REDIS
                && config.getStateStoreType() != StateStoreType.REDIS) {
            validateRedis();
        }
        validatePermissionMode(request.getPermissionMode());

        agent.setAgentName(request.getAgentName().trim())
                .setDescription(trimToNull(request.getDescription()))
                .setAgentType(AGENT_TYPE);
        if (agentMapper.updateById(EntityDefaults.update(agent)) != 1) {
            throw optimisticConflict();
        }

        config.setSysPromptId(request.getSysPromptId())
                .setModelId(model == null ? null : model.getId())
                .setMaxIters(defaultInt(request.getMaxIters(), config.getMaxIters()))
                .setPermissionMode(defaultText(
                        request.getPermissionMode(), config.getPermissionMode()))
                .setCompactionEnabled(defaultInt(
                        request.getCompactionEnabled(), config.getCompactionEnabled()))
                .setTriggerMessages(defaultInt(
                        request.getTriggerMessages(), config.getTriggerMessages()))
                .setKeepMessages(defaultInt(
                        request.getKeepMessages(), config.getKeepMessages()))
                .setTriggerTokens(defaultInt(
                        request.getTriggerTokens(), config.getTriggerTokens()))
                .setKeepTokens(defaultInt(request.getKeepTokens(), config.getKeepTokens()))
                .setToolResultEvictionEnabled(defaultInt(
                        request.getToolResultEvictionEnabled(),
                        config.getToolResultEvictionEnabled()))
                .setMemoryEnable(defaultInt(request.getMemoryEnable(), config.getMemoryEnable()))
                .setPlanModeEnabled(defaultInt(
                        request.getPlanModeEnabled(), config.getPlanModeEnabled()))
                .setPlanFileDirectory(defaultText(
                        request.getPlanFileDirectory(), config.getPlanFileDirectory()))
                .setTaskListEnabled(defaultInt(
                        request.getTaskListEnabled(), config.getTaskListEnabled()))
                .setAllowShellInPlanMode(defaultInt(
                        request.getAllowShellInPlanMode(), config.getAllowShellInPlanMode()))
                .setStateStoreType(stateStoreType);
        validateConfig(config);
        if (configMapper.updateById(EntityDefaults.update(config)) != 1) {
            throw optimisticConflict();
        }

        syncBindings(request, agent.getId(), config.getId(), tenantId);
        invalidateAgentsAfterCommit();
        return detail(agent.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        Long tenantId = tenantId();
        AiAgentEntity agent = requireAgent(id, tenantId);
        AiAgentConfigEntity config = requireConfig(id, tenantId);
        agent.setDeleted(1);
        if (agentMapper.updateById(EntityDefaults.update(agent)) != 1) {
            throw new IllegalStateException("智能体删除失败");
        }
        config.setDeleted(1);
        if (configMapper.updateById(EntityDefaults.update(config)) != 1) {
            throw new IllegalStateException("智能体配置删除失败");
        }
        List<AiSubagentEntity> localDefinitions = subagentMapper.selectList(
                new LambdaQueryWrapper<AiSubagentEntity>()
                        .eq(AiSubagentEntity::getTenantId, tenantId)
                        .eq(AiSubagentEntity::getDeleted, 0)
                        .eq(AiSubagentEntity::getSourceType, (byte) 1)
                        .eq(AiSubagentEntity::getLocalAgentId, id));
        for (AiSubagentEntity definition : localDefinitions) {
            definition.setDeleted(1);
            subagentMapper.updateById(EntityDefaults.update(definition));
        }
        invalidateAgentsAfterCommit();
        return true;
    }

    public IPage<AgentRunLogResponse> runs(
            Long agentId,
            long current,
            long size,
            String status,
            LocalDateTime start,
            LocalDateTime end
    ) {
        validatePage(current, size);
        Long tenantId = tenantId();
        requireAgent(agentId, tenantId);
        String normalizedStatus = normalizeRunStatus(status);
        if (start != null && end != null && !start.isBefore(end)) {
            throw new IllegalArgumentException("开始时间必须早于结束时间");
        }
        LambdaQueryWrapper<AiAgentRunLogEntity> query =
                new LambdaQueryWrapper<AiAgentRunLogEntity>()
                        .eq(AiAgentRunLogEntity::getTenantId, tenantId)
                        .eq(AiAgentRunLogEntity::getDeleted, 0)
                        .eq(AiAgentRunLogEntity::getAgentId, agentId)
                        .eq(normalizedStatus != null,
                                AiAgentRunLogEntity::getStatus, normalizedStatus)
                        .ge(start != null, AiAgentRunLogEntity::getStartedAt, start)
                        .lt(end != null, AiAgentRunLogEntity::getStartedAt, end)
                        .orderByDesc(AiAgentRunLogEntity::getStartedAt)
                        .orderByDesc(AiAgentRunLogEntity::getId);
        IPage<AiAgentRunLogEntity> source = runLogMapper.selectPage(
                new Page<>(current, Math.min(size, 100)), query);
        Page<AgentRunLogResponse> result =
                new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(source.getRecords().stream().map(log -> new AgentRunLogResponse(
                log.getId(),
                log.getSessionId(),
                log.getAgentId(),
                log.getAgentConfigId(),
                log.getStatus(),
                log.getErrorCode(),
                log.getErrorMessage(),
                log.getStartedAt(),
                log.getEndedAt(),
                durationMs(log.getStartedAt(), log.getEndedAt())
        )).toList());
        return result;
    }

    private void syncBindings(
            AgentSaveRequest request,
            Long agentId,
            Long configId,
            Long tenantId
    ) {
        Map<Long, AiToolInfoConfigEntity> tools =
                requireTools(request.getSelectedToolIds(), tenantId);
        Map<Long, AiSkillInfoEntity> skills =
                requireSkills(request.getSelectedSkillIds(), tenantId);
        Map<Long, AiKnowledgeBaseEntity> knowledgeBases =
                requireKnowledgeBases(request.getSelectedKnowledgeBaseIds(), tenantId);
        Map<Long, AiSubagentEntity> subagents =
                requireSubagents(request.getSelectedSubagentIds(), tenantId);
        validateNoDelegationCycle(agentId, subagents.values(), tenantId);
        syncTools(agentId, configId, tenantId, tools);
        syncSkills(agentId, configId, tenantId, skills);
        syncKnowledgeBases(agentId, configId, tenantId, knowledgeBases);
        syncSubagents(agentId, configId, tenantId, subagents);
    }

    private void syncTools(
            Long agentId,
            Long configId,
            Long tenantId,
            Map<Long, AiToolInfoConfigEntity> requested
    ) {
        List<AiAgentToolEntity> active = agentToolMapper.selectList(
                new LambdaQueryWrapper<AiAgentToolEntity>()
                        .eq(AiAgentToolEntity::getTenantId, tenantId)
                        .eq(AiAgentToolEntity::getDeleted, 0)
                        .eq(AiAgentToolEntity::getAgentId, agentId)
                        .eq(AiAgentToolEntity::getAgentConfigId, configId));
        Set<Long> retained = new HashSet<>();
        for (AiAgentToolEntity binding : active) {
            if (requested.containsKey(binding.getToolInfoConfigId())) {
                retained.add(binding.getToolInfoConfigId());
            } else {
                logicalDelete(binding, agentToolMapper::updateById);
            }
        }
        for (AiToolInfoConfigEntity tool : requested.values()) {
            if (retained.contains(tool.getId())) {
                continue;
            }
            AiAgentToolEntity binding = new AiAgentToolEntity()
                    .setAgentId(agentId)
                    .setAgentConfigId(configId)
                    .setToolInfoConfigId(tool.getId())
                    .setToolName(tool.getToolName())
                    .setToolAlias(tool.getToolNameExplain())
                    .setToolDescription(tool.getDescription())
                    .setToolGroup(tool.getGroupId())
                    .setStatus((byte) 1);
            binding.setTenantId(tenantId);
            agentToolMapper.insert(EntityDefaults.create(binding));
        }
    }

    private void syncSkills(
            Long agentId,
            Long configId,
            Long tenantId,
            Map<Long, AiSkillInfoEntity> requested
    ) {
        List<AiSkillAgentBindingEntity> active = skillBindingMapper.selectList(
                new LambdaQueryWrapper<AiSkillAgentBindingEntity>()
                        .eq(AiSkillAgentBindingEntity::getTenantId, tenantId)
                        .eq(AiSkillAgentBindingEntity::getDeleted, 0)
                        .eq(AiSkillAgentBindingEntity::getAgentId, agentId)
                        .eq(AiSkillAgentBindingEntity::getAgentConfigId, configId));
        Set<Long> retained = new HashSet<>();
        for (AiSkillAgentBindingEntity binding : active) {
            if (requested.containsKey(binding.getSkillId())) {
                retained.add(binding.getSkillId());
            } else {
                logicalDelete(binding, skillBindingMapper::updateById);
            }
        }
        for (AiSkillInfoEntity skill : requested.values()) {
            if (retained.contains(skill.getId())) {
                continue;
            }
            AiSkillAgentBindingEntity binding = new AiSkillAgentBindingEntity()
                    .setAgentId(agentId)
                    .setAgentConfigId(configId)
                    .setSkillId(skill.getId())
                    .setLoadMode("DYNAMIC")
                    .setOverridePolicy("DENY_OVERRIDE");
            binding.setTenantId(tenantId);
            skillBindingMapper.insert(EntityDefaults.create(binding));
        }
    }

    private void syncKnowledgeBases(
            Long agentId,
            Long configId,
            Long tenantId,
            Map<Long, AiKnowledgeBaseEntity> requested
    ) {
        List<AiKnowledgeAgentBindingEntity> active = knowledgeBindingMapper.selectList(
                new LambdaQueryWrapper<AiKnowledgeAgentBindingEntity>()
                        .eq(AiKnowledgeAgentBindingEntity::getTenantId, tenantId)
                        .eq(AiKnowledgeAgentBindingEntity::getDeleted, 0)
                        .eq(AiKnowledgeAgentBindingEntity::getAgentId, agentId)
                        .eq(AiKnowledgeAgentBindingEntity::getAgentConfigId, configId));
        Set<Long> retained = new HashSet<>();
        for (AiKnowledgeAgentBindingEntity binding : active) {
            if (requested.containsKey(binding.getKnowledgeBaseId())) {
                retained.add(binding.getKnowledgeBaseId());
            } else {
                logicalDelete(binding, knowledgeBindingMapper::updateById);
            }
        }
        for (AiKnowledgeBaseEntity knowledgeBase : requested.values()) {
            if (retained.contains(knowledgeBase.getId())) {
                continue;
            }
            AiKnowledgeAgentBindingEntity binding = new AiKnowledgeAgentBindingEntity()
                    .setAgentId(agentId)
                    .setAgentConfigId(configId)
                    .setKnowledgeBaseId(knowledgeBase.getId())
                    .setStatus((byte) 1);
            binding.setTenantId(tenantId);
            knowledgeBindingMapper.insert(EntityDefaults.create(binding));
        }
    }

    private void syncSubagents(
            Long agentId,
            Long configId,
            Long tenantId,
            Map<Long, AiSubagentEntity> requested
    ) {
        List<AiSubagentAgentBindingEntity> active = subagentBindingMapper.selectList(
                new LambdaQueryWrapper<AiSubagentAgentBindingEntity>()
                        .eq(AiSubagentAgentBindingEntity::getTenantId, tenantId)
                        .eq(AiSubagentAgentBindingEntity::getDeleted, 0)
                        .eq(AiSubagentAgentBindingEntity::getAgentId, agentId)
                        .eq(AiSubagentAgentBindingEntity::getAgentConfigId, configId));
        Set<Long> retained = new HashSet<>();
        for (AiSubagentAgentBindingEntity binding : active) {
            if (requested.containsKey(binding.getSubagentId())) {
                retained.add(binding.getSubagentId());
            } else {
                logicalDelete(binding, subagentBindingMapper::updateById);
            }
        }
        for (AiSubagentEntity subagent : requested.values()) {
            if (retained.contains(subagent.getId())) {
                continue;
            }
            AiSubagentAgentBindingEntity binding = new AiSubagentAgentBindingEntity()
                    .setAgentId(agentId)
                    .setAgentConfigId(configId)
                    .setSubagentId(subagent.getId())
                    .setEnabled((byte) 1)
                    .setVisibleToParent((byte) 1)
                    .setExposeToUser((byte) 0)
                    .setDefaultTimeoutSeconds(0)
                    .setMaxTimeoutSeconds(300)
                    .setMaxParallelTasks(3)
                    .setInheritParentPermissions((byte) 1)
                    .setInheritParentMemory((byte) 1)
                    .setInheritParentKnowledge((byte) 1);
            binding.setTenantId(tenantId);
            subagentBindingMapper.insert(EntityDefaults.create(binding));
        }
    }

    private List<AgentBoundResourceResponse> toolBindings(
            Long agentId, Long configId, Long tenantId
    ) {
        List<AiAgentToolEntity> bindings = agentToolMapper.selectList(
                new LambdaQueryWrapper<AiAgentToolEntity>()
                        .eq(AiAgentToolEntity::getTenantId, tenantId)
                        .eq(AiAgentToolEntity::getDeleted, 0)
                        .eq(AiAgentToolEntity::getAgentId, agentId)
                        .eq(AiAgentToolEntity::getAgentConfigId, configId));
        Map<Long, AiToolInfoConfigEntity> resources = selectByIds(
                bindings.stream().map(AiAgentToolEntity::getToolInfoConfigId).toList(),
                ids -> toolMapper.selectList(new LambdaQueryWrapper<AiToolInfoConfigEntity>()
                        .eq(AiToolInfoConfigEntity::getTenantId, tenantId)
                        .in(AiToolInfoConfigEntity::getId, ids)),
                AiToolInfoConfigEntity::getId
        );
        return bindings.stream().map(binding -> {
            AiToolInfoConfigEntity tool = resources.get(binding.getToolInfoConfigId());
            boolean available = tool != null
                    && Integer.valueOf(0).equals(tool.getDeleted())
                    && Boolean.TRUE.equals(tool.getEnabled());
            String name = tool == null
                    ? defaultText(binding.getToolAlias(), "工具 #" + binding.getToolInfoConfigId())
                    : defaultText(tool.getToolNameExplain(), tool.getToolName());
            return new AgentBoundResourceResponse(
                    binding.getToolInfoConfigId(), name, available);
        }).toList();
    }

    private List<AgentBoundResourceResponse> skillBindings(
            Long agentId, Long configId, Long tenantId
    ) {
        List<AiSkillAgentBindingEntity> bindings = skillBindingMapper.selectList(
                new LambdaQueryWrapper<AiSkillAgentBindingEntity>()
                        .eq(AiSkillAgentBindingEntity::getTenantId, tenantId)
                        .eq(AiSkillAgentBindingEntity::getDeleted, 0)
                        .eq(AiSkillAgentBindingEntity::getAgentId, agentId)
                        .eq(AiSkillAgentBindingEntity::getAgentConfigId, configId));
        Map<Long, AiSkillInfoEntity> resources = selectByIds(
                bindings.stream().map(AiSkillAgentBindingEntity::getSkillId).toList(),
                ids -> skillMapper.selectList(new LambdaQueryWrapper<AiSkillInfoEntity>()
                        .eq(AiSkillInfoEntity::getTenantId, tenantId)
                        .in(AiSkillInfoEntity::getId, ids)),
                AiSkillInfoEntity::getId
        );
        return bindings.stream().map(binding -> {
            AiSkillInfoEntity skill = resources.get(binding.getSkillId());
            boolean available = skill != null
                    && Integer.valueOf(0).equals(skill.getDeleted())
                    && Byte.valueOf((byte) 1).equals(skill.getStatus());
            return new AgentBoundResourceResponse(
                    binding.getSkillId(),
                    skill == null ? "技能 #" + binding.getSkillId() : skill.getName(),
                    available
            );
        }).toList();
    }

    private List<AgentBoundResourceResponse> knowledgeBindings(
            Long agentId, Long configId, Long tenantId
    ) {
        List<AiKnowledgeAgentBindingEntity> bindings = knowledgeBindingMapper.selectList(
                new LambdaQueryWrapper<AiKnowledgeAgentBindingEntity>()
                        .eq(AiKnowledgeAgentBindingEntity::getTenantId, tenantId)
                        .eq(AiKnowledgeAgentBindingEntity::getDeleted, 0)
                        .eq(AiKnowledgeAgentBindingEntity::getAgentId, agentId)
                        .eq(AiKnowledgeAgentBindingEntity::getAgentConfigId, configId));
        Map<Long, AiKnowledgeBaseEntity> resources = selectByIds(
                bindings.stream().map(AiKnowledgeAgentBindingEntity::getKnowledgeBaseId).toList(),
                ids -> knowledgeBaseMapper.selectList(
                        new LambdaQueryWrapper<AiKnowledgeBaseEntity>()
                                .eq(AiKnowledgeBaseEntity::getTenantId, tenantId)
                                .in(AiKnowledgeBaseEntity::getId, ids)),
                AiKnowledgeBaseEntity::getId
        );
        return bindings.stream().map(binding -> {
            AiKnowledgeBaseEntity knowledge = resources.get(binding.getKnowledgeBaseId());
            boolean available = knowledge != null
                    && Integer.valueOf(0).equals(knowledge.getDeleted())
                    && Byte.valueOf((byte) 1).equals(knowledge.getStatus());
            return new AgentBoundResourceResponse(
                    binding.getKnowledgeBaseId(),
                    knowledge == null
                            ? "知识库 #" + binding.getKnowledgeBaseId()
                            : knowledge.getKnowledgeName(),
                    available
            );
        }).toList();
    }

    private List<AgentBoundResourceResponse> subagentBindings(
            Long agentId, Long configId, Long tenantId
    ) {
        List<AiSubagentAgentBindingEntity> bindings = subagentBindingMapper.selectList(
                new LambdaQueryWrapper<AiSubagentAgentBindingEntity>()
                        .eq(AiSubagentAgentBindingEntity::getTenantId, tenantId)
                        .eq(AiSubagentAgentBindingEntity::getDeleted, 0)
                        .eq(AiSubagentAgentBindingEntity::getAgentId, agentId)
                        .eq(AiSubagentAgentBindingEntity::getAgentConfigId, configId));
        Map<Long, AiSubagentEntity> resources = selectByIds(
                bindings.stream().map(AiSubagentAgentBindingEntity::getSubagentId).toList(),
                ids -> subagentMapper.selectList(new LambdaQueryWrapper<AiSubagentEntity>()
                        .eq(AiSubagentEntity::getTenantId, tenantId)
                        .in(AiSubagentEntity::getId, ids)),
                AiSubagentEntity::getId
        );
        Set<Long> localAgentIds = resources.values().stream()
                .filter(item -> Byte.valueOf((byte) 1).equals(item.getSourceType()))
                .map(AiSubagentEntity::getLocalAgentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> activeLocalAgents = selectActiveAgentIds(localAgentIds, tenantId);
        return bindings.stream().map(binding -> {
            AiSubagentEntity subagent = resources.get(binding.getSubagentId());
            boolean sourceAvailable = subagent != null
                    && (!Byte.valueOf((byte) 1).equals(subagent.getSourceType())
                    || activeLocalAgents.contains(subagent.getLocalAgentId()));
            boolean available = subagent != null
                    && Integer.valueOf(0).equals(subagent.getDeleted())
                    && Byte.valueOf((byte) 1).equals(subagent.getEnabled())
                    && sourceAvailable;
            return new AgentBoundResourceResponse(
                    binding.getSubagentId(),
                    subagent == null
                            ? "子智能体 #" + binding.getSubagentId()
                            : subagent.getSubagentName(),
                    available
            );
        }).toList();
    }

    private Map<Long, AiToolInfoConfigEntity> requireTools(
            Collection<Long> ids, Long tenantId
    ) {
        LinkedHashSet<Long> requested = normalizeIds(ids, "工具");
        if (requested.isEmpty()) {
            return Map.of();
        }
        List<AiToolInfoConfigEntity> resources = toolMapper.selectList(
                new LambdaQueryWrapper<AiToolInfoConfigEntity>()
                        .eq(AiToolInfoConfigEntity::getTenantId, tenantId)
                        .eq(AiToolInfoConfigEntity::getDeleted, 0)
                        .eq(AiToolInfoConfigEntity::getEnabled, true)
                        .in(AiToolInfoConfigEntity::getId, requested));
        return requireAll(
                requested, resources, AiToolInfoConfigEntity::getId, "一个或多个工具不可用");
    }

    private Map<Long, AiSkillInfoEntity> requireSkills(
            Collection<Long> ids, Long tenantId
    ) {
        LinkedHashSet<Long> requested = normalizeIds(ids, "技能");
        if (requested.isEmpty()) {
            return Map.of();
        }
        List<AiSkillInfoEntity> resources = skillMapper.selectList(
                new LambdaQueryWrapper<AiSkillInfoEntity>()
                        .eq(AiSkillInfoEntity::getTenantId, tenantId)
                        .eq(AiSkillInfoEntity::getDeleted, 0)
                        .eq(AiSkillInfoEntity::getStatus, (byte) 1)
                        .in(AiSkillInfoEntity::getId, requested));
        return requireAll(
                requested, resources, AiSkillInfoEntity::getId, "一个或多个技能不可用");
    }

    private Map<Long, AiKnowledgeBaseEntity> requireKnowledgeBases(
            Collection<Long> ids, Long tenantId
    ) {
        LinkedHashSet<Long> requested = normalizeIds(ids, "知识库");
        if (requested.isEmpty()) {
            return Map.of();
        }
        List<AiKnowledgeBaseEntity> resources = knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<AiKnowledgeBaseEntity>()
                        .eq(AiKnowledgeBaseEntity::getTenantId, tenantId)
                        .eq(AiKnowledgeBaseEntity::getDeleted, 0)
                        .eq(AiKnowledgeBaseEntity::getStatus, (byte) 1)
                        .in(AiKnowledgeBaseEntity::getId, requested));
        return requireAll(
                requested, resources, AiKnowledgeBaseEntity::getId,
                "一个或多个知识库不可用");
    }

    private Map<Long, AiSubagentEntity> requireSubagents(
            Collection<Long> ids, Long tenantId
    ) {
        LinkedHashSet<Long> requested = normalizeIds(ids, "子智能体");
        if (requested.isEmpty()) {
            return Map.of();
        }
        List<AiSubagentEntity> resources = subagentMapper.selectList(
                new LambdaQueryWrapper<AiSubagentEntity>()
                        .eq(AiSubagentEntity::getTenantId, tenantId)
                        .eq(AiSubagentEntity::getDeleted, 0)
                        .eq(AiSubagentEntity::getEnabled, (byte) 1)
                        .in(AiSubagentEntity::getId, requested));
        Map<Long, AiSubagentEntity> result = requireAll(
                requested, resources, AiSubagentEntity::getId,
                "一个或多个子智能体不可用");
        Set<Long> localAgentIds = resources.stream()
                .filter(item -> Byte.valueOf((byte) 1).equals(item.getSourceType()))
                .map(AiSubagentEntity::getLocalAgentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!selectActiveAgentIds(localAgentIds, tenantId).containsAll(localAgentIds)) {
            throw new IllegalArgumentException("一个或多个本地子智能体的来源智能体不可用");
        }
        return result;
    }

    private void validateNoDelegationCycle(
            Long currentAgentId,
            Collection<AiSubagentEntity> requested,
            Long tenantId
    ) {
        Map<Long, Set<Long>> graph = new LinkedHashMap<>();
        List<AiSubagentAgentBindingEntity> bindings = subagentBindingMapper.selectList(
                new LambdaQueryWrapper<AiSubagentAgentBindingEntity>()
                        .eq(AiSubagentAgentBindingEntity::getTenantId, tenantId)
                        .eq(AiSubagentAgentBindingEntity::getDeleted, 0)
                        .eq(AiSubagentAgentBindingEntity::getEnabled, (byte) 1));
        Set<Long> subagentIds = bindings.stream()
                .map(AiSubagentAgentBindingEntity::getSubagentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AiSubagentEntity> definitions = selectByIds(
                subagentIds,
                ids -> subagentMapper.selectList(new LambdaQueryWrapper<AiSubagentEntity>()
                        .eq(AiSubagentEntity::getTenantId, tenantId)
                        .eq(AiSubagentEntity::getDeleted, 0)
                        .in(AiSubagentEntity::getId, ids)),
                AiSubagentEntity::getId
        );
        for (AiSubagentAgentBindingEntity binding : bindings) {
            if (Objects.equals(binding.getAgentId(), currentAgentId)) {
                continue;
            }
            AiSubagentEntity definition = definitions.get(binding.getSubagentId());
            if (definition != null
                    && Byte.valueOf((byte) 1).equals(definition.getSourceType())
                    && definition.getLocalAgentId() != null) {
                graph.computeIfAbsent(binding.getAgentId(), ignored -> new LinkedHashSet<>())
                        .add(definition.getLocalAgentId());
            }
        }
        Set<Long> requestedLocalAgents = requested.stream()
                .filter(item -> Byte.valueOf((byte) 1).equals(item.getSourceType()))
                .map(AiSubagentEntity::getLocalAgentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        graph.put(currentAgentId, requestedLocalAgents);
        List<Long> cycle = findReachableCycle(currentAgentId, graph);
        if (!cycle.isEmpty()) {
            throw new IllegalArgumentException(
                    "本地子智能体委派不能形成循环: "
                            + cycle.stream().map(String::valueOf)
                            .collect(Collectors.joining(" -> "))
            );
        }
    }

    private List<Long> findReachableCycle(Long start, Map<Long, Set<Long>> graph) {
        Set<Long> visited = new HashSet<>();
        Set<Long> visiting = new HashSet<>();
        Deque<Long> path = new ArrayDeque<>();
        List<Long> cycle = new ArrayList<>();
        findCycle(start, graph, visited, visiting, path, cycle);
        return cycle;
    }

    private boolean findCycle(
            Long node,
            Map<Long, Set<Long>> graph,
            Set<Long> visited,
            Set<Long> visiting,
            Deque<Long> path,
            List<Long> cycle
    ) {
        if (visiting.contains(node)) {
            boolean copy = false;
            for (Long item : path) {
                if (Objects.equals(item, node)) {
                    copy = true;
                }
                if (copy) {
                    cycle.add(item);
                }
            }
            cycle.add(node);
            return true;
        }
        if (!visited.add(node)) {
            return false;
        }
        visiting.add(node);
        path.addLast(node);
        for (Long target : graph.getOrDefault(node, Set.of())) {
            if (findCycle(target, graph, visited, visiting, path, cycle)) {
                return true;
            }
        }
        path.removeLast();
        visiting.remove(node);
        return false;
    }

    private Map<Long, Long> activeSubagentCounts(
            Collection<Long> agentIds, Long tenantId
    ) {
        List<AiSubagentAgentBindingEntity> bindings = subagentBindingMapper.selectList(
                new LambdaQueryWrapper<AiSubagentAgentBindingEntity>()
                        .eq(AiSubagentAgentBindingEntity::getTenantId, tenantId)
                        .eq(AiSubagentAgentBindingEntity::getDeleted, 0)
                        .eq(AiSubagentAgentBindingEntity::getEnabled, (byte) 1)
                        .in(AiSubagentAgentBindingEntity::getAgentId, agentIds));
        Map<Long, AiSubagentEntity> definitions = selectByIds(
                bindings.stream().map(AiSubagentAgentBindingEntity::getSubagentId).toList(),
                ids -> subagentMapper.selectList(new LambdaQueryWrapper<AiSubagentEntity>()
                        .eq(AiSubagentEntity::getTenantId, tenantId)
                        .eq(AiSubagentEntity::getDeleted, 0)
                        .eq(AiSubagentEntity::getEnabled, (byte) 1)
                        .in(AiSubagentEntity::getId, ids)),
                AiSubagentEntity::getId
        );
        Set<Long> localAgentIds = definitions.values().stream()
                .filter(item -> Byte.valueOf((byte) 1).equals(item.getSourceType()))
                .map(AiSubagentEntity::getLocalAgentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> activeLocalAgents = selectActiveAgentIds(localAgentIds, tenantId);
        Map<Long, Long> counts = new HashMap<>();
        for (AiSubagentAgentBindingEntity binding : bindings) {
            AiSubagentEntity definition = definitions.get(binding.getSubagentId());
            if (definition == null) {
                continue;
            }
            if (Byte.valueOf((byte) 1).equals(definition.getSourceType())
                    && !activeLocalAgents.contains(definition.getLocalAgentId())) {
                continue;
            }
            counts.merge(binding.getAgentId(), 1L, Long::sum);
        }
        return counts;
    }

    private Set<Long> selectActiveAgentIds(Collection<Long> ids, Long tenantId) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return agentMapper.selectList(new LambdaQueryWrapper<AiAgentEntity>()
                        .eq(AiAgentEntity::getTenantId, tenantId)
                        .eq(AiAgentEntity::getDeleted, 0)
                        .in(AiAgentEntity::getId, ids))
                .stream()
                .map(AiAgentEntity::getId)
                .collect(Collectors.toSet());
    }

    private Map<Long, AiAgentModelEntity> selectModels(
            Collection<Long> ids, Long tenantId
    ) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return modelMapper.selectList(new LambdaQueryWrapper<AiAgentModelEntity>()
                        .eq(AiAgentModelEntity::getTenantId, tenantId)
                        .eq(AiAgentModelEntity::getDeleted, 0)
                        .eq(AiAgentModelEntity::getStatus, 1)
                        .in(AiAgentModelEntity::getId, ids))
                .stream()
                .collect(Collectors.toMap(AiAgentModelEntity::getId, Function.identity()));
    }

    private AiAgentModelEntity validateModel(Long modelId, Long tenantId) {
        if (modelId == null) {
            return null;
        }
        AiAgentModelEntity model = modelMapper.selectOne(
                new LambdaQueryWrapper<AiAgentModelEntity>()
                        .eq(AiAgentModelEntity::getTenantId, tenantId)
                        .eq(AiAgentModelEntity::getDeleted, 0)
                        .eq(AiAgentModelEntity::getStatus, 1)
                        .eq(AiAgentModelEntity::getId, modelId));
        if (model == null) {
            throw new IllegalArgumentException("所选模型配置不存在、已停用或已删除");
        }
        return model;
    }

    private void validatePrompt(Long promptId, Long tenantId) {
        if (promptId == null) {
            return;
        }
        long count = promptMapper.selectCount(
                new LambdaQueryWrapper<AiAgentSysPromptEntity>()
                        .eq(AiAgentSysPromptEntity::getTenantId, tenantId)
                        .eq(AiAgentSysPromptEntity::getDeleted, 0)
                        .eq(AiAgentSysPromptEntity::getStatus, (byte) 1)
                        .eq(AiAgentSysPromptEntity::getId, promptId));
        if (count != 1) {
            throw new IllegalArgumentException("所选系统提示词不存在、已停用或已删除");
        }
    }

    private void validateIdentity(AgentSaveRequest request, boolean creating) {
        if (request == null) {
            throw new IllegalArgumentException("智能体请求不能为空");
        }
        if (!StringUtils.hasText(request.getAgentName())) {
            throw new IllegalArgumentException("智能体名称不能为空");
        }
        if (request.getAgentName().trim().length() > 100) {
            throw new IllegalArgumentException("智能体名称不能超过 100 个字符");
        }
        if (creating && !StringUtils.hasText(request.getAgentCode())) {
            throw new IllegalArgumentException("智能体编码不能为空");
        }
        if (StringUtils.hasText(request.getAgentCode())
                && !request.getAgentCode().trim().matches(AGENT_CODE_PATTERN)) {
            throw new IllegalArgumentException(
                    "智能体编码只能包含小写字母、数字和连字符，长度为 2–64");
        }
        if (request.getDescription() != null && request.getDescription().length() > 500) {
            throw new IllegalArgumentException("智能体描述不能超过 500 个字符");
        }
    }

    private void validateVersions(
            AgentSaveRequest request,
            AiAgentEntity agent,
            AiAgentConfigEntity config
    ) {
        if (request.getAgentVersion() == null || request.getConfigVersion() == null) {
            throw new IllegalArgumentException("更新智能体必须携带版本号");
        }
        if (!Objects.equals(request.getAgentVersion(), agent.getVersion())
                || !Objects.equals(request.getConfigVersion(), config.getVersion())) {
            throw optimisticConflict();
        }
    }

    private void validateConfig(AiAgentConfigEntity config) {
        if (config.getMaxIters() == null || config.getMaxIters() < 1
                || config.getMaxIters() > 100) {
            throw new IllegalArgumentException("最大循环次数必须在 1–100 之间");
        }
        requireNonNegative(config.getTriggerMessages(), "触发消息数");
        requireNonNegative(config.getKeepMessages(), "保留消息数");
        requireNonNegative(config.getTriggerTokens(), "触发 Token");
        requireNonNegative(config.getKeepTokens(), "保留 Token");
        if (Integer.valueOf(1).equals(config.getCompactionEnabled())) {
            if (config.getKeepMessages() > config.getTriggerMessages()) {
                throw new IllegalArgumentException("保留消息数不能大于触发消息数");
            }
            if (config.getKeepTokens() > config.getTriggerTokens()) {
                throw new IllegalArgumentException("保留 Token 不能大于触发 Token");
            }
        }
        if (config.getPlanFileDirectory() == null
                || config.getPlanFileDirectory().isBlank()
                || config.getPlanFileDirectory().contains("..")
                || config.getPlanFileDirectory().contains("/")
                || config.getPlanFileDirectory().contains("\\")) {
            throw new IllegalArgumentException("计划文件目录必须是工作区内的单级目录名");
        }
    }

    private void validatePermissionMode(String mode) {
        String value = defaultText(mode, DEFAULT_PERMISSION_MODE);
        try {
            PermissionMode.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("不支持的权限模式: " + value);
        }
    }

    private StateStoreType resolveStateStoreType(StateStoreType type) {
        StateStoreType resolved = type == null ? StateStoreType.LOCAL_FILE : type;
        if (resolved != StateStoreType.LOCAL_FILE && resolved != StateStoreType.REDIS) {
            throw new IllegalArgumentException(
                    "stateStoreType 只支持 local_file 或 redis");
        }
        return resolved;
    }

    private void validateRedis() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String pong = connection.ping();
            if (!"PONG".equalsIgnoreCase(pong)) {
                throw new IllegalStateException("Redis 连通性检查失败");
            }
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Redis 不可用，不能将会话状态存储切换为 redis", exception);
        }
    }

    private AiAgentEntity requireAgent(Long id, Long tenantId) {
        if (id == null) {
            throw new IllegalArgumentException("智能体 ID 不能为空");
        }
        AiAgentEntity agent = agentMapper.selectOne(
                new LambdaQueryWrapper<AiAgentEntity>()
                        .eq(AiAgentEntity::getTenantId, tenantId)
                        .eq(AiAgentEntity::getDeleted, 0)
                        .eq(AiAgentEntity::getId, id));
        if (agent == null) {
            throw new IllegalArgumentException("智能体不存在或已删除");
        }
        return agent;
    }

    private AiAgentConfigEntity requireConfig(Long agentId, Long tenantId) {
        AiAgentConfigEntity config = configMapper.selectOne(
                new LambdaQueryWrapper<AiAgentConfigEntity>()
                        .eq(AiAgentConfigEntity::getTenantId, tenantId)
                        .eq(AiAgentConfigEntity::getDeleted, 0)
                        .eq(AiAgentConfigEntity::getAgentId, agentId));
        if (config == null) {
            throw new IllegalStateException("智能体配置不存在");
        }
        return config;
    }

    private void ensureUniqueCode(String code, Long tenantId) {
        long count = agentMapper.selectCount(
                new LambdaQueryWrapper<AiAgentEntity>()
                        .eq(AiAgentEntity::getTenantId, tenantId)
                        .eq(AiAgentEntity::getAgentCode, code));
        if (count > 0) {
            throw new IllegalArgumentException("智能体编码已存在");
        }
    }

    private AgentRunSummary summary(
            Long tenantId,
            LocalDateTime start,
            LocalDateTime end,
            Long agentId
    ) {
        AgentRunSummary summary = runLogMapper.selectSummary(tenantId, start, end, agentId);
        return summary == null ? new AgentRunSummary() : summary;
    }

    private Double successRate(AgentRunSummary summary) {
        if (summary == null) {
            return null;
        }
        long finished = value(summary.getSuccessRuns())
                + value(summary.getFailedRuns())
                + value(summary.getCancelledRuns());
        if (finished == 0) {
            return null;
        }
        return round(value(summary.getSuccessRuns()) * 100D / finished);
    }

    private Double difference(Double current, Double previous) {
        if (current == null || previous == null) {
            return null;
        }
        return round(current - previous);
    }

    private Double percentChange(long current, long previous) {
        if (previous == 0) {
            return current == 0 ? 0D : 100D;
        }
        return round((current - previous) * 100D / previous);
    }

    private Double round(Double value) {
        if (value == null) {
            return null;
        }
        return Math.round(value * 100D) / 100D;
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }

    private Long durationMs(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return null;
        }
        return Math.max(0L, Duration.between(start, end).toMillis());
    }

    private String normalizeRunStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if (!RUN_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("不支持的运行状态: " + status);
        }
        return normalized;
    }

    private void validatePage(long current, long size) {
        if (current < 1) {
            throw new IllegalArgumentException("页码必须大于 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("每页数量必须在 1–100 之间");
        }
    }

    private LambdaQueryWrapper<AiAgentEntity> activeAgents(Long tenantId) {
        return new LambdaQueryWrapper<AiAgentEntity>()
                .eq(AiAgentEntity::getTenantId, tenantId)
                .eq(AiAgentEntity::getDeleted, 0);
    }

    private Long tenantId() {
        UserInfo user = UserContext.get();
        if (user == null || user.getTenantId() == null) {
            throw new IllegalStateException("缺少认证租户上下文");
        }
        return user.getTenantId();
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

    private IllegalStateException optimisticConflict() {
        return new IllegalStateException("智能体配置已被其他人修改，请刷新后重试");
    }

    private <T extends com.zw.common.entity.BaseEntity> void logicalDelete(
            T entity,
            Function<T, Integer> updater
    ) {
        entity.setDeleted(1);
        if (updater.apply(EntityDefaults.update(entity)) != 1) {
            throw new IllegalStateException("能力绑定更新失败");
        }
    }

    private <T> Map<Long, T> requireAll(
            Set<Long> requested,
            List<T> resources,
            Function<T, Long> idGetter,
            String message
    ) {
        Map<Long, T> result = resources.stream().collect(Collectors.toMap(
                idGetter, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        if (result.size() != requested.size() || !result.keySet().containsAll(requested)) {
            throw new IllegalArgumentException(message);
        }
        return result;
    }

    private <T> Map<Long, T> selectByIds(
            Collection<Long> ids,
            Function<Collection<Long>, List<T>> loader,
            Function<T, Long> idGetter
    ) {
        LinkedHashSet<Long> normalized = ids == null
                ? new LinkedHashSet<>()
                : ids.stream().filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalized.isEmpty()) {
            return Map.of();
        }
        return loader.apply(normalized).stream().collect(Collectors.toMap(
                idGetter, Function.identity(), (left, right) -> left));
    }

    private LinkedHashSet<Long> normalizeIds(Collection<Long> ids, String label) {
        if (ids == null || ids.isEmpty()) {
            return new LinkedHashSet<>();
        }
        if (ids.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(label + " ID 不能为空");
        }
        return new LinkedHashSet<>(ids);
    }

    private int defaultInt(Integer value, Integer fallback) {
        return value == null ? (fallback == null ? 0 : fallback) : value;
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void requireNonNegative(Integer value, String label) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(label + "不能小于 0");
        }
    }
}
