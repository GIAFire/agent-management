package com.zw.agent.factory.modelFactory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zw.agent.entity.AiToolRolePermissionEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.service.AiToolRolePermissionService;
import com.zw.common.context.UserInfo;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.formatter.ollama.OllamaChatFormatter;
import io.agentscope.core.formatter.openai.OpenAIChatFormatter;
import io.agentscope.core.model.ChatModelBase;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.OllamaChatModel;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionMode;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.core.tool.Toolkit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Component
public class ModelFactory {


    public ChatModelBase buildModelConfig(
            ChatModelBase builder,
            AgentConfigDTO config
    ){
        // 根据模型类型,创建对应模型实例
        if (builder instanceof OpenAIChatModel) {
            return OpenAIChatModel.builder()
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelName())
                    .baseUrl(config.getBaseUrl())
                    .stream(config.getIsStream())
                    .formatter(new OpenAIChatFormatter())
                    .build();
        } else if (builder instanceof DashScopeChatModel) {
            return DashScopeChatModel.builder()
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelName())
                    .baseUrl(config.getBaseUrl())
                    .stream(config.getIsStream())
                    .formatter(new DashScopeChatFormatter())
                    .build();
        } else if (builder instanceof OllamaChatModel) {
            return OllamaChatModel.builder()
                    .modelName(config.getModelName())
                    .baseUrl(config.getBaseUrl())
                    .formatter(new OllamaChatFormatter())
                    .build();
        }
    }
}
