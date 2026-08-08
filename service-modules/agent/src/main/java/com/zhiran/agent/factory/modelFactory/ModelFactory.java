package com.zhiran.agent.factory.modelFactory;

import com.zhiran.agent.constant.enumeration.HeaderSourceType;
import com.zhiran.agent.constant.enumeration.ModelProtocol;
import com.zhiran.agent.entity.AiHttpHeaderEntity;
import com.zhiran.agent.entity.DTO.AgentConfigDTO;
import com.zhiran.agent.runtime.model.AuditedModel;
import com.zhiran.agent.runtime.model.AnthropicModelClientConfigurer;
import com.zhiran.agent.runtime.model.HeaderInjectingHttpTransport;
import com.zhiran.agent.runtime.model.ModelAuditDescriptor;
import com.zhiran.agent.service.AiHttpHeaderService;
import com.zhiran.agent.service.ModelCallAuditService;
import com.zhiran.common.context.UserInfo;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.transport.HttpTransportFactory;
import io.agentscope.extensions.model.anthropic.AnthropicChatModel;
import io.agentscope.extensions.model.anthropic.formatter.AnthropicChatFormatter;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.extensions.model.dashscope.formatter.DashScopeChatFormatter;
import io.agentscope.extensions.model.ollama.OllamaChatModel;
import io.agentscope.extensions.model.ollama.formatter.OllamaChatFormatter;
import io.agentscope.extensions.model.ollama.options.OllamaOptions;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.extensions.model.openai.formatter.OpenAIChatFormatter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ModelFactory {

    private final AiHttpHeaderService httpHeaderService;
    private final ModelCallAuditService auditService;

    public Model buildRuntimeModel(
            AgentConfigDTO config,
            UserInfo userInfo,
            String sourcePath
    ) {
        List<AiHttpHeaderEntity> headers = modelHeaders(config);
        Model delegate = buildModel(config, headers);
        List<String> secrets = new ArrayList<>();
        if (hasText(config.getApiKey())) {
            secrets.add(config.getApiKey());
        }
        headers.stream()
                .map(AiHttpHeaderEntity::getHeaderValue)
                .filter(this::hasText)
                .forEach(secrets::add);
        ModelAuditDescriptor descriptor = new ModelAuditDescriptor(
                userInfo,
                config.getModelId(),
                config.getAgentId(),
                config.getAgentConfigId(),
                config.getModelConfigName(),
                config.getProtocol().getCode(),
                config.getModelName(),
                sourcePath,
                List.copyOf(secrets)
        );
        return new AuditedModel(delegate, auditService, descriptor);
    }

    public Model buildModel(AgentConfigDTO config) {
        return buildModel(config, modelHeaders(config));
    }

    public Model buildModel(
            AgentConfigDTO config,
            List<AiHttpHeaderEntity> headers
    ) {
        validate(config);
        GenerateOptions options = generateOptions(config, headers);
        ModelProtocol protocol = config.getProtocol();

        if (protocol == ModelProtocol.OPENAI_COMPATIBLE) {
            return OpenAIChatModel.builder()
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelName())
                    .baseUrl(config.getBaseUrl())
                    .stream(Boolean.TRUE.equals(config.getStreaming()))
                    .formatter(new OpenAIChatFormatter())
                    .generateOptions(options)
                    .build();
        }
        if (protocol == ModelProtocol.DASH_SCOPE) {
            return DashScopeChatModel.builder()
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelName())
                    .baseUrl(config.getBaseUrl())
                    .stream(Boolean.TRUE.equals(config.getStreaming()))
                    .enableThinking(Boolean.TRUE.equals(config.getThinking()))
                    .formatter(new DashScopeChatFormatter())
                    .defaultOptions(options)
                    .build();
        }
        if (protocol == ModelProtocol.ANTHROPIC) {
            AnthropicChatModel model = AnthropicChatModel.builder()
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelName())
                    .baseUrl(config.getBaseUrl())
                    .stream(Boolean.TRUE.equals(config.getStreaming()))
                    .formatter(new AnthropicChatFormatter())
                    .defaultOptions(options)
                    .build();
            return AnthropicModelClientConfigurer.configure(
                    model,
                    config.getBaseUrl(),
                    config.getApiKey(),
                    config.getTimeoutMs(),
                    headerMap(headers)
            );
        }
        if (protocol == ModelProtocol.OLLAMA) {
            OllamaChatModel.Builder builder = OllamaChatModel.builder()
                    .modelName(config.getModelName())
                    .baseUrl(config.getBaseUrl())
                    .formatter(new OllamaChatFormatter())
                    .defaultOptions(OllamaOptions.fromGenerateOptions(options));
            Map<String, String> headerMap = headerMap(headers);
            if (!headerMap.isEmpty()) {
                builder.httpTransport(new HeaderInjectingHttpTransport(
                        HttpTransportFactory.getDefault(),
                        headerMap
                ));
            }
            return builder.build();
        }
        throw new IllegalArgumentException("Unsupported model protocol");
    }

    private GenerateOptions generateOptions(
            AgentConfigDTO config,
            List<AiHttpHeaderEntity> headers
    ) {
        ExecutionConfig executionConfig = ExecutionConfig.builder()
                .timeout(Duration.ofMillis(config.getTimeoutMs()))
                .maxAttempts(config.getMaxAttempts())
                .build();
        GenerateOptions.Builder builder = GenerateOptions.builder()
                .stream(config.getStreaming())
                .temperature(config.getTemperature())
                .topP(config.getTopP())
                .maxTokens(config.getMaxTokens())
//                .maxCompletionTokens(config.getMaxTokens())
                .executionConfig(executionConfig);
        if (Boolean.TRUE.equals(config.getThinking())
                && config.getThinkingBudget() != null) {
            builder.thinkingBudget(config.getThinkingBudget());
        }
        Map<String, String> headerMap = headerMap(headers);
        if (!headerMap.isEmpty()) {
            builder.additionalHeaders(headerMap);
        }
        return builder.build();
    }

    private Map<String, String> headerMap(List<AiHttpHeaderEntity> headers) {
        Map<String, String> headerMap = new LinkedHashMap<>();
        for (AiHttpHeaderEntity header : headers) {
            if (hasText(header.getHeaderName()) && hasText(header.getHeaderValue())) {
                headerMap.put(header.getHeaderName(), header.getHeaderValue());
            }
        }
        return headerMap;
    }

    private List<AiHttpHeaderEntity> modelHeaders(AgentConfigDTO config) {
        if (config.getModelId() == null || config.getTenantId() == null) {
            return List.of();
        }
        return httpHeaderService.getHeaderList(
                config.getModelId(),
                config.getTenantId(),
                HeaderSourceType.MODEL
        );
    }

    private void validate(AgentConfigDTO config) {
        if (config == null
                || config.getProtocol() == null
                || !hasText(config.getBaseUrl())
                || !hasText(config.getModelName())) {
            throw new IllegalArgumentException("Incomplete model configuration");
        }
        if (config.getTimeoutMs() == null || config.getTimeoutMs() < 1000) {
            throw new IllegalArgumentException("Model timeout must be at least 1000 ms");
        }
        if (config.getMaxAttempts() == null || config.getMaxAttempts() < 1) {
            throw new IllegalArgumentException("Model maxAttempts must be at least 1");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
