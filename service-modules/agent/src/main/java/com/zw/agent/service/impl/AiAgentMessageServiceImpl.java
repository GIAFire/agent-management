package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentMessageEntity;
import com.zw.agent.entity.AiAgentSessionEntity;
import com.zw.agent.mapper.AiAgentMessageMapper;
import com.zw.agent.mapper.AiAgentSessionMapper;
import com.zw.agent.runtime.message.RuntimeMessageDraft;
import com.zw.agent.service.AiAgentMessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.common.context.UserInfo;
import com.zw.common.support.EntityDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Agent 消息表：保存用户输入、Agent 回复、工具消息等完整上下文 服务实现类
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Service
public class AiAgentMessageServiceImpl extends ServiceImpl<AiAgentMessageMapper, AiAgentMessageEntity> implements AiAgentMessageService {

    @Autowired
    private AiAgentMessageMapper agentMessageMapper;
    @Autowired
    private AiAgentSessionMapper agentSessionMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(rollbackFor = Exception.class)
    @Override
    public AiAgentMessageEntity saveUserMessage(UserInfo userInfo, Long sessionId, String content) {
        LocalDateTime now = LocalDateTime.now();
        long seq = allocateMessageSequences(userInfo.getTenantId(), sessionId, 1);
        AiAgentMessageEntity agentMessageEntity = new AiAgentMessageEntity();
        agentMessageEntity.setRole("USER");
        agentMessageEntity.setMessageType("USER_TEXT");
        agentMessageEntity.setMessageStatus("COMPLETED");
        agentMessageEntity.setSenderName(userInfo.getUserName());
        agentMessageEntity.setContentFormat("TEXT");
        agentMessageEntity.setTextContent(content);
        agentMessageEntity.setContentSchemaVersion(1);
        agentMessageEntity.setTenantId(userInfo.getTenantId());
        agentMessageEntity.setSessionId(sessionId);
        agentMessageEntity.setSeq(seq);
        agentMessageEntity.setStartedAt(now);
        agentMessageEntity.setFinishedAt(now);
        agentMessageEntity.setDurationMs(0L);
        agentMessageEntity.setCreatedBy(userInfo.getUserId());
        agentMessageMapper.insert(EntityDefaults.create(agentMessageEntity));
        touchSessionLastMessageAt(sessionId, now, seq == 1L ? titleFromContent(content) : null);
        return agentMessageEntity;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void bindRunId(Long messageId, Long runId) {
        if (messageId == null || runId == null) {
            return;
        }
        AiAgentMessageEntity entity = new AiAgentMessageEntity();
        entity.setId(messageId);
        entity.setRunId(runId);
        agentMessageMapper.updateById(EntityDefaults.update(entity));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public AiAgentMessageEntity saveRunMessages(
            UserInfo userInfo,
            Long sessionId,
            Long runId,
            List<RuntimeMessageDraft> drafts,
            Integer usageToken,
            Double usageTime
    ) {
        if (drafts == null || drafts.isEmpty()) {
            return null;
        }

        long currentSeq = allocateMessageSequences(userInfo.getTenantId(), sessionId, drafts.size());
        String finalAssistantTextKey = finalAssistantTextKey(drafts);
        Map<String, Long> runtimeIdMap = new HashMap<>();
        AiAgentMessageEntity finalOutput = null;

        for (RuntimeMessageDraft draft : drafts) {
            AiAgentMessageEntity entity = toEntity(userInfo, sessionId, runId, currentSeq++, draft, runtimeIdMap);
            if (Objects.equals(draft.getRuntimeKey(), finalAssistantTextKey)) {
                entity.setUsageJson(buildUsageJson(usageToken, usageTime));
            }
            agentMessageMapper.insert(EntityDefaults.create(entity));
            runtimeIdMap.put(draft.getRuntimeKey(), entity.getId());
            if (Objects.equals(draft.getRuntimeKey(), finalAssistantTextKey)) {
                finalOutput = entity;
            }
        }

        touchSessionLastMessageAt(sessionId, LocalDateTime.now(), null);
        return finalOutput;
    }

    private void touchSessionLastMessageAt(Long sessionId, LocalDateTime time, String title) {
        if (sessionId == null) {
            return;
        }
        AiAgentSessionEntity session = new AiAgentSessionEntity();
        session.setId(sessionId);
        session.setLastMessageAt(time == null ? LocalDateTime.now() : time);
        if (title != null && !title.isBlank()) {
            session.setTitle(title);
        }
        agentSessionMapper.updateById(EntityDefaults.update(session));
    }

    private String titleFromContent(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        String title = content.strip().replaceAll("\\s+", " ");
        return title.length() > 18 ? title.substring(0, 18) : title;
    }

    private AiAgentMessageEntity toEntity(
            UserInfo userInfo,
            Long sessionId,
            Long runId,
            long seq,
            RuntimeMessageDraft draft,
            Map<String, Long> runtimeIdMap
    ) {
        AiAgentMessageEntity entity = new AiAgentMessageEntity();
        entity.setTenantId(userInfo.getTenantId());
        entity.setSessionId(sessionId);
        entity.setRunId(runId);
        entity.setSeq(seq);
        if (draft.getParentRuntimeKey() != null) {
            entity.setParentMessageId(runtimeIdMap.get(draft.getParentRuntimeKey()));
        }
        entity.setRole(draft.getRole());
        entity.setMessageType(draft.getMessageType());
        entity.setMessageStatus(defaultText(draft.getMessageStatus(), "COMPLETED"));
        entity.setSenderName(draft.getSenderName());
        entity.setContentFormat(defaultText(draft.getContentFormat(), "TEXT"));
        entity.setTextContent(draft.getTextContent());
        entity.setContentJson(toJson(draft.getContent()));
        entity.setContentSchemaVersion(1);
        entity.setRefType(draft.getRefType());
        entity.setRefId(draft.getRefId());
        entity.setRefKey(draft.getRefKey());
        entity.setExternalMessageId(draft.getExternalMessageId());
        entity.setFinishReason(draft.getFinishReason());
        entity.setErrorCode(draft.getErrorCode());
        entity.setErrorMessage(safeErrorMessage(draft.getErrorMessage()));
        entity.setStartedAt(draft.getStartedAt());
        entity.setFinishedAt(draft.getFinishedAt());
        entity.setDurationMs(draft.getDurationMs());
        entity.setCreatedBy(userInfo.getUserId());
        return entity;
    }

    private long allocateMessageSequences(Long tenantId, Long sessionId, int count) {
        if (count <= 0) {
            return 0L;
        }
        Long lockedSessionId = agentSessionMapper.lockSessionForUpdate(tenantId, sessionId);
        if (lockedSessionId == null) {
            throw new IllegalArgumentException("当前会话不存在,无法分配消息序号");
        }
        Long maxSeq = agentMessageMapper.selectMaxSeq(tenantId, sessionId);
        return (maxSeq == null ? 0L : maxSeq) + 1L;
    }

    private String finalAssistantTextKey(List<RuntimeMessageDraft> drafts) {
        String runtimeKey = null;
        for (RuntimeMessageDraft draft : drafts) {
            if ("ASSISTANT".equals(draft.getRole()) && "ASSISTANT_TEXT".equals(draft.getMessageType())) {
                runtimeKey = draft.getRuntimeKey();
            }
        }
        return runtimeKey;
    }

    private String buildUsageJson(Integer usageToken, Double usageTime) {
        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("totalTokens", usageToken == null ? 0 : usageToken);
        usage.put("time", usageTime == null ? 0.0 : usageTime);
        return toJson(usage);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ignored) {
            return jsonString(String.valueOf(value));
        }
    }

    private String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n") + "\"";
    }

    private String safeErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }
        String safe = errorMessage
                .replaceAll("(?i)(authorization\\s*[:=]\\s*)(bearer\\s+)?[^\\s,}\\]]+", "$1***")
                .replaceAll("(?i)(api[_-]?key|access[_-]?token|refresh[_-]?token|password|secret)([\"'\\s:=]+)[^,\"'\\s}\\]]+", "$1$2***");
        return safe.length() > 2_000 ? safe.substring(0, 2_000) : safe;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
