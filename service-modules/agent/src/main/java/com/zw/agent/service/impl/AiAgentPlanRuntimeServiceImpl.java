package com.zw.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
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
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

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

/**
 * 将 AgentScope Plan Mode 的工具事件同步到业务表，并生成前端可直接消费的 SSE 快照。
 * PLAN.md 仍由 AgentScope 框架负责写入，本服务只记录元数据并读取内容快照。
 */
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

    /**
     * 为单次流式调用创建事件跟踪器，用来缓存工具调用的名称和参数增量。
     */
    @Override
    public PlanRuntimeEventTracker newTracker() {
        return new PlanRuntimeEventTracker();
    }

    /**
     * 统一入口：接收 AgentScope 运行事件，分发给具体处理函数，并返回前端需要的 Plan 快照。
     */
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
            // 这里只做事件旁路监听，不改变 AgentScope 原本的计划、审批和执行流程。
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

    /**
     * 处理工具调用开始事件：只识别 Plan 白名单工具，并初始化本次工具调用的跟踪缓存。
     */
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

    /**
     * 处理工具参数增量事件：把 plan_write、plan_exit、todo_write 等工具的入参片段拼起来。
     */
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

    /**
     * 处理 plan_exit 触发的人工确认事件：把当前计划置为 WAITING_APPROVAL 并记录申请日志。
     */
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

        // plan_exit 会触发 ASK 审批；先把计划标记为待确认，等待用户批准或拒绝。
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

    /**
     * 处理用户确认结果：批准时进入 EXECUTING，拒绝时进入 REJECTED，并写入审批信息。
     */
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

        // 用户批准后进入执行阶段；拒绝后保持计划可继续修改。
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

    /**
     * 处理工具结果结束事件：根据工具名分派到 plan_enter、plan_write、plan_exit、todo_write 的落库逻辑。
     */
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

    /**
     * 处理 plan_enter 成功结果：创建或复用当前会话计划记录，并标记为 DRAFT。
     */
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
        String userGoal = extractLatestUserGoal(agentState);
        if (StringUtils.hasText(userGoal)) {
            plan.setTitle(buildTitleFromText(userGoal, plan.getTitle()));
            plan.setGoal(truncate(userGoal, 500));
        }
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

    /**
     * 处理 plan_write 成功结果：读取框架已经写好的 PLAN.md，并把内容、标题、目标快照到计划表。
     */
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
        // 不在业务层生成 PLAN.md：框架已完成写入，这里只读取文件内容做数据库快照。
        String content = readPlanContent(config, relativePath);
        if (!StringUtils.hasText(content)) {
            content = extractPlanContent(rawInput);
        }

        plan.setStatus("DRAFT");
        plan.setRunId(runId);
        plan.setPlanFilePath(relativePath);
        plan.setPlanContent(content);
        String userGoal = extractLatestUserGoal(agentState);
        String goal = extractGoal(content);
        if (!StringUtils.hasText(goal)) {
            goal = userGoal;
        }
        plan.setTitle(extractTitle(content, goal, userGoal, plan.getTitle()));
        plan.setGoal(StringUtils.hasText(goal) ? truncate(goal, 500) : config.getAgentName());
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

    /**
     * 处理 plan_exit 工具真正执行成功后的结果：确认 Agent 已进入执行阶段并刷新计划状态。
     */
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

    /**
     * 处理 todo_write 成功结果：从 AgentState 读取最新任务清单，同步到任务表并返回前端进度快照。
     */
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

    /**
     * 将 AgentState.tasksContext 中的任务全量镜像到 ai_agent_plan_task，并保留已开始/已完成时间。
     */
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

        // todo_write 是全量替换语义，数据库侧也按全量镜像处理，避免残留旧任务。
        List<AiAgentPlanTaskEntity> oldTasks = planTaskService.list(new LambdaQueryWrapper<AiAgentPlanTaskEntity>()
                .eq(AiAgentPlanTaskEntity::getPlanId, plan.getId())
                .orderByAsc(AiAgentPlanTaskEntity::getTaskIndex));
        Map<String, AiAgentPlanTaskEntity> oldTaskMap = oldTasks.stream()
                .collect(Collectors.toMap(
                        task -> taskKey(task.getTaskIndex(), task.getSubject()),
                        Function.identity(),
                        (left, right) -> left
                ));

        planTaskService.physicalDeleteByPlanId(plan.getId());

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

    /**
     * 根据任务完成情况反推计划执行状态：全部完成则 COMPLETED，有任务推进则 EXECUTING。
     */
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

    /**
     * 获取当前会话可用计划记录；没有记录时新建，已有草稿/待确认/已拒绝记录时复用。
     */
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
        String userGoal = extractLatestUserGoal(agentState);
        plan.setTitle(buildTitleFromText(userGoal, config.getAgentName()));
        plan.setGoal(StringUtils.hasText(userGoal) ? truncate(userGoal, 500) : config.getAgentName());
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

    /**
     * 查询当前用户、Agent 配置和会话下最近一条计划记录，作为本次 Plan 事件的归属。
     */
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

    /**
     * 写入计划操作日志，记录工具调用、状态变化、原始参数和成功/失败信息。
     */
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

    /**
     * 组装发送给前端的 Plan SSE 快照，包括计划元数据、任务列表和进度统计。
     */
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

    /**
     * 组装最基础的 Plan 事件 payload，供工具开始事件或完整快照复用。
     */
    private Map<String, Object> lightweightPayload(String type, String toolName, String toolCallId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", type);
        payload.put("toolName", toolName);
        payload.put("toolCallId", toolCallId);
        payload.put("occurredAt", DISPLAY_TIME_FORMATTER.format(LocalDateTime.now()));
        return payload;
    }

    /**
     * 将计划实体转换为前端展示字段；按需携带计划内容，避免不必要的大字段下发。
     */
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

    /**
     * 将任务实体转换为前端展示字段，同时把 blocks/blockedBy 的 JSON 还原成列表。
     */
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

    /**
     * 统计任务总数、待执行、执行中、已完成数量和完成百分比，供前端进度条使用。
     */
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

    /**
     * 获取当前 RuntimeContext 对应的 AgentState；读取失败时降级到 Agent 默认状态。
     */
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

    /**
     * 解析当前计划文件相对路径，并确保路径始终限制在 workspace 内。
     */
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

    /**
     * 从 workspace 中读取 AgentScope 已经生成的 PLAN.md 内容；路径不存在时返回空字符串。
     */
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

    /**
     * 当文件快照读取不到时，从 plan_write 原始入参里兜底提取 content 字段。
     */
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

    /**
     * 从计划内容和用户诉求中提取计划标题。
     * 优先使用 PLAN.md 里的 Markdown 标题，其次使用计划正文第一段，再用目标、用户最新需求和旧标题兜底；
     * 这样 plan_enter 和 plan_write 的前端标题都会贴近实际计划内容，而不是显示 sessionId 或计划编号。
     */
    private String extractTitle(String content, String goalFallback, String userGoalFallback, String fallback) {
        return firstTitleCandidate(
                extractMarkdownHeading(content),
                extractFirstMeaningfulLine(content),
                goalFallback,
                userGoalFallback,
                fallback
        );
    }

    /**
     * 从一段文本生成适合列表展示的短标题。
     * plan_enter 阶段还没有 PLAN.md 内容，因此会用用户最新输入先生成临时标题，等 plan_write 后再由计划内容覆盖。
     */
    private String buildTitleFromText(String text, String fallback) {
        return firstTitleCandidate(text, fallback);
    }

    /**
     * 按顺序挑选第一个有效标题候选。
     * 这里会跳过“Plan”“计划”“PLAN-日期-sessionId”等泛化标题，避免前端继续展示无业务含义的默认值。
     */
    private String firstTitleCandidate(String... candidates) {
        if (candidates != null) {
            for (String candidate : candidates) {
                String title = normalizeTitleCandidate(candidate);
                if (StringUtils.hasText(title) && !isGenericPlanTitle(title)) {
                    return truncate(title, 200);
                }
            }
        }
        return "计划";
    }

    /**
     * 提取 PLAN.md 中第一个 Markdown 标题。
     * 如果标题只是“Plan”“计划”等通用词，后续 firstTitleCandidate 会继续寻找更具体的正文或用户诉求。
     */
    private String extractMarkdownHeading(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#")) {
                return normalizeTitleCandidate(trimmed);
            }
        }
        return "";
    }

    /**
     * 从 PLAN.md 正文中提取第一行有信息量的内容。
     * 这用于模型没有写 Markdown 标题，或者只写了通用标题时，仍然能从计划内容里得到可读标题。
     */
    private String extractFirstMeaningfulLine(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        for (String line : content.split("\\R")) {
            String title = normalizeTitleLine(line);
            if (StringUtils.hasText(title) && !isGenericPlanTitle(title)) {
                return title;
            }
        }
        return "";
    }

    /**
     * 标准化标题候选文本。
     * 多行文本会从上到下选第一行有效内容，同时过滤代码块、分隔线、Authorization token 等不适合展示为标题的内容。
     */
    private String normalizeTitleCandidate(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        for (String line : value.split("\\R")) {
            String title = normalizeTitleLine(line);
            if (StringUtils.hasText(title)) {
                return title;
            }
        }
        return "";
    }

    /**
     * 标准化单行标题文本。
     * 会去掉 Markdown 标题符号、列表序号、常见“目标：/标题：”前缀和多余空白，保留真正能说明计划意图的部分。
     */
    private String normalizeTitleLine(String line) {
        if (!StringUtils.hasText(line)) {
            return "";
        }
        String title = line.trim();
        if (title.startsWith("```") || title.matches("^-{3,}$") || title.matches("^={3,}$")) {
            return "";
        }
        String lowerTitle = title.toLowerCase(Locale.ROOT);
        if (lowerTitle.startsWith("authorization:")
                || lowerTitle.startsWith("bearer ")
                || lowerTitle.contains(" bearer ")) {
            return "";
        }
        if (title.endsWith(":") || title.endsWith("：")) {
            return "";
        }
        title = title.replaceFirst("^#+\\s*", "");
        title = title.replaceFirst("^[-*+>]\\s*", "");
        title = title.replaceFirst("^\\d+[.)、]\\s*", "");
        title = title.replace("**", "").replace("__", "").trim();
        title = stripTitleLabelPrefix(title);
        title = title.replaceAll("\\s+", " ").trim();
        return truncate(title, 200);
    }

    /**
     * 去掉标题候选里的常见标签前缀。
     * 例如“目标：修复计划模式权限”会被整理成“修复计划模式权限”，避免所有计划标题都显示为“目标”。
     */
    private String stripTitleLabelPrefix(String title) {
        if (!StringUtils.hasText(title)) {
            return "";
        }
        int colonIndex = title.indexOf('：');
        if (colonIndex < 0) {
            colonIndex = title.indexOf(':');
        }
        if (colonIndex <= 0 || colonIndex > 12) {
            return title;
        }
        String label = title.substring(0, colonIndex).trim().toLowerCase(Locale.ROOT);
        if (Set.of("目标", "计划目标", "任务", "需求", "标题", "goal", "objective", "title", "plan").contains(label)) {
            return title.substring(colonIndex + 1).trim();
        }
        return title;
    }

    /**
     * 判断标题候选是否只是系统默认值或泛化描述。
     * 这些标题没有业务信息量，需要继续使用计划正文或用户诉求生成更贴近内容的标题。
     */
    private boolean isGenericPlanTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return true;
        }
        String normalized = title.trim().toLowerCase(Locale.ROOT);
        if (Set.of("plan", "plan.md", "plans", "计划", "工作计划", "执行计划", "计划模式", "goal", "objective").contains(normalized)) {
            return true;
        }
        return normalized.matches("^plan\\s*\\d+$")
                || normalized.matches("^plan[-_\\s]*\\d{8,}.*")
                || normalized.matches("^plan[-_\\s]*session.*")
                || normalized.matches("^\\d{4}[-_/]?\\d{2}[-_/]?\\d{2}.*session.*");
    }

    /**
     * 从 Markdown 计划内容中提取第一行正文作为目标摘要，便于列表和详情展示。
     */
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

    /**
     * 从 AgentState 的上下文里读取最近一条用户消息，作为 plan_enter 阶段的计划标题和目标兜底。
     * plan_enter 执行时 PLAN.md 通常还没有写入，所以需要先用用户本轮需求给前端一个有业务含义的临时标题。
     */
    private String extractLatestUserGoal(AgentState agentState) {
        if (agentState == null || agentState.getContext() == null) {
            return "";
        }
        List<Msg> context = agentState.getContext();
        for (int index = context.size() - 1; index >= 0; index--) {
            Msg msg = context.get(index);
            if (msg == null || !MsgRole.USER.equals(msg.getRole())) {
                continue;
            }
            String content = cleanUserGoalContent(msg.getTextContent());
            if (StringUtils.hasText(content)) {
                return truncate(content, 500);
            }
        }
        return "";
    }

    /**
     * 清理用户消息内容，去掉空行和 Authorization/Bearer token。
     * 计划标题会进入前端展示和数据库，不能把令牌这类敏感内容误当成标题或目标保存。
     */
    private String cleanUserGoalContent(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (lower.startsWith("authorization:")
                    || lower.startsWith("bearer ")
                    || lower.contains(" bearer ")) {
                continue;
            }
            lines.add(trimmed);
        }
        return truncate(String.join("\n", lines), 500);
    }

    /**
     * 从一组待确认工具调用中找到指定名称的工具，主要用于定位 plan_exit。
     */
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

    /**
     * 从用户确认结果中找到 plan_exit 对应的确认结果，用于判断计划是否获批。
     */
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

    /**
     * 将 AgentScope 的 ToolUseBlock 转成可序列化 Map，写日志和 SSE payload 都复用该结构。
     */
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

    /**
     * 组装原始工具入参日志 payload，并做长度截断，避免大参数撑爆日志字段。
     */
    private Object rawInputPayload(String rawInput) {
        return Map.of("input", nullToEmpty(truncate(rawInput)));
    }

    /**
     * 统一工具名大小写和空白处理，保证事件匹配稳定。
     */
    private String normalizeToolName(String toolName) {
        return toolName == null ? "" : toolName.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 判断工具名是否属于 Plan Mode 相关工具集合。
     */
    private boolean isPlanTool(String toolName) {
        return PLAN_TOOL_NAMES.contains(normalizeToolName(toolName));
    }

    /**
     * 将工具开始事件映射成操作日志类型，方便前端和日志区分阶段。
     */
    private String opTypeForToolStart(String toolName) {
        return switch (normalizeToolName(toolName)) {
            case TOOL_PLAN_ENTER -> "PLAN_ENTER_START";
            case TOOL_PLAN_WRITE -> "PLAN_WRITE_START";
            case TOOL_PLAN_EXIT -> "PLAN_EXIT_START";
            case TOOL_TODO_WRITE -> "TODO_WRITE_START";
            default -> "PLAN_TOOL_START";
        };
    }

    /**
     * 将工具结果事件映射成业务操作类型，供操作日志和 SSE 快照使用。
     */
    private String opTypeForToolResult(String toolName) {
        return switch (normalizeToolName(toolName)) {
            case TOOL_PLAN_ENTER -> "PLAN_ENTER";
            case TOOL_PLAN_WRITE -> "PLAN_WRITE";
            case TOOL_PLAN_EXIT -> "PLAN_STATUS_CHANGE";
            case TOOL_TODO_WRITE -> "TODO_WRITE";
            default -> "PLAN_TOOL";
        };
    }

    /**
     * 生成平台内可读的计划编号，包含时间和 sessionId，便于排查会话内计划记录。
     */
    private String generatePlanNo(Long sessionId) {
        return "PLAN-" + PLAN_NO_FORMATTER.format(LocalDateTime.now()) + "-" + sessionId;
    }

    /**
     * 刷新计划实体的通用更新时间和更新人字段。
     */
    private void touchUpdate(AiAgentPlanEntity plan, UserInfo userInfo) {
        plan.setUpdatedAt(LocalDateTime.now());
        plan.setUpdateBy(userInfo.getUserId());
    }

    /**
     * 生成任务去重键，用于全量替换任务时继承旧任务的开始/完成时间。
     */
    private String taskKey(Integer taskIndex, String subject) {
        return String.valueOf(taskIndex) + "::" + nullToEmpty(subject);
    }

    /**
     * 将对象序列化成 JSON 字符串；序列化失败时退化为 toString，避免日志流程中断。
     */
    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        return objectMapper.writeValueAsString(value);
    }

    /**
     * 将数据库中的 JSON 数组字符串还原成列表；解析失败时保留原字符串作为单项列表。
     */
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

    /**
     * 使用默认日志最大长度截断字符串。
     */
    private String truncate(String value) {
        return truncate(value, MAX_OP_PAYLOAD_LENGTH);
    }

    /**
     * 按指定最大长度截断字符串，保护日志字段和 SSE payload 不被超大内容撑大。
     */
    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    /**
     * 将 null 字符串统一转为空字符串，简化 Map.of 等不允许 null 的场景。
     */
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * 将 LocalDateTime 格式化成前端展示用的固定时间字符串。
     */
    private String formatTime(LocalDateTime value) {
        return value == null ? null : DISPLAY_TIME_FORMATTER.format(value);
    }
}
