package com.zw.agent.factory.modelFactory;

import com.zw.agent.entity.DTO.AgentConfigDTO;
import io.agentscope.core.formatter.anthropic.AnthropicChatFormatter;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.formatter.ollama.OllamaChatFormatter;
import io.agentscope.core.formatter.openai.OpenAIChatFormatter;
import io.agentscope.core.model.*;
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
        // 根据模型类型,创建对应模型实例
        if (ModelType.OPENAI.getCode().equals(config.getProvider().getCode())) {
            return OpenAIChatModel.builder()
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelName())
                    .baseUrl(config.getBaseUrl())
                    .stream(config.getIsStream())
                    .formatter(new OpenAIChatFormatter())
                    .build();
        } else if (ModelType.DASH_SCOPE.getCode().equals(config.getProvider().getCode())) {
            return DashScopeChatModel.builder()
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelName())
                    .baseUrl(config.getBaseUrl())
                    .stream(config.getIsStream())
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
                    .stream(config.getIsStream())
                    .formatter(new AnthropicChatFormatter())
                    .build();
        }
        return null;
    }
}
