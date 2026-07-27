package com.zw.agent.factory.modelFactory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zw.agent.entity.AiHttpHeaderEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.service.AiAgentModelService;
import com.zw.agent.service.AiHttpHeaderService;
import io.agentscope.core.model.*;
import io.agentscope.extensions.model.anthropic.AnthropicChatModel;
import io.agentscope.extensions.model.anthropic.formatter.AnthropicChatFormatter;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.extensions.model.dashscope.formatter.DashScopeChatFormatter;
import io.agentscope.extensions.model.ollama.OllamaChatModel;
import io.agentscope.extensions.model.ollama.formatter.OllamaChatFormatter;
import io.agentscope.extensions.model.ollama.options.OllamaOptions;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.extensions.model.openai.formatter.OpenAIChatFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class ModelFactory {

    private final AiHttpHeaderService httpHeaderService;


    public ChatModelBase buildModel(
            AgentConfigDTO config
    ){

        if (ModelType.OPENAI.getCode().equals(config.getProvider().getCode())) {
            GenerateOptions.Builder optionsBuilder = GenerateOptions.builder()
                    .temperature(config.getTemperature())
                    .maxCompletionTokens(config.getMaxTokens())
                    .topP(config.getTopP())
                    .thinkingBudget(config.getThinkingBudget())
                    .stream(config.getStreaming());

            List<AiHttpHeaderEntity> headerList = httpHeaderService.getHeaderlist(config.getModelId(),config.getTenantId());

            OpenAIChatModel.Builder builder = OpenAIChatModel.builder()
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelName())
                    .baseUrl(config.getBaseUrl())
                    .stream(config.getStreaming())
                    .formatter(new OpenAIChatFormatter());

            if (headerList != null){
                HashMap<String, String> headerMap = new HashMap<>();
                for (AiHttpHeaderEntity header : headerList){
                    headerMap.put(header.getHeaderName(), header.getHeaderValue());
                }
                builder.generateOptions(optionsBuilder.additionalHeaders(headerMap).build());
            }

            return builder.build();
        } else if (ModelType.DASH_SCOPE.getCode().equals(config.getProvider().getCode())) {
            return DashScopeChatModel.builder()
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelName())
                    .baseUrl(config.getBaseUrl())
                    .stream(config.getStreaming())
                    .formatter(new DashScopeChatFormatter())
                    .build();
        } else if (ModelType.OLLAMA.getCode().equals(config.getProvider().getCode())) {
            GenerateOptions.Builder optionsBuilder = GenerateOptions.builder();
            optionsBuilder.temperature(config.getTemperature())
                    .maxCompletionTokens(config.getMaxTokens())
                    .topP(config.getTopP())
                    .thinkingBudget(config.getThinkingBudget())
                    .stream(config.getStreaming());

            List<AiHttpHeaderEntity> headerList = httpHeaderService.getHeaderlist(config.getModelId(),config.getTenantId());


            OllamaChatModel.Builder builder = OllamaChatModel.builder()
                    .modelName(config.getModelName())
                    .baseUrl(config.getBaseUrl())
                    .formatter(new OllamaChatFormatter());

            if (headerList != null){
                HashMap<String, String> headerMap = new HashMap<>();
                for (AiHttpHeaderEntity header : headerList){
                    headerMap.put(header.getHeaderName(), header.getHeaderValue());
                }
                builder.defaultOptions(OllamaOptions.fromGenerateOptions(optionsBuilder.build()));
            }

            return builder.build();
        } else if (ModelType.ANTHROPIC.getCode().equals(config.getProvider().getCode())) {
            return AnthropicChatModel.builder()
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelName())
                    .baseUrl(config.getBaseUrl())
                    .stream(config.getStreaming())
                    .formatter(new AnthropicChatFormatter())
                    .build();
        }
        return null;
    }
}
