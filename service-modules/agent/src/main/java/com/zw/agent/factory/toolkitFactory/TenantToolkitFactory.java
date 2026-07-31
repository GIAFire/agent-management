package com.zw.agent.factory.toolkitFactory;

import com.zw.agent.entity.DTO.AgentBindToolDTO;
import com.zw.agent.factory.RAGFactory.runTime.KnowledgeRuntimeFactory;
import com.zw.agent.mapper.AiKnowledgeChunkMapper;
import com.zw.agent.service.AiAgentToolService;
import com.zw.agent.service.AiKnowledgeBaseService;
import com.zw.common.context.UserInfo;
import io.agentscope.core.tool.Toolkit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TenantToolkitFactory {

    private static final Logger log = LoggerFactory.getLogger(TenantToolkitFactory.class);

    private final AiAgentToolService agentToolService;
    private final AiKnowledgeBaseService knowledgeBaseService;
    private final ApplicationContext applicationContext;
    private final KnowledgeRuntimeFactory knowledgeRuntimeFactory;
    private final AiKnowledgeChunkMapper knowledgeChunkMapper;

    public Toolkit buildToolkit(Long agentId, Long agentConfigId, UserInfo userInfo) {
        Toolkit toolkit = new Toolkit();

        List<AgentBindToolDTO> toolList = agentToolService.agentBindTools(
                agentId,
                agentConfigId,
                userInfo.getTenantId()
        );
        toolkit.registerTool(
                new AgentKnowledgeSearchTool(
                        agentId,
                        agentConfigId,
                        userInfo.getTenantId(),
                        knowledgeBaseService,
                        knowledgeRuntimeFactory,
                        knowledgeChunkMapper
                )
        );

        Set<String> registeredClasses = new LinkedHashSet<>();
        for (AgentBindToolDTO toolInfo : toolList) {
            if (!StringUtils.hasText(toolInfo.getClassName())
                    || !registeredClasses.add(toolInfo.getClassName())) {
                continue;
            }

            Object toolBean = resolveToolBean(toolInfo);
            if (toolBean == null) {
                log.error("Tool bean not found, toolId={}, beanName={}, className={}",
                        toolInfo.getId(), toolInfo.getBeanName(), toolInfo.getClassName());
                continue;
            }
            toolkit.registerTool(toolBean);
        }
        return toolkit;
    }

    private Object resolveToolBean(AgentBindToolDTO toolConfig) {
        try {
            if (StringUtils.hasText(toolConfig.getBeanName())
                    && applicationContext.containsBean(toolConfig.getBeanName())) {
                return applicationContext.getBean(toolConfig.getBeanName());
            }

            Class<?> toolClass = ClassUtils.forName(toolConfig.getClassName(), applicationContext.getClassLoader());
            String[] beanNames = applicationContext.getBeanNamesForType(toolClass, false, false);
            if (beanNames.length > 0) {
                return applicationContext.getBean(beanNames[0]);
            }

            return applicationContext.getAutowireCapableBeanFactory().createBean(toolClass);
        } catch (ClassNotFoundException | LinkageError | BeansException ex) {
            log.error("Failed to resolve tool bean, beanName={}, className={}",
                    toolConfig.getBeanName(), toolConfig.getClassName(), ex);
            return null;
        }
    }
}
