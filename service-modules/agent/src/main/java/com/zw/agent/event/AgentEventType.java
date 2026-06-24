package com.zw.agent.event;

import java.util.HashMap;
import java.util.Map;

/**
 * AgentScope Agent 事件类型枚举。
 *
 * <p>事件用于表示智能体执行过程中的增量进度，例如文本 token、工具调用片段、
 * 工具执行结果、模型调用、人机交互请求等。</p>
 */
public enum AgentEventType {

    /** 智能体开始新的回复。 */
    AGENT_START("AGENT_START", "智能体开始新的回复"),

    /** 智能体完成回复。 */
    AGENT_END("AGENT_END", "智能体完成回复"),

    /** 智能体结果事件；源码中定义，文档该小节未展开说明。 */
    AGENT_RESULT("AGENT_RESULT", "智能体结果事件"),

    /** 模型 API 调用开始，通常携带 modelName。 */
    MODEL_CALL_START("MODEL_CALL_START", "模型 API 调用开始"),

    /** 模型 API 调用完成，通常携带 inputTokens / outputTokens。 */
    MODEL_CALL_END("MODEL_CALL_END", "模型 API 调用完成"),

    /** 新的文本块开始。 */
    TEXT_BLOCK_START("TEXT_BLOCK_START", "新的文本块开始"),

    /** 增量文本内容到达。 */
    TEXT_BLOCK_DELTA("TEXT_BLOCK_DELTA", "增量文本内容到达"),

    /** 文本块完成。 */
    TEXT_BLOCK_END("TEXT_BLOCK_END", "文本块完成"),

    /** 思考内容块开始；结构与文本流式事件对应。 */
    THINKING_BLOCK_START("THINKING_BLOCK_START", "思考内容块开始"),

    /** 思考内容增量到达；结构与文本流式事件对应。 */
    THINKING_BLOCK_DELTA("THINKING_BLOCK_DELTA", "思考内容增量到达"),

    /** 思考内容块完成；结构与文本流式事件对应。 */
    THINKING_BLOCK_END("THINKING_BLOCK_END", "思考内容块完成"),

    /** 数据块开始，承载图片、音频、视频等二进制数据；通常携带 mediaType。 */
    DATA_BLOCK_START("DATA_BLOCK_START", "数据块开始"),

    /** 数据块增量到达；通常携带增量 base64 编码数据。 */
    DATA_BLOCK_DELTA("DATA_BLOCK_DELTA", "数据块增量到达"),

    /** 数据块完成。 */
    DATA_BLOCK_END("DATA_BLOCK_END", "数据块完成"),

    /** 智能体开始一次工具调用，通常携带 toolCallId 和 toolCallName。 */
    TOOL_CALL_START("TOOL_CALL_START", "工具调用开始"),

    /** 增量工具调用参数到达；delta 通常是 JSON 参数片段。 */
    TOOL_CALL_DELTA("TOOL_CALL_DELTA", "工具调用参数增量到达"),

    /** 工具调用参数完成。 */
    TOOL_CALL_END("TOOL_CALL_END", "工具调用参数完成"),

    /** 工具开始执行，通常携带 toolCallId 和 toolCallName。 */
    TOOL_RESULT_START("TOOL_RESULT_START", "工具开始执行"),

    /** 工具的增量文本输出。 */
    TOOL_RESULT_TEXT_DELTA("TOOL_RESULT_TEXT_DELTA", "工具文本结果增量到达"),

    /** 工具的二进制数据输出，通常包含 mediaType / data / url 字段。 */
    TOOL_RESULT_DATA_DELTA("TOOL_RESULT_DATA_DELTA", "工具数据结果增量到达"),

    /** 工具执行完成，通常携带最终状态。 */
    TOOL_RESULT_END("TOOL_RESULT_END", "工具执行完成"),

    /** 智能体达到最大推理-执行迭代次数。 */
    EXCEED_MAX_ITERS("EXCEED_MAX_ITERS", "达到最大推理-执行迭代次数"),

    /** 智能体暂停，等待用户确认。 */
    REQUIRE_USER_CONFIRM("REQUIRE_USER_CONFIRM", "等待用户确认"),

    /** 智能体暂停，等待外部系统执行。 */
    REQUIRE_EXTERNAL_EXECUTION("REQUIRE_EXTERNAL_EXECUTION", "等待外部执行"),

    /** 用户提供确认结果的输入事件，通常携带 List<ConfirmResult>。 */
    USER_CONFIRM_RESULT("USER_CONFIRM_RESULT", "用户确认结果"),

    /** 外部系统提供执行结果的输入事件，通常携带 List<ToolResultBlock>。 */
    EXTERNAL_EXECUTION_RESULT("EXTERNAL_EXECUTION_RESULT", "外部执行结果"),

    /** 中间件或工具发起的提前停止请求。 */
    REQUEST_STOP("REQUEST_STOP", "提前停止请求"),

    /** 子 Agent 被暴露为用户可寻址入口点。 */
    SUBAGENT_EXPOSED("SUBAGENT_EXPOSED", "子 Agent 暴露事件"),

    /** HintBlock 相关事件；源码中定义，文档该小节未展开说明。 */
    HINT_BLOCK("HINT_BLOCK", "提示块事件"),

    /** 自定义事件。 */
    CUSTOM("CUSTOM", "自定义事件");

    private static final Map<String, AgentEventType> VALUE_MAP = new HashMap<>();

    static {
        for (AgentEventType type : values()) {
            VALUE_MAP.put(type.value, type);
        }

        // 兼容历史别名
        VALUE_MAP.put("RUN_STARTED", AGENT_START);
        VALUE_MAP.put("REPLY_START", AGENT_START);
        VALUE_MAP.put("RUN_FINISHED", AGENT_END);
        VALUE_MAP.put("REPLY_END", AGENT_END);
        VALUE_MAP.put("MODEL_CALL_STARTED", MODEL_CALL_START);
        VALUE_MAP.put("MODEL_CALL_ENDED", MODEL_CALL_END);
        VALUE_MAP.put("BINARY_BLOCK_START", DATA_BLOCK_START);
        VALUE_MAP.put("BINARY_BLOCK_DELTA", DATA_BLOCK_DELTA);
        VALUE_MAP.put("BINARY_BLOCK_END", DATA_BLOCK_END);
        VALUE_MAP.put("TOOL_RESULT_BINARY_DELTA", TOOL_RESULT_DATA_DELTA);
        VALUE_MAP.put("THREAD_EXPOSED", SUBAGENT_EXPOSED);
    }

    private final String value;
    private final String description;

    AgentEventType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    /** 返回事件类型的规范字符串值。 */
    public String getValue() {
        return value;
    }

    /** 返回事件类型说明。 */
    public String getDescription() {
        return description;
    }

    /**
     * 根据规范值或历史别名解析事件类型。
     *
     * @param value 事件类型字符串
     * @return 对应的事件类型
     * @throws IllegalArgumentException 当 value 为空或未知时抛出
     */
    public static AgentEventType fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("AgentEventType value must not be null");
        }

        AgentEventType type = VALUE_MAP.get(value);
        if (type == null) {
            throw new IllegalArgumentException("Unknown AgentEventType value: " + value);
        }

        return type;
    }
}