package com.zw.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zw.agent.entity.AiAgentPlanEntity;
import com.zw.agent.entity.AiAgentPlanOpLogEntity;
import com.zw.agent.entity.AiAgentPlanTaskEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.event.AgentRuntimeEvent;
import com.zw.agent.service.AiAgentPlanOpLogService;
import com.zw.agent.service.AiAgentPlanRuntimeService;
import com.zw.agent.service.AiAgentPlanService;
import com.zw.agent.service.AiAgentPlanTaskService;
import com.zw.agent.service.plan.PlanRuntimeEventTracker;
import com.zw.common.context.UserInfo;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.ConfirmResult;
import io.agentscope.core.event.RequireUserConfirmEvent;
import io.agentscope.core.event.ToolCallDeltaEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.event.ToolResultEndEvent;
import io.agentscope.core.event.UserConfirmResultEvent;
import io.agentscope.core.message.ToolResultState;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.state.AgentState;
import io.agentscope.core.state.Task;
import io.agentscope.harness.agent.HarnessAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAgentPlanRuntimeServiceImpl implements AiAgentPlanRuntimeService {

    private static final String TOOL_PLAN_ENTER = "plan_enter";
    private static final String TOOL_PLAN_WRITE = "plan_write";
    private static final String TOOL_PLAN_EXIT = "plan_exit";
    private static final String TOOL_TODO_WRITE = "todo_write";
    private static final String DEFAULT_PLAN_DIR = "plans";
    private static final String DEFAULT_PLAN_FILE = "PLAN.md";
    private static final int MAX_OP_PAYLOAD_LENGTH = 20_000;
    private static final DateTimeFormatter PLAN_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> PLAN_TOOL_NAMES = Set.of(
            TOOL_PLAN_ENTER,
            TOOL_PLAN_WRITE,
            TOOL_PLAN_EXIT,
            TOOL_TODO_WRITE
    );
    private static final Set<String> REUSABLE_PLAN_STATUSES = Set.of(
            "DRAFT",
            "WAITING_APPROVAL",
            "REJECTED"
    );

    private final AiAgentPlanService planService;
    private final AiAgentPlanTaskService planTaskService;
    private final AiAgentPlanOpLogService planOpLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public PlanRuntimeEventTracker newTracker() {
        return new PlanRuntimeEventTracker();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> handleRuntimeEvent(
            HarnessAgent agent,
            RuntimeContext runtimeContext,
            AgentRuntimeEvent runtimeEvent,
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId,
            PlanRuntimeEventTracker tracker
    ) {
        if (runtimeEvent == null || runtimeEvent.getRawEvent() == null) {
            return null;
        }

        try {
            AgentEvent rawEvent = runtimeEvent.getRawEvent();
            if (rawEvent instanceof ToolCallStartEvent toolCallStartEvent) {
                return handleToolCallStart(toolCallStartEvent, tracker);
            }
            if (rawEvent instanceof ToolCallDeltaEvent toolCallDeltaEvent) {
                handleToolCallDelta(toolCallDeltaEvent, tracker);
                return null;
            }
            if (rawEvent instanceof RequireUserConfirmEvent requireUserConfirmEvent) {
                return handleRequireUserConfirm(
                        requireUserConfirmEvent,
                        agent,
                        runtimeContext,
                        config,
                        userInfo,
                        sessionId,
                        runId
                );
            }
            if (rawEvent instanceof UserConfirmResultEvent userConfirmResultEvent) {
                return handleUserConfirmResult(
                        userConfirmResultEvent,
                        agent,
                        runtimeContext,
                        config,
                        userInfo,
                        sessionId,
                        runId
                );
            }
            if (rawEvent instanceof ToolResultEndEvent toolResultEndEvent) {
                return handleToolResultEnd(
                        toolResultEndEvent,
                        agent,
                        runtimeContext,
                        config,
                        userInfo,
                        sessionId,
                        runId,
                        tracker
                );
            }
        } catch (Exception ex) {
            log.warn("Handle plan runtime event failed, eventType={}, runId={}",
                    runtimeEvent.getEventType(), runId, ex);
        }

        return null;
    }

    private Map<String, Object> handleToolCallStart(
            ToolCallStartEvent event,
            PlanRuntimeEventTracker tracker
    ) {
        String toolName = normalizeToolName(event.getToolCallName());
        if (!isPlanTool(toolName)) {
            return null;
        }
        tracker.startTool(event.getToolCallId(), toolName);
        return lightweightPayload(opTypeForToolStart(toolName), toolName, event.getToolCallId());
    }

    private void handleToolCallDelta(
            ToolCallDeltaEvent event,
            PlanRuntimeEventTracker tracker
    ) {
        String toolName = normalizeToolName(event.getToolCallName());
        if (!isPlanTool(toolName) && !isPlanTool(tracker.toolName(event.getToolCallId()))) {
            return;
        }
        tracker.appendInput(event.getToolCallId(), event.getDelta());
    }

    private Map<String, Object> handleRequireUserConfirm(
            RequireUserConfirmEvent event,
            HarnessAgent agent,
            RuntimeContext runtimeContext,
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId
    ) {
        ToolUseBlock planExitToolCall = firstToolCall(event.getToolCalls(), TOOL_PLAN_EXIT);
        if (planExitToolCall == null) {
            return null;
        }

        AgentState agentState = currentAgentState(agent, runtimeContext);
        AiAgentPlanEntity plan = ensurePlanForCurrentSession(
                config,
                userInfo,
                sessionId,
                runId,
                agentState,
                false
        );
        String beforeStatus = plan.getStatus();
        plan.setStatus("WAITING_APPROVAL");
        plan.setRunId(runId);
        touchUpdate(plan, userInfo);
        planService.updateById(plan);

        recordOp(
                config,
                userInfo,
                sessionId,
                runId,
                plan,
                "PLAN_EXIT_REQUEST",
                planExitToolCall.getId(),
                beforeStatus,
                plan.getStatus(),
                Map.of(
                        "replyId", nullToEmpty(event.getReplyId()),
                        "toolCall", toolUsePayload(planExitToolCall)
                ),
                true,
                null
        );

        return snapshotPayload("PLAN_EXIT_REQUEST", TOOL_PLAN_EXIT, planExitToolCall.getId(), plan, true);
    }

    private Map<String, Object> handleUserConfirmResult(
            UserConfirmResultEvent event,
            HarnessAgent agent,
            RuntimeContext runtimeContext,
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId
    ) {
        ConfirmResult planExitConfirm = firstPlanExitConfirm(event.getConfirmResults());
        if (planExitConfirm == null) {
            return null;
        }

        AgentState agentState = currentAgentState(agent, runtimeContext);
        AiAgentPlanEntity plan = ensurePlanForCurrentSession(
                config,
                userInfo,
                sessionId,
                runId,
                agentState,
                false
        );

        String beforeStatus = plan.getStatus();
        LocalDateTime now = LocalDateTime.now();
        boolean confirmed = planExitConfirm.isConfirmed();
        plan.setRunId(runId);
        plan.setApprovedBy(userInfo.getUserId());
        plan.setApprovedAt(now);
        plan.setApprovalComment(confirmed ? "User approved plan_exit" : "User rejected plan_exit");
        if (confirmed) {
            plan.setStatus("EXECUTING");
            if (plan.getStartedAt() == null) {
                plan.setStartedAt(now);
            }
        } else {
            plan.setStatus("REJECTED");
        }
        touchUpdate(plan, userInfo);
        planService.updateById(plan);

        recordOp(
                config,
                userInfo,
                sessionId,
                runId,
                plan,
                confirmed ? "PLAN_APPROVE" : "PLAN_REJECT",
                Optional.ofNullable(planExitConfirm.getToolCall()).map(ToolUseBlock::getId).orElse(null),
                beforeStatus,
                plan.getStatus(),
                Map.of(
                        "replyId", nullToEmpty(event.getReplyId()),
                        "confirmed", confirmed,
                        "toolCall", toolUsePayload(planExitConfirm.getToolCall())
                ),
                true,
                null
        );

        return snapshotPayload(confirmed ? "PLAN_APPROVE" : "PLAN_REJECT", TOOL_PLAN_EXIT,
                Optional.ofNullable(planExitConfirm.getToolCall()).map(ToolUseBlock::getId).orElse(null),
                plan,
                true);
    }

    private Map<String, Object> handleToolResultEnd(
            ToolResultEndEvent event,
            HarnessAgent agent,
            RuntimeContext runtimeContext,
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId,
            PlanRuntimeEventTracker tracker
    ) throws IOException {
        String toolName = normalizeToolName(
                StringUtils.hasText(event.getToolCallName())
                        ? event.getToolCallName()
                        : tracker.toolName(event.getToolCallId())
        );
        if (!isPlanTool(toolName)) {
            return null;
        }

        boolean success = event.getState() == ToolResultState.SUCCESS;
        AgentState agentState = currentAgentState(agent, runtimeContext);
        String rawInput = tracker.toolInput(event.getToolCallId());

        if (!success) {
            AiAgentPlanEntity plan = findLatestPlan(config, userInfo, sessionId);
            recordOp(
                    config,
                    userInfo,
                    sessionId,
                    runId,
                    plan,
                    opTypeForToolResult(toolName),
                    event.getToolCallId(),
                    plan == null ? null : plan.getStatus(),
                    plan == null ? null : plan.getStatus(),
                    rawInputPayload(rawInput),
                    false,
                    event.getState() == null ? "Tool failed" : event.getState().getValue()
            );
            return snapshotPayload(opTypeForToolResult(toolName), toolName, event.getToolCallId(), plan, false);
        }

        return switch (toolName) {
            case TOOL_PLAN_ENTER -> handlePlanEnterResult(
                    event,
                    agentState,
                    config,
                    userInfo,
                    sessionId,
                    runId,
                    rawInput
            );
            case TOOL_PLAN_WRITE -> handlePlanWriteResult(
                    event,
                    agentState,
                    config,
                    userInfo,
                    sessionId,
                    runId,
                    rawInput
            );
            case TOOL_PLAN_EXIT -> handlePlanExitResult(
                    event,
                    agentState,
                    config,
                    userInfo,
                    sessionId,
                    runId,
                    rawInput
            );
            case TOOL_TODO_WRITE -> handleTodoWriteResult(
                    event,
                    agentState,
                    config,
                    userInfo,
                    sessionId,
                    runId,
                    rawInput
            );
            default -> null;
        };
    }

    private Map<String, Object> handlePlanEnterResult(
            ToolResultEndEvent event,
            AgentState agentState,
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId,
            String rawInput
    ) {
        AiAgentPlanEntity plan = ensurePlanForCurrentSession(
                config,
                userInfo,
                sessionId,
                runId,
                agentState,
                true
        );
        String beforeStatus = plan.getStatus();
        plan.setStatus("DRAFT");
        plan.setPlanFilePath(resolvePlanRelativePath(config, agentState));
        plan.setRunId(runId);
        touchUpdate(plan, userInfo);
        planService.updateById(plan);

        recordOp(
                config,
                userInfo,
                sessionId,
                runId,
                plan,
                "PLAN_ENTER",
                event.getToolCallId(),
                beforeStatus,
                plan.getStatus(),
                rawInputPayload(rawInput),
                true,
                null
        );

        return snapshotPayload("PLAN_ENTER", TOOL_PLAN_ENTER, event.getToolCallId(), plan, true);
    }

    private Map<String, Object> handlePlanWriteResult(
            ToolResultEndEvent event,
            AgentState agentState,
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId,
            String rawInput
    ) throws IOException {
        AiAgentPlanEntity plan = ensurePlanForCurrentSession(
                config,
                userInfo,
                sessionId,
                runId,
                agentState,
                false
        );
        String beforeStatus = plan.getStatus();
        String relativePath = resolvePlanRelativePath(config, agentState);
        String content = readPlanContent(config, relativePath);
        if (!StringUtils.hasText(content)) {
            content = extractPlanContent(rawInput);
        }

        plan.setStatus("DRAFT");
        plan.setRunId(runId);
        plan.setPlanFilePath(relativePath);
        plan.setPlanContent(content);
        plan.setTitle(extractTitle(content, plan.getPlanNo()));
        plan.setGoal(extractGoal(content));
        touchUpdate(plan, userInfo);
        planService.updateById(plan);

        recordOp(
                config,
                userInfo,
                sessionId,
                runId,
                plan,
                "PLAN_WRITE",
                event.getToolCallId(),
                beforeStatus,
                plan.getStatus(),
                Map.of(
                        "input", nullToEmpty(truncate(rawInput)),
                        "planFilePath", nullToEmpty(relativePath),
                        "contentBytes", content == null ? 0 : content.getBytes(StandardCharsets.UTF_8).length
                ),
                true,
                null
        );

        return snapshotPayload("PLAN_WRITE", TOOL_PLAN_WRITE, event.getToolCallId(), plan, true);
    }

    private Map<String, Object> handlePlanExitResult(
            ToolResultEndEvent event,
            AgentState agentState,
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId,
            String rawInput
    ) {
        AiAgentPlanEntity plan = ensurePlanForCurrentSession(
                config,
                userInfo,
                sessionId,
                runId,
                agentState,
                false
        );
        String beforeStatus = plan.getStatus();
        plan.setStatus("EXECUTING");
        plan.setRunId(runId);
        if (plan.getStartedAt() == null) {
            plan.setStartedAt(LocalDateTime.now());
        }
        touchUpdate(plan, userInfo);
        planService.updateById(plan);

        recordOp(
                config,
                userInfo,
                sessionId,
                runId,
                plan,
                "PLAN_STATUS_CHANGE",
                event.getToolCallId(),
                beforeStatus,
                plan.getStatus(),
                rawInputPayload(rawInput),
                true,
                null
        );

        return snapshotPayload("PLAN_EXECUTING", TOOL_PLAN_EXIT, event.getToolCallId(), plan, true);
    }

    private Map<String, Object> handleTodoWriteResult(
            ToolResultEndEvent event,
            AgentState agentState,
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId,
            String rawInput
    ) {
        AiAgentPlanEntity plan = ensurePlanForCurrentSession(
                config,
                userInfo,
                sessionId,
                runId,
                agentState,
                false
        );
        String beforeStatus = plan.getStatus();
        List<AiAgentPlanTaskEntity> tasks = syncTasksFromAgentState(plan, agentState, userInfo, sessionId, runId);
        updatePlanStatusFromTasks(plan, tasks, userInfo, runId);

        recordOp(
                config,
                userInfo,
                sessionId,
                runId,
                plan,
                "TODO_WRITE",
                event.getToolCallId(),
                beforeStatus,
                plan.getStatus(),
                Map.of(
                        "input", nullToEmpty(truncate(rawInput)),
                        "taskCount", tasks.size()
                ),
                true,
                null
        );

        return snapshotPayload("TODO_WRITE", TOOL_TODO_WRITE, event.getToolCallId(), plan, true);
    }

    private List<AiAgentPlanTaskEntity> syncTasksFromAgentState(
            AiAgentPlanEntity plan,
            AgentState agentState,
            UserInfo userInfo,
            Long sessionId,
            Long runId
    ) {
        List<Task> stateTasks = agentState == null || agentState.getTasksContext() == null
                ? List.of()
                : agentState.getTasksContext().getTasks();

        List<AiAgentPlanTaskEntity> oldTasks = planTaskService.list(new LambdaQueryWrapper<AiAgentPlanTaskEntity>()
                .eq(AiAgentPlanTaskEntity::getPlanId, plan.getId())
                .orderByAsc(AiAgentPlanTaskEntity::getTaskIndex));
        Map<String, AiAgentPlanTaskEntity> oldTaskMap = oldTasks.stream()
                .collect(Collectors.toMap(
                        task -> taskKey(task.getTaskIndex(), task.getSubject()),
                        Function.identity(),
                        (left, right) -> left
                ));

        planTaskService.remove(new LambdaQueryWrapper<AiAgentPlanTaskEntity>()
                .eq(AiAgentPlanTaskEntity::getPlanId, plan.getId()));

        List<AiAgentPlanTaskEntity> entities = new ArrayList<>();
        for (int index = 0; index < stateTasks.size(); index++) {
            Task task = stateTasks.get(index);
            String subject = task.getSubject();
            String state = task.getState() == null ? "PENDING" : task.getState().name();
            AiAgentPlanTaskEntity oldTask = oldTaskMap.get(taskKey(index + 1, subject));

            AiAgentPlanTaskEntity entity = new AiAgentPlanTaskEntity();
            entity.setTenantId(userInfo.getTenantId());
            entity.setCreatedBy(userInfo.getUserId());
            entity.setUpdatedAt(LocalDateTime.now());
            entity.setCreatedAt(LocalDateTime.now());
            entity.setDeleted(0);
            entity.setVersion(0);
            entity.setPlanId(plan.getId());
            entity.setSessionId(sessionId);
            entity.setRunId(runId);
            entity.setTaskIndex(index + 1);
            entity.setSubject(subject);
            entity.setDetail(task.getDescription());
            entity.setState(state);
            entity.setOwner(task.getOwner());
            entity.setBlocksJson(toJson(task.getBlocks()));
            entity.setBlockedByJson(toJson(task.getBlockedBy()));
            entity.setEvidenceJson(toJson(Map.of(
                    "taskId", nullToEmpty(task.getId()),
                    "metadata", task.getMetadata() == null ? Map.of() : task.getMetadata(),
                    "createdAt", nullToEmpty(task.getCreatedAt())
            )));

            if ("IN_PROGRESS".equals(state)) {
                entity.setStartedAt(oldTask != null && oldTask.getStartedAt() != null
                        ? oldTask.getStartedAt()
                        : LocalDateTime.now());
            } else if (oldTask != null) {
                entity.setStartedAt(oldTask.getStartedAt());
            }

            if ("COMPLETED".equals(state)) {
                entity.setStartedAt(oldTask != null && oldTask.getStartedAt() != null
                        ? oldTask.getStartedAt()
                        : LocalDateTime.now());
                entity.setFinishedAt(oldTask != null && "COMPLETED".equals(oldTask.getState()) && oldTask.getFinishedAt() != null
                        ? oldTask.getFinishedAt()
                        : LocalDateTime.now());
            }

            entities.add(entity);
        }

        if (!entities.isEmpty()) {
            planTaskService.saveBatch(entities);
        }
        return entities;
    }

    private void updatePlanStatusFromTasks(
            AiAgentPlanEntity plan,
            List<AiAgentPlanTaskEntity> tasks,
            UserInfo userInfo,
            Long runId
    ) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        boolean allCompleted = tasks.stream().allMatch(task -> "COMPLETED".equals(task.getState()));
        boolean hasStarted = tasks.stream().anyMatch(task ->
                "IN_PROGRESS".equals(task.getState()) || "COMPLETED".equals(task.getState()));

        if (allCompleted) {
            plan.setStatus("COMPLETED");
            if (plan.getFinishedAt() == null) {
                plan.setFinishedAt(LocalDateTime.now());
            }
        } else if (hasStarted && !"WAITING_APPROVAL".equals(plan.getStatus()) && !"REJECTED".equals(plan.getStatus())) {
            plan.setStatus("EXECUTING");
            if (plan.getStartedAt() == null) {
                plan.setStartedAt(LocalDateTime.now());
            }
        }

        plan.setRunId(runId);
        touchUpdate(plan, userInfo);
        planService.updateById(plan);
    }

    private AiAgentPlanEntity ensurePlanForCurrentSession(
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId,
            AgentState agentState,
            boolean forceDraftReusable
    ) {
        AiAgentPlanEntity latest = findLatestPlan(config, userInfo, sessionId);
        if (latest != null && (!forceDraftReusable || REUSABLE_PLAN_STATUSES.contains(nullToEmpty(latest.getStatus())))) {
            latest.setRunId(runId);
            if (!StringUtils.hasText(latest.getPlanFilePath())) {
                latest.setPlanFilePath(resolvePlanRelativePath(config, agentState));
            }
            touchUpdate(latest, userInfo);
            return latest;
        }

        AiAgentPlanEntity plan = new AiAgentPlanEntity();
        plan.setTenantId(userInfo.getTenantId());
        plan.setUserId(userInfo.getUserId());
        plan.setAgentId(config.getAgentId());
        plan.setAgentConfigId(config.getAgentConfigId());
        plan.setSessionId(sessionId);
        plan.setRunId(runId);
        plan.setPlanNo(generatePlanNo(sessionId));
        plan.setTitle("Plan " + sessionId);
        plan.setGoal(config.getAgentName());
        plan.setStatus("DRAFT");
        plan.setPlanFilePath(resolvePlanRelativePath(config, agentState));
        plan.setRiskLevel("MEDIUM");
        plan.setCreatedBy(userInfo.getUserId());
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        plan.setDeleted(0);
        plan.setVersion(0);
        planService.save(plan);
        return plan;
    }

    private AiAgentPlanEntity findLatestPlan(AgentConfigDTO config, UserInfo userInfo, Long sessionId) {
        return planService.getOne(new LambdaQueryWrapper<AiAgentPlanEntity>()
                        .eq(AiAgentPlanEntity::getTenantId, userInfo.getTenantId())
                        .eq(AiAgentPlanEntity::getUserId, userInfo.getUserId())
                        .eq(AiAgentPlanEntity::getAgentId, config.getAgentId())
                        .eq(AiAgentPlanEntity::getAgentConfigId, config.getAgentConfigId())
                        .eq(AiAgentPlanEntity::getSessionId, sessionId)
                        .orderByDesc(AiAgentPlanEntity::getId)
                        .last("LIMIT 1"),
                false);
    }

    private void recordOp(
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId,
            AiAgentPlanEntity plan,
            String opType,
            String toolCallId,
            String beforeStatus,
            String afterStatus,
            Object payload,
            boolean success,
            String errorMessage
    ) {
        AiAgentPlanOpLogEntity opLog = new AiAgentPlanOpLogEntity();
        opLog.setTenantId(userInfo.getTenantId());
        opLog.setUserId(userInfo.getUserId());
        opLog.setAgentId(config.getAgentId());
        opLog.setAgentConfigId(config.getAgentConfigId());
        opLog.setSessionId(sessionId);
        opLog.setRunId(runId);
        opLog.setPlanId(plan == null ? null : plan.getId());
        opLog.setOpType(opType);
        opLog.setToolCallId(toolCallId);
        opLog.setBeforeStatus(beforeStatus);
        opLog.setAfterStatus(afterStatus);
        opLog.setPayloadJson(toJson(payload));
        opLog.setSuccess((byte) (success ? 1 : 0));
        opLog.setErrorMessage(errorMessage);
        opLog.setCreatedBy(userInfo.getUserId());
        opLog.setCreatedAt(LocalDateTime.now());
        opLog.setUpdatedAt(LocalDateTime.now());
        opLog.setDeleted(0);
        opLog.setVersion(0);
        planOpLogService.save(opLog);
    }

    private Map<String, Object> snapshotPayload(
            String type,
            String toolName,
            String toolCallId,
            AiAgentPlanEntity plan,
            boolean includeContent
    ) {
        Map<String, Object> payload = lightweightPayload(type, toolName, toolCallId);
        if (plan == null) {
            payload.put("progress", progressPayload(List.of()));
            payload.put("tasks", List.of());
            return payload;
        }

        List<AiAgentPlanTaskEntity> tasks = planTaskService.list(new LambdaQueryWrapper<AiAgentPlanTaskEntity>()
                .eq(AiAgentPlanTaskEntity::getPlanId, plan.getId())
                .orderByAsc(AiAgentPlanTaskEntity::getTaskIndex));
        payload.put("plan", planPayload(plan, includeContent));
        payload.put("tasks", tasks.stream().map(this::taskPayload).toList());
        payload.put("progress", progressPayload(tasks));
        return payload;
    }

    private Map<String, Object> lightweightPayload(String type, String toolName, String toolCallId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", type);
        payload.put("toolName", toolName);
        payload.put("toolCallId", toolCallId);
        payload.put("occurredAt", DISPLAY_TIME_FORMATTER.format(LocalDateTime.now()));
        return payload;
    }

    private Map<String, Object> planPayload(AiAgentPlanEntity plan, boolean includeContent) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", plan.getId());
        payload.put("planNo", plan.getPlanNo());
        payload.put("title", plan.getTitle());
        payload.put("goal", plan.getGoal());
        payload.put("status", plan.getStatus());
        payload.put("planFilePath", plan.getPlanFilePath());
        payload.put("riskLevel", plan.getRiskLevel());
        payload.put("riskSummary", plan.getRiskSummary());
        payload.put("expectedResult", plan.getExpectedResult());
        payload.put("approvedAt", formatTime(plan.getApprovedAt()));
        payload.put("startedAt", formatTime(plan.getStartedAt()));
        payload.put("finishedAt", formatTime(plan.getFinishedAt()));
        if (includeContent) {
            payload.put("planContent", plan.getPlanContent());
        }
        return payload;
    }

    private Map<String, Object> taskPayload(AiAgentPlanTaskEntity task) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", task.getId());
        payload.put("planId", task.getPlanId());
        payload.put("taskIndex", task.getTaskIndex());
        payload.put("subject", task.getSubject());
        payload.put("detail", task.getDetail());
        payload.put("state", task.getState());
        payload.put("owner", task.getOwner());
        payload.put("blocks", fromJsonList(task.getBlocksJson()));
        payload.put("blockedBy", fromJsonList(task.getBlockedByJson()));
        payload.put("startedAt", formatTime(task.getStartedAt()));
        payload.put("finishedAt", formatTime(task.getFinishedAt()));
        return payload;
    }

    private Map<String, Object> progressPayload(List<AiAgentPlanTaskEntity> tasks) {
        int total = tasks == null ? 0 : tasks.size();
        long pending = tasks == null ? 0 : tasks.stream().filter(task -> "PENDING".equals(task.getState())).count();
        long inProgress = tasks == null ? 0 : tasks.stream().filter(task -> "IN_PROGRESS".equals(task.getState())).count();
        long completed = tasks == null ? 0 : tasks.stream().filter(task -> "COMPLETED".equals(task.getState())).count();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("total", total);
        payload.put("pending", pending);
        payload.put("inProgress", inProgress);
        payload.put("completed", completed);
        payload.put("percent", total == 0 ? 0 : Math.round((completed * 100.0) / total));
        return payload;
    }

    private AgentState currentAgentState(HarnessAgent agent, RuntimeContext runtimeContext) {
        if (agent == null) {
            return null;
        }
        try {
            if (agent.getDelegate() != null) {
                return agent.getDelegate().getAgentState(runtimeContext);
            }
        } catch (Exception ex) {
            log.debug("Get scoped agent state failed, fallback to default state", ex);
        }
        try {
            return agent.getAgentState();
        } catch (Exception ex) {
            log.debug("Get default agent state failed", ex);
            return null;
        }
    }

    private String resolvePlanRelativePath(AgentConfigDTO config, AgentState agentState) {
        String currentPlanFile = agentState != null && agentState.getPlanModeContext() != null
                ? agentState.getPlanModeContext().getCurrentPlanFile()
                : null;
        String candidate = StringUtils.hasText(currentPlanFile)
                ? currentPlanFile
                : Optional.ofNullable(config.getPlanFileDirectory())
                .filter(StringUtils::hasText)
                .orElse(DEFAULT_PLAN_DIR) + "/" + DEFAULT_PLAN_FILE;

        Path workspaceRoot = Paths.get(config.getWorkspacePath()).toAbsolutePath().normalize();
        Path candidatePath = Paths.get(candidate);
        Path absolutePath = candidatePath.isAbsolute()
                ? candidatePath.normalize()
                : workspaceRoot.resolve(candidate).normalize();

        if (!absolutePath.startsWith(workspaceRoot)) {
            return Optional.ofNullable(config.getPlanFileDirectory())
                    .filter(StringUtils::hasText)
                    .orElse(DEFAULT_PLAN_DIR) + "/" + DEFAULT_PLAN_FILE;
        }
        return workspaceRoot.relativize(absolutePath).toString().replace('\\', '/');
    }

    private String readPlanContent(AgentConfigDTO config, String relativePath) throws IOException {
        if (!StringUtils.hasText(config.getWorkspacePath()) || !StringUtils.hasText(relativePath)) {
            return "";
        }
        Path workspaceRoot = Paths.get(config.getWorkspacePath()).toAbsolutePath().normalize();
        Path targetPath = workspaceRoot.resolve(relativePath).normalize();
        if (!targetPath.startsWith(workspaceRoot) || !Files.isRegularFile(targetPath)) {
            return "";
        }
        return Files.readString(targetPath, StandardCharsets.UTF_8);
    }

    private String extractPlanContent(String rawInput) {
        if (!StringUtils.hasText(rawInput)) {
            return "";
        }
        try {
            Map<String, Object> input = objectMapper.readValue(rawInput, new TypeReference<>() {
            });
            Object content = input.get("content");
            return content == null ? "" : content.toString();
        } catch (Exception ignored) {
            return rawInput;
        }
    }

    private String extractTitle(String content, String fallback) {
        if (StringUtils.hasText(content)) {
            for (String line : content.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("#")) {
                    String title = trimmed.replaceFirst("^#+\\s*", "").trim();
                    if (StringUtils.hasText(title)) {
                        return truncate(title, 200);
                    }
                }
            }
        }
        return StringUtils.hasText(fallback) ? fallback : "Plan";
    }

    private String extractGoal(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isBlank() || trimmed.startsWith("#")) {
                continue;
            }
            return truncate(trimmed, 500);
        }
        return "";
    }

    private ToolUseBlock firstToolCall(List<ToolUseBlock> toolCalls, String expectedName) {
        if (toolCalls == null) {
            return null;
        }
        return toolCalls.stream()
                .filter(Objects::nonNull)
                .filter(toolCall -> expectedName.equals(normalizeToolName(toolCall.getName())))
                .findFirst()
                .orElse(null);
    }

    private ConfirmResult firstPlanExitConfirm(List<ConfirmResult> confirmResults) {
        if (confirmResults == null) {
            return null;
        }
        return confirmResults.stream()
                .filter(Objects::nonNull)
                .filter(confirmResult -> confirmResult.getToolCall() != null)
                .filter(confirmResult -> TOOL_PLAN_EXIT.equals(normalizeToolName(confirmResult.getToolCall().getName())))
                .findFirst()
                .orElse(null);
    }

    private Map<String, Object> toolUsePayload(ToolUseBlock toolUseBlock) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (toolUseBlock == null) {
            return payload;
        }
        payload.put("id", toolUseBlock.getId());
        payload.put("name", toolUseBlock.getName());
        payload.put("input", toolUseBlock.getInput());
        payload.put("content", toolUseBlock.getContent());
        payload.put("metadata", toolUseBlock.getMetadata());
        payload.put("state", toolUseBlock.getState() == null ? null : toolUseBlock.getState().getValue());
        return payload;
    }

    private Object rawInputPayload(String rawInput) {
        return Map.of("input", nullToEmpty(truncate(rawInput)));
    }

    private String normalizeToolName(String toolName) {
        return toolName == null ? "" : toolName.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isPlanTool(String toolName) {
        return PLAN_TOOL_NAMES.contains(normalizeToolName(toolName));
    }

    private String opTypeForToolStart(String toolName) {
        return switch (normalizeToolName(toolName)) {
            case TOOL_PLAN_ENTER -> "PLAN_ENTER_START";
            case TOOL_PLAN_WRITE -> "PLAN_WRITE_START";
            case TOOL_PLAN_EXIT -> "PLAN_EXIT_START";
            case TOOL_TODO_WRITE -> "TODO_WRITE_START";
            default -> "PLAN_TOOL_START";
        };
    }

    private String opTypeForToolResult(String toolName) {
        return switch (normalizeToolName(toolName)) {
            case TOOL_PLAN_ENTER -> "PLAN_ENTER";
            case TOOL_PLAN_WRITE -> "PLAN_WRITE";
            case TOOL_PLAN_EXIT -> "PLAN_STATUS_CHANGE";
            case TOOL_TODO_WRITE -> "TODO_WRITE";
            default -> "PLAN_TOOL";
        };
    }

    private String generatePlanNo(Long sessionId) {
        return "PLAN-" + PLAN_NO_FORMATTER.format(LocalDateTime.now()) + "-" + sessionId;
    }

    private void touchUpdate(AiAgentPlanEntity plan, UserInfo userInfo) {
        plan.setUpdatedAt(LocalDateTime.now());
        plan.setUpdateBy(userInfo.getUserId());
    }

    private String taskKey(Integer taskIndex, String subject) {
        return String.valueOf(taskIndex) + "::" + nullToEmpty(subject);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }

    private List<Object> fromJsonList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ignored) {
            return List.of(json);
        }
    }

    private String truncate(String value) {
        return truncate(value, MAX_OP_PAYLOAD_LENGTH);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String formatTime(LocalDateTime value) {
        return value == null ? null : DISPLAY_TIME_FORMATTER.format(value);
    }
}
