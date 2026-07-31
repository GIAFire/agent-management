package com.zw.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zw.agent.entity.AiAgentConfigEntity;
import com.zw.agent.entity.AiAgentEntity;
import com.zw.agent.entity.AiSkillAgentBindingEntity;
import com.zw.agent.entity.AiSkillInfoEntity;
import com.zw.agent.entity.AiSkillLogEntity;
import com.zw.agent.entity.AiSkillResourceEntity;
import com.zw.agent.entity.AiSkillRoleEntity;
import com.zw.agent.entity.SysRoleEntity;
import com.zw.agent.entity.DTO.AiSkillInfoSaveRequest;
import com.zw.agent.entity.DTO.SkillDetailResponse;
import com.zw.agent.entity.DTO.SkillListItemResponse;
import com.zw.agent.entity.DTO.SkillMetricsResponse;
import com.zw.agent.entity.DTO.SkillUseLogResponse;
import com.zw.agent.mapper.AiSkillAgentBindingMapper;
import com.zw.agent.mapper.AiSkillInfoMapper;
import com.zw.agent.mapper.AiSkillResourceMapper;
import com.zw.agent.mapper.AiSkillRoleMapper;
import com.zw.common.context.UserContext;
import com.zw.common.context.UserInfo;
import com.zw.common.support.EntityDefaults;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SkillManagementService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern SKILL_CODE =
            Pattern.compile("^[a-z][a-z0-9-]{0,62}[a-z0-9]$");
    private static final Set<String> CATEGORIES = Set.of(
            "data", "report", "document", "code", "research", "file", "rag", "ops"
    );
    private static final Set<String> RISKS =
            Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
    private static final Set<String> OPERATIONS =
            Set.of("LOAD_SKILL", "READ_REFERENCE", "RUN_SCRIPT");

    private final AiSkillInfoService skillService;
    private final AiSkillResourceService resourceService;
    private final AiSkillRoleService roleService;
    private final AiSkillLogService logService;
    private final AiSkillAgentBindingService bindingService;
    private final AiAgentService agentService;
    private final AiAgentConfigService agentConfigService;
    private final SysRoleService sysRoleService;
    private final AiSkillInfoMapper skillMapper;
    private final AiSkillResourceMapper resourceMapper;
    private final AiSkillRoleMapper roleMapper;
    private final AiSkillAgentBindingMapper bindingMapper;
    private final SkillContentMetadataParser contentMetadataParser;

    public SkillMetricsResponse metrics() {
        Long tenantId = currentTenantId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime yesterdaySameTime = now.minusDays(1);

        long total = skillService.count(skillQuery(tenantId));
        long enabled = skillService.count(skillQuery(tenantId)
                .eq(AiSkillInfoEntity::getStatus, (byte) 1));
        List<AiSkillLogEntity> today = logsBetween(tenantId, todayStart, now);
        List<AiSkillLogEntity> yesterday =
                logsBetween(tenantId, yesterdayStart, yesterdaySameTime);
        long failures = today.stream()
                .filter(log -> Byte.valueOf((byte) 0).equals(log.getSuccess()))
                .count();
        long successes = today.stream()
                .filter(log -> Byte.valueOf((byte) 1).equals(log.getSuccess()))
                .count();

        return new SkillMetricsResponse(
                total,
                enabled,
                today.size(),
                yesterday.isEmpty()
                        ? null
                        : percentage(today.size() - yesterday.size(), yesterday.size()),
                today.isEmpty() ? null : percentage(successes, today.size()),
                failures,
                averageDuration(today),
                durationDifference(today, yesterday)
        );
    }

    public IPage<SkillListItemResponse> pageSkills(
            long current,
            long size,
            String keyword,
            String category,
            Byte status,
            String riskLevel
    ) {
        Long tenantId = currentTenantId();
        LambdaQueryWrapper<AiSkillInfoEntity> query = skillQuery(tenantId);
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            query.and(wrapper -> wrapper
                    .like(AiSkillInfoEntity::getName, value)
                    .or()
                    .like(AiSkillInfoEntity::getSource, value)
                    .or()
                    .like(AiSkillInfoEntity::getDescription, value));
        }
        if (status != null) {
            validateStatus(status);
            query.eq(AiSkillInfoEntity::getStatus, status);
        }
        if (StringUtils.hasText(riskLevel)) {
            query.eq(AiSkillInfoEntity::getRiskLevel, normalizeRisk(riskLevel));
        }
        query.orderByDesc(AiSkillInfoEntity::getUpdatedAt)
                .orderByDesc(AiSkillInfoEntity::getCreatedAt);

        List<AiSkillInfoEntity> skills = skillService.list(query);
        String normalizedCategory = normalizeOptionalCategory(category);
        if (normalizedCategory != null) {
            skills = skills.stream()
                    .filter(skill -> normalizedCategory.equals(categoryOf(skill)))
                    .toList();
        }

        Map<Long, List<String>> roles = rolesBySkill(tenantId, ids(skills));
        List<AiSkillLogEntity> todayLogs = logsBetween(
                tenantId,
                LocalDate.now().atStartOfDay(),
                LocalDateTime.now()
        );
        Map<Long, List<AiSkillLogEntity>> logsBySkill = todayLogs.stream()
                .filter(log -> log.getSkillId() != null)
                .collect(Collectors.groupingBy(AiSkillLogEntity::getSkillId));
        Map<Long, Long> currentBoundAgents = currentBoundAgents(tenantId);
        Set<Long> skillsWithScripts = resourceService.list(
                        new LambdaQueryWrapper<AiSkillResourceEntity>()
                                .eq(AiSkillResourceEntity::getTenantId, tenantId)
                                .likeRight(AiSkillResourceEntity::getResourcePath, "scripts/")
                ).stream()
                .map(AiSkillResourceEntity::getSkillId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<SkillListItemResponse> responses = skills.stream()
                .map(skill -> toListItem(
                        skill,
                        roles.getOrDefault(skill.getId(), List.of()),
                        logsBySkill.getOrDefault(skill.getId(), List.of()),
                        currentBoundAgents.getOrDefault(skill.getId(), 0L),
                        skillsWithScripts.contains(skill.getId())
                ))
                .toList();
        long safeCurrent = sanitizePage(current);
        long safeSize = sanitizeSize(size);
        int start = (int) Math.min(responses.size(), (safeCurrent - 1) * safeSize);
        int end = (int) Math.min(responses.size(), start + safeSize);
        Page<SkillListItemResponse> page = new Page<>(safeCurrent, safeSize, responses.size());
        page.setRecords(responses.subList(start, end));
        return page;
    }

    public List<AiSkillInfoEntity> listCandidates() {
        Long tenantId = currentTenantId();
        List<AiSkillInfoEntity> skills = skillService.list(skillQuery(tenantId)
                .eq(AiSkillInfoEntity::getStatus, (byte) 1)
                .orderByAsc(AiSkillInfoEntity::getName));
        skills.forEach(skill -> {
            skill.setSkillName(skill.getName());
            skill.setSkillKey(skill.getSource());
            skill.setSkillMdContent(null);
            skill.setSkillContent(null);
            skill.setMetadataJson(null);
            skill.setTagsJson(null);
        });
        return skills;
    }

    public SkillDetailResponse detail(Long id) {
        AiSkillInfoEntity skill = requireSkill(id, currentTenantId());
        List<String> roleCodes = rolesBySkill(skill.getTenantId(), List.of(skill.getId()))
                .getOrDefault(skill.getId(), List.of());
        return new SkillDetailResponse(
                skill.getId(),
                skill.getSource(),
                skill.getName(),
                skill.getDescription(),
                skill.getSkillContent(),
                categoryOf(skill),
                tagsOf(skill),
                skill.getRiskLevel(),
                skill.getStatus(),
                roleCodes
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public SkillDetailResponse create(AiSkillInfoSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Skill request must not be null");
        }
        Long tenantId = currentTenantId();
        String displayName =
                requiredText(firstText(request.getSkillName(), request.getName()), "Skill name");
        String code = normalizeCode(firstText(request.getSkillKey(), request.getSource()));
        ensureCodeAvailable(tenantId, code);
        String requestedDescription =
                requiredText(request.getDescription(), "Skill description");
        String category = normalizeCategory(request.getCategory());
        String risk = normalizeRisk(request.getRiskLevel());
        List<String> tags = normalizeTags(request.getTags());
        List<String> roleCodes = normalizeAndValidateRoles(tenantId, request.getRoleCodes());
        Byte status = request.getStatus() == null ? (byte) 0 : request.getStatus();
        validateStatus(status);
        String content = firstText(request.getSkillContent(), request.getSkillMdContent());
        if (!StringUtils.hasText(content)) {
            content = contentMetadataParser.createInitialContent(
                    code,
                    Objects.toString(requestedDescription, ""),
                    displayName
            );
        }
        SkillContentMetadataParser.SkillContentMetadata contentMetadata =
                contentMetadataParser.parseRequired(content);
        String name = contentMetadata.name();
        String description = contentMetadata.description();
        if (status == 1) {
            validateEnabled(name, description, content, roleCodes);
        }

        AiSkillInfoEntity skill = new AiSkillInfoEntity()
                .setName(name)
                .setDescription(description)
                .setSkillContent(content)
                .setSource(code)
                .setMetadataJson(metadataJson(category))
                .setRiskLevel(risk)
                .setTagsJson(tagsJson(tags))
                .setStatus(status);
        skill.setTenantId(tenantId);
        skillService.save(EntityDefaults.create(skill));
        replaceRoles(skill.getId(), tenantId, roleCodes);
        return detail(skill.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public SkillDetailResponse update(AiSkillInfoSaveRequest request) {
        if (request == null || request.getId() == null) {
            throw new IllegalArgumentException("Skill id must not be null");
        }
        Long tenantId = currentTenantId();
        AiSkillInfoEntity skill = requireSkill(request.getId(), tenantId);
        String requestedCode = firstText(request.getSkillKey(), request.getSource());
        if (StringUtils.hasText(requestedCode)
                && !skill.getSource().equalsIgnoreCase(requestedCode.trim())) {
            throw new IllegalArgumentException("Skill code cannot be changed");
        }
        String content = request.getSkillContent() != null
                ? request.getSkillContent()
                : request.getSkillMdContent();
        if (content == null) {
            content = skill.getSkillContent();
        }
        SkillContentMetadataParser.SkillContentMetadata contentMetadata =
                contentMetadataParser.parseRequired(content);
        String name = contentMetadata.name();
        String description = contentMetadata.description();
        String category = normalizeCategory(request.getCategory());
        String risk = normalizeRisk(request.getRiskLevel());
        List<String> tags = normalizeTags(request.getTags());
        List<String> roleCodes = normalizeAndValidateRoles(tenantId, request.getRoleCodes());
        Byte status = request.getStatus() == null ? skill.getStatus() : request.getStatus();
        validateStatus(status);
        if (status == 1) {
            validateEnabled(name, description, content, roleCodes);
        }

        skill.setName(name)
                .setDescription(description)
                .setSkillContent(content)
                .setMetadataJson(metadataJson(category))
                .setRiskLevel(risk)
                .setTagsJson(tagsJson(tags))
                .setStatus(status);
        skillService.updateById(EntityDefaults.update(skill));
        replaceRoles(skill.getId(), tenantId, roleCodes);
        return detail(skill.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        Long tenantId = currentTenantId();
        AiSkillInfoEntity skill = requireSkill(id, tenantId);
        long bindingCount = bindingMapper.countIncludingDeleted(id, tenantId);
        long logCount = logService.count(new LambdaQueryWrapper<AiSkillLogEntity>()
                .eq(AiSkillLogEntity::getTenantId, tenantId)
                .eq(AiSkillLogEntity::getSkillId, id));
        if (bindingCount > 0 || logCount > 0) {
            bindingService.remove(new LambdaQueryWrapper<AiSkillAgentBindingEntity>()
                    .eq(AiSkillAgentBindingEntity::getTenantId, tenantId)
                    .eq(AiSkillAgentBindingEntity::getSkillId, id));
            return skillService.removeById(skill.getId());
        }
        resourceMapper.hardDeleteBySkill(id, tenantId);
        roleMapper.hardDeleteBySkill(id, tenantId);
        return skillMapper.hardDeleteById(id, tenantId) > 0;
    }

    public IPage<SkillUseLogResponse> pageLogs(
            long current,
            long size,
            Long skillId,
            Byte success,
            String operation
    ) {
        Long tenantId = currentTenantId();
        LambdaQueryWrapper<AiSkillLogEntity> query =
                new LambdaQueryWrapper<AiSkillLogEntity>()
                        .eq(AiSkillLogEntity::getTenantId, tenantId);
        if (skillId != null) {
            query.eq(AiSkillLogEntity::getSkillId, skillId);
        }
        if (success != null) {
            if (success != 0 && success != 1) {
                throw new IllegalArgumentException("success must be 0 or 1");
            }
            query.eq(AiSkillLogEntity::getSuccess, success);
        }
        if (StringUtils.hasText(operation)) {
            String normalized = operation.trim().toUpperCase(Locale.ROOT);
            if (!OPERATIONS.contains(normalized)) {
                throw new IllegalArgumentException("Unsupported skill operation");
            }
            query.eq(AiSkillLogEntity::getOperation, normalized);
        }
        query.orderByDesc(AiSkillLogEntity::getStartedAt)
                .orderByDesc(AiSkillLogEntity::getId);
        IPage<AiSkillLogEntity> page =
                logService.page(new Page<>(sanitizePage(current), sanitizeSize(size)), query);
        return convertLogs(page, tenantId);
    }

    public List<SkillUseLogResponse> recentLogs(int limit, Byte success) {
        IPage<SkillUseLogResponse> page = pageLogs(1, Math.min(100, Math.max(1, limit)), null, success, null);
        return page.getRecords();
    }

    private IPage<SkillUseLogResponse> convertLogs(IPage<AiSkillLogEntity> page, Long tenantId) {
        Set<Long> skillIds = page.getRecords().stream()
                .map(AiSkillLogEntity::getSkillId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AiSkillInfoEntity> skills = skillIds.isEmpty()
                ? Map.of()
                : skillMapper.selectNamesIncludingDeleted(tenantId, skillIds).stream()
                        .collect(Collectors.toMap(AiSkillInfoEntity::getId, Function.identity()));
        Set<Long> agentIds = page.getRecords().stream()
                .map(AiSkillLogEntity::getAgentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AiAgentEntity> agents = agentIds.isEmpty()
                ? Map.of()
                : agentService.list(new LambdaQueryWrapper<AiAgentEntity>()
                                .eq(AiAgentEntity::getTenantId, tenantId)
                                .in(AiAgentEntity::getId, agentIds))
                        .stream()
                        .collect(Collectors.toMap(AiAgentEntity::getId, Function.identity()));
        return page.convert(log -> {
            AiSkillInfoEntity skill = skills.get(log.getSkillId());
            AiAgentEntity agent = agents.get(log.getAgentId());
            return new SkillUseLogResponse(
                    log.getId(),
                    log.getSkillId(),
                    skill == null ? null : skill.getSource(),
                    skill == null ? null : skill.getName(),
                    log.getAgentId(),
                    agent == null ? null : agent.getAgentName(),
                    log.getOperation(),
                    log.getResourcePath(),
                    log.getSuccess(),
                    log.getErrorMessage(),
                    log.getStartedAt(),
                    log.getDurationMs()
            );
        });
    }

    private SkillListItemResponse toListItem(
            AiSkillInfoEntity skill,
            List<String> roles,
            List<AiSkillLogEntity> logs,
            long boundAgents,
            boolean hasScripts
    ) {
        long successful = logs.stream()
                .filter(log -> Byte.valueOf((byte) 1).equals(log.getSuccess()))
                .count();
        return new SkillListItemResponse(
                skill.getId(),
                skill.getSource(),
                skill.getName(),
                skill.getDescription(),
                categoryOf(skill),
                tagsOf(skill),
                skill.getRiskLevel(),
                skill.getStatus(),
                roles,
                logs.size(),
                boundAgents,
                logs.isEmpty() ? null : percentage(successful, logs.size()),
                hasScripts,
                skill.getUpdatedAt()
        );
    }

    private Map<Long, Long> currentBoundAgents(Long tenantId) {
        List<AiSkillAgentBindingEntity> bindings = bindingService.list(
                new LambdaQueryWrapper<AiSkillAgentBindingEntity>()
                        .eq(AiSkillAgentBindingEntity::getTenantId, tenantId)
        );
        Set<Long> agentIds = bindings.stream()
                .map(AiSkillAgentBindingEntity::getAgentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (agentIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> currentConfigByAgent = agentConfigService.list(
                        new LambdaQueryWrapper<AiAgentConfigEntity>()
                                .eq(AiAgentConfigEntity::getTenantId, tenantId)
                                .in(AiAgentConfigEntity::getAgentId, agentIds)
                ).stream()
                .sorted(Comparator.comparing(AiAgentConfigEntity::getCreatedAt,
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(AiAgentConfigEntity::getId))
                .collect(Collectors.toMap(
                        AiAgentConfigEntity::getAgentId,
                        AiAgentConfigEntity::getId,
                        (first, second) -> second
                ));
        return bindings.stream()
                .filter(binding -> Objects.equals(
                        binding.getAgentConfigId(),
                        currentConfigByAgent.get(binding.getAgentId())
                ))
                .collect(Collectors.groupingBy(
                        AiSkillAgentBindingEntity::getSkillId,
                        Collectors.mapping(
                                AiSkillAgentBindingEntity::getAgentId,
                                Collectors.collectingAndThen(Collectors.toSet(), set -> (long) set.size())
                        )
                ));
    }

    private Map<Long, List<String>> rolesBySkill(Long tenantId, Collection<Long> skillIds) {
        if (skillIds.isEmpty()) {
            return Map.of();
        }
        return roleService.list(new LambdaQueryWrapper<AiSkillRoleEntity>()
                        .eq(AiSkillRoleEntity::getTenantId, tenantId)
                        .in(AiSkillRoleEntity::getSkillInfoId, skillIds)
                        .orderByAsc(AiSkillRoleEntity::getRoleCode))
                .stream()
                .collect(Collectors.groupingBy(
                        AiSkillRoleEntity::getSkillInfoId,
                        LinkedHashMap::new,
                        Collectors.mapping(AiSkillRoleEntity::getRoleCode, Collectors.toList())
                ));
    }

    private void replaceRoles(Long skillId, Long tenantId, List<String> roleCodes) {
        roleMapper.hardDeleteBySkill(skillId, tenantId);
        List<AiSkillRoleEntity> roles = roleCodes.stream().map(code -> {
            AiSkillRoleEntity role = new AiSkillRoleEntity()
                    .setSkillInfoId(skillId)
                    .setRoleCode(code);
            role.setTenantId(tenantId);
            return EntityDefaults.create(role);
        }).toList();
        if (!roles.isEmpty()) {
            roleService.saveBatch(roles);
        }
    }

    private List<String> normalizeAndValidateRoles(Long tenantId, List<String> rawRoleCodes) {
        List<String> roleCodes = rawRoleCodes == null
                ? List.of("0")
                : rawRoleCodes.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .distinct()
                        .toList();
        if (roleCodes.isEmpty()) {
            return List.of();
        }
        if (roleCodes.contains("0")) {
            return List.of("0");
        }
        long valid = sysRoleService.count(new LambdaQueryWrapper<SysRoleEntity>()
                .eq(SysRoleEntity::getTenantId, tenantId)
                .eq(SysRoleEntity::getStatus, 1)
                .in(SysRoleEntity::getRoleCode, roleCodes));
        if (valid != roleCodes.size()) {
            throw new IllegalArgumentException("One or more roles do not exist in the current tenant");
        }
        return roleCodes;
    }

    private void validateEnabled(
            String name,
            String description,
            String content,
            List<String> roleCodes
    ) {
        requiredText(name, "Skill name");
        requiredText(description, "Skill description");
        requiredText(content, "SKILL.md content");
        if (roleCodes.isEmpty()) {
            throw new IllegalArgumentException("At least one role is required before enabling a skill");
        }
    }

    private void ensureCodeAvailable(Long tenantId, String code) {
        if (skillMapper.countCodeIncludingDeleted(tenantId, code) > 0) {
            throw new IllegalArgumentException("Skill code already exists in the current tenant");
        }
    }

    private AiSkillInfoEntity requireSkill(Long id, Long tenantId) {
        if (id == null) {
            throw new IllegalArgumentException("Skill id must not be null");
        }
        AiSkillInfoEntity skill = skillService.getOne(skillQuery(tenantId)
                .eq(AiSkillInfoEntity::getId, id), false);
        if (skill == null) {
            throw new IllegalArgumentException("Skill does not exist in the current tenant");
        }
        return skill;
    }

    private LambdaQueryWrapper<AiSkillInfoEntity> skillQuery(Long tenantId) {
        return new LambdaQueryWrapper<AiSkillInfoEntity>()
                .eq(AiSkillInfoEntity::getTenantId, tenantId);
    }

    private List<AiSkillLogEntity> logsBetween(
            Long tenantId,
            LocalDateTime start,
            LocalDateTime end
    ) {
        return logService.list(new LambdaQueryWrapper<AiSkillLogEntity>()
                .eq(AiSkillLogEntity::getTenantId, tenantId)
                .ge(AiSkillLogEntity::getStartedAt, start)
                .lt(AiSkillLogEntity::getStartedAt, end));
    }

    private Double averageDuration(List<AiSkillLogEntity> logs) {
        var average = logs.stream()
                .map(AiSkillLogEntity::getDurationMs)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .average();
        return average.isEmpty()
                ? null
                : BigDecimal.valueOf(average.getAsDouble())
                        .setScale(1, RoundingMode.HALF_UP)
                        .doubleValue();
    }

    private Double durationDifference(
            List<AiSkillLogEntity> today,
            List<AiSkillLogEntity> yesterday
    ) {
        Double todayAverage = averageDuration(today);
        Double yesterdayAverage = averageDuration(yesterday);
        return todayAverage == null || yesterdayAverage == null
                ? null
                : BigDecimal.valueOf(todayAverage - yesterdayAverage)
                        .setScale(1, RoundingMode.HALF_UP)
                        .doubleValue();
    }

    private Double percentage(long numerator, long denominator) {
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private List<Long> ids(List<AiSkillInfoEntity> skills) {
        return skills.stream().map(AiSkillInfoEntity::getId).toList();
    }

    private String metadataJson(String category) {
        try {
            return OBJECT_MAPPER.writeValueAsString(Map.of("category", category));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialize skill metadata", e);
        }
    }

    private String tagsJson(List<String> tags) {
        try {
            return OBJECT_MAPPER.writeValueAsString(tags);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialize skill tags", e);
        }
    }

    private String categoryOf(AiSkillInfoEntity skill) {
        if (!StringUtils.hasText(skill.getMetadataJson())) {
            return null;
        }
        try {
            Map<String, Object> metadata =
                    OBJECT_MAPPER.readValue(skill.getMetadataJson(), new TypeReference<>() {});
            Object value = metadata.get("category");
            return value == null ? null : String.valueOf(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<String> tagsOf(AiSkillInfoEntity skill) {
        if (!StringUtils.hasText(skill.getTagsJson())) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(skill.getTagsJson(), new TypeReference<>() {});
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<String> normalizeTags(List<String> rawTags) {
        if (rawTags == null) {
            return List.of();
        }
        LinkedHashSet<String> tags = rawTags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (tags.size() > 20) {
            throw new IllegalArgumentException("A skill supports at most 20 tags");
        }
        if (tags.stream().anyMatch(tag -> tag.length() > 32)) {
            throw new IllegalArgumentException("Each skill tag supports at most 32 characters");
        }
        return new ArrayList<>(tags);
    }

    private String normalizeCode(String value) {
        String code = requiredText(value, "Skill code").toLowerCase(Locale.ROOT);
        if (!SKILL_CODE.matcher(code).matches()) {
            throw new IllegalArgumentException(
                    "Skill code must be 2-64 lowercase letters, digits or hyphens, start with a letter and end with a letter or digit"
            );
        }
        return code;
    }

    private String normalizeCategory(String value) {
        String category = requiredText(value, "Skill category").toLowerCase(Locale.ROOT);
        if (!CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("Unsupported skill category");
        }
        return category;
    }

    private String normalizeOptionalCategory(String value) {
        return StringUtils.hasText(value) ? normalizeCategory(value) : null;
    }

    private String normalizeRisk(String value) {
        String risk = StringUtils.hasText(value)
                ? value.trim().toUpperCase(Locale.ROOT)
                : "LOW";
        if (!RISKS.contains(risk)) {
            throw new IllegalArgumentException("Unsupported skill risk level");
        }
        return risk;
    }

    private void validateStatus(Byte status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("status must be 0 or 1");
        }
    }

    private long sanitizePage(long value) {
        return Math.max(1, value);
    }

    private long sanitizeSize(long value) {
        return Math.min(100, Math.max(1, value));
    }

    private String requiredText(String value, String fieldName) {
        String result = trimToNull(value);
        if (result == null) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return result;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private Long currentTenantId() {
        UserInfo userInfo = UserContext.get();
        if (userInfo == null || userInfo.getTenantId() == null) {
            throw new IllegalStateException("Authenticated tenant context is required");
        }
        return userInfo.getTenantId();
    }
}
