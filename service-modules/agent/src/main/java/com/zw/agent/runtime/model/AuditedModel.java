package com.zw.agent.runtime.model;

import com.zw.agent.entity.AiModelCallLogEntity;
import com.zw.agent.runtime.AgentRuntimeKeys;
import com.zw.agent.service.ModelCallAuditService;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.ChatUsage;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

public final class AuditedModel implements Model {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditedModel.class);

    private final Model delegate;
    private final ModelCallAuditService auditService;
    private final ModelAuditDescriptor descriptor;

    public AuditedModel(
            Model delegate,
            ModelCallAuditService auditService,
            ModelAuditDescriptor descriptor
    ) {
        this.delegate = delegate;
        this.auditService = auditService;
        this.descriptor = descriptor;
    }

    @Override
    public Flux<ChatResponse> stream(
            List<Msg> messages,
            List<ToolSchema> tools,
            GenerateOptions options
    ) {
        return Flux.deferContextual(context -> {
            Long runId = context.getOrDefault(AgentRuntimeKeys.MODEL_AUDIT_RUN_ID, null);
            Long sessionId = context.getOrDefault(AgentRuntimeKeys.MODEL_AUDIT_SESSION_ID, null);
            AiModelCallLogEntity log = startAudit(runId, sessionId);
            AtomicReference<ChatUsage> usage = new AtomicReference<>();
            AtomicBoolean finished = new AtomicBoolean(false);
            return delegate.stream(messages, tools, options)
                    .doOnNext(response -> {
                        if (response != null && response.getUsage() != null) {
                            usage.set(response.getUsage());
                        }
                    })
                    .doOnComplete(() -> {
                        if (finished.compareAndSet(false, true)) {
                            auditSafely(() -> auditService.success(
                                    descriptor,
                                    log,
                                    usage.get()
                            ));
                        }
                    })
                    .doOnError(error -> {
                        if (finished.compareAndSet(false, true)) {
                            auditSafely(() -> auditService.failed(descriptor, log, error));
                        }
                    })
                    .doOnCancel(() -> {
                        if (finished.compareAndSet(false, true)) {
                            auditSafely(() -> auditService.cancelled(descriptor, log));
                        }
                    });
        });
    }

    private AiModelCallLogEntity startAudit(Long runId, Long sessionId) {
        try {
            return auditService.start(
                    descriptor,
                    runId,
                    sessionId,
                    ModelCallAuditService.SOURCE_AGENT_RUN
            );
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Unable to start model call audit for config {}",
                    descriptor.modelConfigId(),
                    exception
            );
            return null;
        }
    }

    private void auditSafely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Unable to finish model call audit for config {}",
                    descriptor.modelConfigId(),
                    exception
            );
        }
    }

    @Override
    public String getModelName() {
        return delegate.getModelName();
    }

    @Override
    public boolean supportsNativeStructuredOutput() {
        return delegate.supportsNativeStructuredOutput();
    }

    @Override
    public boolean supportsNativeStructuredOutputWithTools() {
        return delegate.supportsNativeStructuredOutputWithTools();
    }

    @Override
    public int getContextWindowSize() {
        return delegate.getContextWindowSize();
    }
}
