package com.zw.agent.tools.testCustomer;

import com.zw.agent.entity.AiKnowledgeBackendConfigEntity;
import com.zw.agent.entity.AiKnowledgeBaseEntity;
import com.zw.agent.factory.RAGFactory.RagBackendFactory;
import com.zw.agent.factory.RAGFactory.backendImpl.KnowledgeBackend;
import com.zw.agent.factory.RAGFactory.entity.KnowledgeBase;
import com.zw.agent.service.AiKnowledgeBackendConfigService;
import com.zw.agent.service.AiKnowledgeBaseService;
import com.zw.agent.tools.ToolResponse;
import com.zw.agent.tools.ToolSchemaUtils;
import com.zw.agent.tools.applicationRunner.Tenant;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.ToolBase;
import io.agentscope.core.tool.ToolCallParam;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tenant("1")
@Component
public class QueryKnowledgeBaseTool extends ToolBase {

    private final AiKnowledgeBaseService aiKnowledgeBaseService;
    private final AiKnowledgeBackendConfigService aiKnowledgeBackendConfigService;
    private final RagBackendFactory ragBackendFactory;

    public QueryKnowledgeBaseTool(
            AiKnowledgeBaseService aiKnowledgeBaseService,
            AiKnowledgeBackendConfigService aiKnowledgeBackendConfigService,
            RagBackendFactory ragBackendFactory
    ) {
        super(ToolBase.builder()
                .name("query_knowledge_base")
                .description("查询知识库内容。适合在回答用户问题前，从指定知识库检索相关切片、引用文本和相似度分数。")
                .inputSchema(inputSchema())
                .readOnly(true)
                .concurrencySafe(true));
        this.aiKnowledgeBaseService = aiKnowledgeBaseService;
        this.aiKnowledgeBackendConfigService = aiKnowledgeBackendConfigService;
        this.ragBackendFactory = ragBackendFactory;
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        try {
            String query = ToolSchemaUtils.requiredString(param, "query");
            Long knowledgeBaseId = requiredLong(param, "knowledgeBaseId");

            AiKnowledgeBaseEntity knowledgeBaseEntity = aiKnowledgeBaseService.getById(knowledgeBaseId);
            if (knowledgeBaseEntity == null) {
                throw new IllegalArgumentException("Knowledge base not found: " + knowledgeBaseId);
            }

            Long backendConfigId = optionalLong(param, "backendConfigId");
            if (backendConfigId == null) {
                backendConfigId = knowledgeBaseEntity.getKnowledgeBackendId();
            }
            if (backendConfigId == null) {
                throw new IllegalArgumentException("backendConfigId is required when knowledge base has no backend config");
            }

            AiKnowledgeBackendConfigEntity backendConfig = aiKnowledgeBackendConfigService.getById(backendConfigId);
            if (backendConfig == null) {
                throw new IllegalArgumentException("Knowledge backend config not found: " + backendConfigId);
            }

            int topK = clamp(optionalInteger(param, "topK", defaultTopK(knowledgeBaseEntity)), 1, 20);
            double scoreThreshold = optionalDouble(param, "scoreThreshold", defaultScoreThreshold(knowledgeBaseEntity));
            KnowledgeBase knowledgeBase = buildKnowledgeBase(knowledgeBaseEntity, backendConfigId);
            KnowledgeBackend backend = ragBackendFactory.create(backendConfig, knowledgeBase);
            List<EmbeddingMatch<TextSegment>> matches = backend.retrieveMatches(knowledgeBase, query, topK, scoreThreshold);

            List<Map<String, Object>> hits = matches.stream()
                    .map(this::toHit)
                    .toList();

            ToolResponse toolResponse = new ToolResponse();
            toolResponse.setCount(hits.size());
            toolResponse.setData(hits);

            ObjectMapper mapper = new ObjectMapper();
            return Mono.just(ToolResultBlock.text(mapper.writeValueAsString(toolResponse)));
        } catch (Exception ex) {
            return Mono.just(ToolResultBlock.error(ex.getMessage()));
        }
    }

    private Map<String, Object> toHit(EmbeddingMatch<TextSegment> match) {
        TextSegment segment = match.embedded();
        Map<String, Object> hit = new LinkedHashMap<>();
        hit.put("score", match.score());
        hit.put("vectorId", match.embeddingId());
        hit.put("content", segment == null ? "" : segment.text());
        hit.put("metadata", segment == null ? Map.of() : segment.metadata().toMap());
        return hit;
    }

    private KnowledgeBase buildKnowledgeBase(AiKnowledgeBaseEntity entity, Long backendConfigId) {
        return new KnowledgeBase()
                .setId(entity.getId())
                .setBackendConfigId(backendConfigId)
                .setName(entity.getKnowledgeName())
                .setCollectionName(entity.getCollectionName())
                .setChunkStrategy(entity.getChunkStrategy())
                .setChunkSize(entity.getChunkSize())
                .setChunkOverlap(entity.getChunkOverlap())
                .setProviderMetaJson(entity.getProviderMetaJson());
    }

    private int defaultTopK(AiKnowledgeBaseEntity entity) {
        return entity.getRetrieveTopK() == null || entity.getRetrieveTopK() <= 0
                ? 5
                : entity.getRetrieveTopK();
    }

    private double defaultScoreThreshold(AiKnowledgeBaseEntity entity) {
        BigDecimal threshold = entity.getScoreThreshold();
        return threshold == null ? 0.0 : threshold.doubleValue();
    }

    private Long requiredLong(ToolCallParam param, String name) {
        Long value = optionalLong(param, name);
        if (value == null) {
            throw new IllegalArgumentException("Missing required parameter: " + name);
        }
        return value;
    }

    private Long optionalLong(ToolCallParam param, String name) {
        Object value = input(param).get(name);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Integer optionalInteger(ToolCallParam param, String name, int defaultValue) {
        Object value = input(param).get(name);
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private Double optionalDouble(ToolCallParam param, String name, double defaultValue) {
        Object value = input(param).get(name);
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private Map<String, Object> input(ToolCallParam param) {
        return param == null || param.getInput() == null ? Map.of() : param.getInput();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Map<String, Object> inputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("query", ToolSchemaUtils.stringProperty("用户问题或检索关键词"));
        properties.put("knowledgeBaseId", numberProperty("知识库ID，对应 ai_knowledge_base.id"));
        properties.put("backendConfigId", numberProperty("向量配置ID，可不填；默认使用知识库绑定的配置"));
        properties.put("topK", numberProperty("返回命中数量，默认使用知识库配置，最大20"));
        properties.put("scoreThreshold", numberProperty("相似度阈值，默认使用知识库配置"));
        return ToolSchemaUtils.objectSchema(properties, List.of("query", "knowledgeBaseId"));
    }

    private static Map<String, Object> numberProperty(String description) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "number");
        schema.put("description", description);
        return schema;
    }
}
