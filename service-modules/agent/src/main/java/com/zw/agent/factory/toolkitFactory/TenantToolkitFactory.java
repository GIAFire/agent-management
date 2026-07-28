package com.zw.agent.factory.toolkitFactory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zw.agent.entity.AiKnowledgeBaseEntity;
import com.zw.agent.entity.AiToolInfoConfigEntity;
import com.zw.agent.entity.DTO.AgentBindToolDTO;
import com.zw.agent.factory.RAGFactory.MilvusStoreFactory;
import com.zw.agent.factory.RAGFactory.ModelArtsTextEmbedding;
import com.zw.agent.service.AiAgentToolService;
import com.zw.agent.service.AiKnowledgeBaseService;
import com.zw.agent.service.AiToolInfoConfigService;
import com.zw.common.context.UserInfo;
import io.agentscope.core.embedding.EmbeddingModel;
import io.agentscope.core.embedding.dashscope.DashScopeTextEmbedding;
import io.agentscope.core.embedding.openai.OpenAITextEmbedding;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.rag.KnowledgeRetrievalTools;
import io.agentscope.core.rag.integration.bailian.BailianKnowledge;
import io.agentscope.core.rag.knowledge.SimpleKnowledge;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.reader.ReaderInput;
import io.agentscope.core.rag.reader.TextReader;
import io.agentscope.core.rag.reader.TikaReader;
import io.agentscope.core.rag.store.VDBStoreBase;
import io.agentscope.core.tool.Toolkit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class TenantToolkitFactory {

    private static final Logger log = LoggerFactory.getLogger(TenantToolkitFactory.class);

    private final AiAgentToolService agentToolService;
    private final AiKnowledgeBaseService knowledgeBaseService;
    private final ApplicationContext applicationContext;
    private final MilvusStoreFactory milvusStoreFactory;

    public Toolkit buildToolkit(Long agentId, UserInfo userInfo) {
        Toolkit toolkit = new Toolkit();

        List<AgentBindToolDTO> toolList = agentToolService.agentBindTools(agentId,userInfo.getTenantId());
        List<AiKnowledgeBaseEntity> knowledgeBaseList = knowledgeBaseService.getAgentBindKnowledge(agentId,userInfo.getTenantId());
        if (!CollectionUtils.isEmpty(knowledgeBaseList)) {
            AiKnowledgeBaseEntity knowledgeBaseEntity = knowledgeBaseList.get(0);
            VDBStoreBase milvusStore = milvusStoreFactory.create(knowledgeBaseEntity);
            EmbeddingModel embeddings = ModelArtsTextEmbedding.builder()
                    .baseUrl(knowledgeBaseEntity.getModelUrl())
                    .apiKey(System.getenv("OPENAI_API_KEY2"))
                    .modelName(knowledgeBaseEntity.getEmbeddingModelName())
                    .dimensions(knowledgeBaseEntity.getEmbeddingDimension())
                    .header("X-Project-Id", "1")
                    .header("X-Tenant-Id", "2")
                    .build();
            SimpleKnowledge simpleKnowledge = SimpleKnowledge.builder()
                    .embeddingModel(embeddings)
                    .embeddingStore(milvusStore)
                    .build();
            KnowledgeRetrievalTools simpleKnowledgeTools = new KnowledgeRetrievalTools(simpleKnowledge);
            toolkit.registerTool(simpleKnowledgeTools);
        }

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
