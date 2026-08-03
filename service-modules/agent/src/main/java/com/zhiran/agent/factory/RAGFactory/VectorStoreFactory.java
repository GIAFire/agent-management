package com.zhiran.agent.factory.RAGFactory;

import com.zhiran.agent.config.vector.VectorStoreProperties;
import com.zhiran.agent.entity.AiKnowledgeBaseEntity;
import com.zhiran.agent.factory.RAGFactory.vector.VectorStoreProvider;
import com.zhiran.agent.factory.RAGFactory.vector.VectorStoreSession;
import com.zhiran.agent.factory.RAGFactory.vector.VectorStoreType;
import com.zhiran.agent.knowledge.KnowledgeOperationException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class VectorStoreFactory {

    private final VectorStoreProperties properties;
    private final Map<VectorStoreType, VectorStoreProvider> providers;

    public VectorStoreFactory(
            VectorStoreProperties properties,
            List<VectorStoreProvider> providers
    ) {
        this.properties = properties;
        this.providers = new EnumMap<>(VectorStoreType.class);
        for (VectorStoreProvider provider : providers) {
            VectorStoreProvider previous = this.providers.put(
                    provider.type(),
                    provider
            );
            if (previous != null) {
                throw new IllegalStateException(
                        "重复的向量存储 Provider：" + provider.type()
                );
            }
        }
    }

    public boolean isConfigured() {
        return selectedProvider()
                .map(VectorStoreProvider::isConfigured)
                .orElse(false);
    }

    public String selectedTypeName() {
        return selectedType()
                .map(Enum::name)
                .orElseThrow(
                        () -> new KnowledgeOperationException(
                                "向量存储类型未配置或不支持："
                                        + properties.getType()
                        )
                );
    }

    public VectorStoreSession create(
            AiKnowledgeBaseEntity knowledgeBase
    ) {
        VectorStoreProvider provider = requireSelectedProvider();
        requireConfigured(provider);
        return provider.create(knowledgeBase);
    }

    public void deleteCollection(
            AiKnowledgeBaseEntity knowledgeBase
    ) {
        VectorStoreProvider provider = requireSelectedProvider();
        requireConfigured(provider);
        provider.deleteCollection(knowledgeBase);
    }

    private Optional<VectorStoreType> selectedType() {
        return VectorStoreType.parse(properties.getType());
    }

    private Optional<VectorStoreProvider> selectedProvider() {
        return selectedType().map(providers::get);
    }

    private VectorStoreProvider requireSelectedProvider() {
        return selectedProvider().orElseThrow(
                () -> new KnowledgeOperationException(
                        "向量存储类型未配置或不支持："
                                + properties.getType()
                )
        );
    }

    private static void requireConfigured(VectorStoreProvider provider) {
        if (!provider.isConfigured()) {
            throw new KnowledgeOperationException(
                    provider.type() + " 向量存储缺少必要连接地址"
            );
        }
    }
}
