package com.zhiran.agent.factory.middleware;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.middleware.ModelCallInput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.function.Function;
@Slf4j
@Component
public class SystemPromptDebugMiddleware implements MiddlewareBase {

    @Override
    public Flux<AgentEvent> onModelCall(
            Agent agent,
            RuntimeContext ctx,
            ModelCallInput input,
            Function<ModelCallInput, Flux<AgentEvent>> next) {
        for (Msg msg : input.messages()) {
            if (msg.getRole() == MsgRole.SYSTEM) {
                String systemPrompt = msg.getTextContent();
                log.debug("========== FINAL SYSTEM PROMPT ==========");
                log.debug(systemPrompt);
                log.debug("=========================================");
            }
        }

        return next.apply(input);
    }
}