package com.zw.agent.event;

import io.agentscope.core.event.AgentEvent;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AgentRuntimeEvent{
    // 回复ID
    String replyId;
    // 事件类型
    String eventType;
    // 事件内容
    String delta;
    // 原始事件对象
    AgentEvent rawEvent;
}