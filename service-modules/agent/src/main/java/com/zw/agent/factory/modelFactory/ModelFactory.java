package com.zw.agent.factory.modelFactory;

import com.zw.agent.entity.DTO.AgentConfigDTO;
import io.agentscope.core.model.*;
import io.agentscope.extensions.model.anthropic.AnthropicChatModel;
import io.agentscope.extensions.model.anthropic.formatter.AnthropicChatFormatter;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.extensions.model.dashscope.formatter.DashScopeChatFormatter;
import io.agentscope.extensions.model.ollama.OllamaChatModel;
import io.agentscope.extensions.model.ollama.formatter.OllamaChatFormatter;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.extensions.model.openai.formatter.OpenAIChatFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ModelFactory {


    public ChatModelBase buildModel(
            AgentConfigDTO config
    ){

        if (ModelType.OPENAI.getCode().equals(config.getProvider().getCode())) {
            return OpenAIChatModel.builder()
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelName())
                    .baseUrl(config.getBaseUrl())
                    .stream(config.getStreaming())
                    .formatter(new OpenAIChatFormatter())
                    .build();
        } else if (ModelType.DASH_SCOPE.getCode().equals(config.getProvider().getCode())) {
            return DashScopeChatModel.builder()
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelName())
                    .baseUrl(config.getBaseUrl())
                    .stream(config.getStreaming())
                    .formatter(new DashScopeChatFormatter())
                    .build();
        } else if (ModelType.OLLAMA.getCode().equals(config.getProvider().getCode())) {
            return OllamaChatModel.builder()
                    .modelName(config.getModelName())
                    .baseUrl(config.getBaseUrl())
                    .formatter(new OllamaChatFormatter())
                    .build();
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
