package com.zhiran.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiran.agent.entity.AiAgentSysPromptEntity;
import com.zhiran.agent.entity.DTO.SysPromptAnalyticsResponse;
import com.zhiran.agent.entity.DTO.SysPromptBindingCountRow;
import com.zhiran.agent.entity.DTO.SysPromptDetailResponse;
import com.zhiran.agent.entity.DTO.SysPromptListItemResponse;
import com.zhiran.agent.entity.DTO.SysPromptMetricsResponse;
import com.zhiran.agent.entity.DTO.SysPromptOptionResponse;
import com.zhiran.agent.entity.DTO.SysPromptSaveRequest;
import com.zhiran.agent.factory.agentFactory.AgentRuntimeFactory;
import com.zhiran.agent.mapper.AiAgentSysPromptMapper;
import com.zhiran.common.context.UserContext;
import com.zhiran.common.context.UserInfo;
import com.zhiran.common.support.EntityDefaults;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SysPromptManagementService {

    private static final byte COMPATIBLE_STATUS = 1;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final int MAX_CONTENT_LENGTH = 50_000;
    private static final int PREVIEW_LENGTH = 180;

    private final AiAgentSysPromptMapper promptMapper;
    private final AgentRuntimeFactory agentRuntimeFactory;

    public SysPromptMetricsResponse metrics() {
        Long tenantId = tenantId();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long total = promptMapper.selectCount(currentPrompts(tenantId));
        long newToday = promptMapper.selectCount(currentPrompts(tenantId)
                .ge(AiAgentSysPromptEntity::getCreatedAt, todayStart));
        return new SysPromptMetricsResponse(
                total,
                newToday,
                promptMapper.countBoundPrompts(tenantId),
                promptMapper.countBoundAgents(tenantId)
        );
    }

    public SysPromptAnalyticsResponse analytics(int limit) {
        if (limit < 1 || limit > 10) {
            throw new IllegalArgumentException("分析条数必须在 1–10 之间");
        }
        Long tenantId = tenantId();
        List<SysPromptAnalyticsResponse.BindingRanking> ranking =
                promptMapper.selectTopBindings(tenantId, limit).stream()
                        .map(row -> new SysPromptAnalyticsResponse.BindingRanking(
                                row.getId(),
                                row.getPromptName(),
                                value(row.getBindingCount())
                        ))
                        .toList();
        List<SysPromptAnalyticsResponse.RecentPrompt> recentlyUpdated =
                promptMapper.selectPage(
                                new Page<>(1, limit),
                                currentPrompts(tenantId)
                                        .orderByDesc(AiAgentSysPromptEntity::getUpdatedAt)
                                        .orderByDesc(AiAgentSysPromptEntity::getId)
                        ).getRecords().stream()
                        .map(prompt -> new SysPromptAnalyticsResponse.RecentPrompt(
                                prompt.getId(),
                                prompt.getPromptName(),
                                length(prompt.getSysPrompt()),
                                prompt.getUpdatedAt()
                        ))
                        .toList();
        return new SysPromptAnalyticsResponse(ranking, recentlyUpdated);
    }

    public IPage<SysPromptListItemResponse> page(
            long current,
            long size,
            String keyword
    ) {
        Long tenantId = tenantId();
        LambdaQueryWrapper<AiAgentSysPromptEntity> query = currentPrompts(tenantId)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(AiAgentSysPromptEntity::getPromptName, trim(keyword))
                        .or()
                        .like(AiAgentSysPromptEntity::getDescription, trim(keyword)))
                .orderByDesc(AiAgentSysPromptEntity::getUpdatedAt)
                .orderByDesc(AiAgentSysPromptEntity::getId);
        Page<AiAgentSysPromptEntity> promptPage = promptMapper.selectPage(
                new Page<>(normalizePage(current), normalizeSize(size)),
                query
        );
        Map<Long, Long> bindingCounts = bindingCounts(
                tenantId,
                promptPage.getRecords().stream()
                        .map(AiAgentSysPromptEntity::getId)
                        .toList()
        );
        return promptPage.convert(prompt -> toListItem(
                prompt,
                bindingCounts.getOrDefault(prompt.getId(), 0L)
        ));
    }

    public List<SysPromptOptionResponse> listOptions() {
        return promptMapper.selectList(currentPrompts(tenantId())
                        .orderByAsc(AiAgentSysPromptEntity::getPromptName)
                        .orderByAsc(AiAgentSysPromptEntity::getId))
                .stream()
                .map(prompt -> new SysPromptOptionResponse(
                        prompt.getId(),
                        prompt.getPromptName(),
                        prompt.getDescription(),
                        prompt.getSysPrompt()
                ))
                .toList();
    }

    public SysPromptDetailResponse detail(Long id) {
        AiAgentSysPromptEntity prompt = requirePrompt(id);
        return toDetail(prompt, bindingCount(tenantId(), prompt.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public SysPromptDetailResponse create(SysPromptSaveRequest request) {
        ValidatedPrompt values = validate(request);
        Long tenantId = tenantId();
        ensureUniqueName(values.promptName(), null, tenantId);
        AiAgentSysPromptEntity prompt = new AiAgentSysPromptEntity()
                .setPromptName(values.promptName())
                .setDescription(values.description())
                .setSysPrompt(values.sysPrompt())
                .setStatus(COMPATIBLE_STATUS);
        prompt.setTenantId(tenantId);
        if (promptMapper.insert(EntityDefaults.create(prompt)) != 1) {
            throw new IllegalStateException("系统提示词创建失败");
        }
        return toDetail(prompt, 0L);
    }

    @Transactional(rollbackFor = Exception.class)
    public SysPromptDetailResponse update(SysPromptSaveRequest request) {
        if (request == null || request.getId() == null) {
            throw new IllegalArgumentException("系统提示词 ID 不能为空");
        }
        ValidatedPrompt values = validate(request);
        Long tenantId = tenantId();
        AiAgentSysPromptEntity prompt = requirePrompt(request.getId());
        if (request.getVersion() != null
                && !request.getVersion().equals(prompt.getVersion())) {
            throw optimisticConflict();
        }
        ensureUniqueName(values.promptName(), prompt.getId(), tenantId);
        prompt.setPromptName(values.promptName())
                .setDescription(values.description())
                .setSysPrompt(values.sysPrompt())
                .setStatus(COMPATIBLE_STATUS);
        if (promptMapper.updateById(EntityDefaults.update(prompt)) != 1) {
            throw optimisticConflict();
        }
        invalidateAgentsAfterCommit();
        return toDetail(prompt, bindingCount(tenantId, prompt.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        AiAgentSysPromptEntity prompt = requirePrompt(id);
        if (promptMapper.deleteById(prompt.getId()) != 1) {
            throw new IllegalStateException("系统提示词删除失败");
        }
        invalidateAgentsAfterCommit();
        return true;
    }

    private ValidatedPrompt validate(SysPromptSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("系统提示词内容不能为空");
        }
        String promptName = trim(request.getPromptName());
        if (!StringUtils.hasText(promptName)) {
            throw new IllegalArgumentException("提示词名称不能为空");
        }
        if (promptName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("提示词名称不能超过 100 个字符");
        }
        String description = trimToNull(request.getDescription());
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("描述不能超过 500 个字符");
        }
        String content = request.getSysPrompt();
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("提示词内容不能为空");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("提示词内容不能超过 50000 个字符");
        }
        return new ValidatedPrompt(promptName, description, content);
    }

    private void ensureUniqueName(String promptName, Long excludedId, Long tenantId) {
        long count = promptMapper.selectCount(currentPrompts(tenantId)
                .eq(AiAgentSysPromptEntity::getPromptName, promptName)
                .ne(excludedId != null, AiAgentSysPromptEntity::getId, excludedId));
        if (count > 0) {
            throw new IllegalArgumentException("提示词名称已存在");
        }
    }

    private AiAgentSysPromptEntity requirePrompt(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("系统提示词 ID 不能为空");
        }
        AiAgentSysPromptEntity prompt = promptMapper.selectOne(
                currentPrompts(tenantId())
                        .eq(AiAgentSysPromptEntity::getId, id)
                        .last("LIMIT 1")
        );
        if (prompt == null) {
            throw new IllegalArgumentException("系统提示词不存在或已删除");
        }
        return prompt;
    }

    private Map<Long, Long> bindingCounts(Long tenantId, List<Long> promptIds) {
        Map<Long, Long> result = new HashMap<>();
        if (promptIds.isEmpty()) {
            return result;
        }
        for (SysPromptBindingCountRow row
                : promptMapper.selectBindingCounts(tenantId, promptIds)) {
            result.put(row.getId(), value(row.getBindingCount()));
        }
        return result;
    }

    private long bindingCount(Long tenantId, Long promptId) {
        return bindingCounts(tenantId, List.of(promptId))
                .getOrDefault(promptId, 0L);
    }

    private SysPromptListItemResponse toListItem(
            AiAgentSysPromptEntity prompt,
            long bindingCount
    ) {
        return new SysPromptListItemResponse(
                prompt.getId(),
                prompt.getPromptName(),
                prompt.getDescription(),
                preview(prompt.getSysPrompt()),
                length(prompt.getSysPrompt()),
                bindingCount,
                prompt.getCreatedAt(),
                prompt.getUpdatedAt(),
                prompt.getVersion()
        );
    }

    private SysPromptDetailResponse toDetail(
            AiAgentSysPromptEntity prompt,
            long bindingCount
    ) {
        return new SysPromptDetailResponse(
                prompt.getId(),
                prompt.getPromptName(),
                prompt.getDescription(),
                prompt.getSysPrompt(),
                length(prompt.getSysPrompt()),
                bindingCount,
                prompt.getCreatedAt(),
                prompt.getUpdatedAt(),
                prompt.getVersion()
        );
    }

    private LambdaQueryWrapper<AiAgentSysPromptEntity> currentPrompts(Long tenantId) {
        return new LambdaQueryWrapper<AiAgentSysPromptEntity>()
                .eq(AiAgentSysPromptEntity::getTenantId, tenantId)
                .eq(AiAgentSysPromptEntity::getDeleted, 0);
    }

    private String preview(String content) {
        if (content == null) {
            return "";
        }
        String compact = content.replaceAll("\\s+", " ").trim();
        return compact.length() <= PREVIEW_LENGTH
                ? compact
                : compact.substring(0, PREVIEW_LENGTH) + "…";
    }

    private long length(String value) {
        return value == null ? 0L : value.length();
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        String result = trim(value);
        return StringUtils.hasText(result) ? result : null;
    }

    private long normalizePage(long current) {
        return Math.max(current, 1L);
    }

    private long normalizeSize(long size) {
        return Math.min(Math.max(size, 1L), 100L);
    }

    private Long tenantId() {
        UserInfo user = UserContext.get();
        if (user == null || user.getTenantId() == null) {
            throw new IllegalStateException("缺少已认证的租户上下文");
        }
        return user.getTenantId();
    }

    private IllegalStateException optimisticConflict() {
        return new IllegalStateException("数据已被其他用户修改，请刷新后重试");
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

    private record ValidatedPrompt(
            String promptName,
            String description,
            String sysPrompt
    ) {
    }
}
