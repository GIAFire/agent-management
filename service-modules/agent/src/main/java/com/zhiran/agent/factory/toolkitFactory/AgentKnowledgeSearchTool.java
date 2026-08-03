package com.zhiran.agent.factory.toolkitFactory;

import com.zhiran.agent.entity.AiKnowledgeBaseEntity;
import com.zhiran.agent.entity.AiKnowledgeChunkEntity;
import com.zhiran.agent.factory.RAGFactory.runTime.KnowledgeRuntime;
import com.zhiran.agent.factory.RAGFactory.runTime.KnowledgeRuntimeFactory;
import com.zhiran.agent.mapper.AiKnowledgeChunkMapper;
import com.zhiran.agent.service.AiKnowledgeBaseService;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 智能体知识检索入口。每次工具调用重新读取绑定和知识库状态，因此停用、删除和
 * API Key 轮换无需等待长期缓存的 Agent 实例失效。
 */
public final class AgentKnowledgeSearchTool {

    private static final Logger log =
            LoggerFactory.getLogger(AgentKnowledgeSearchTool.class);
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;
    private static final int MAX_CONCURRENCY = 4;

    private final Long agentId;
    private final Long agentConfigId;
    private final Long tenantId;
    private final AiKnowledgeBaseService knowledgeBaseService;
    private final KnowledgeRuntimeFactory runtimeFactory;
    private final AiKnowledgeChunkMapper chunkMapper;

    public AgentKnowledgeSearchTool(
            Long agentId,
            Long agentConfigId,
            Long tenantId,
            AiKnowledgeBaseService knowledgeBaseService,
            KnowledgeRuntimeFactory runtimeFactory,
            AiKnowledgeChunkMapper chunkMapper
    ) {
        this.agentId = agentId;
        this.agentConfigId = agentConfigId;
        this.tenantId = tenantId;
        this.knowledgeBaseService = knowledgeBaseService;
        this.runtimeFactory = runtimeFactory;
        this.chunkMapper = chunkMapper;
    }

    @Tool(
            name = "search_agent_knowledge",
            description = "检索当前智能体绑定的全部知识库。回答业务规则、产品资料、操作说明或故障处理问题前，应先使用此工具核实知识。"
    )
    public String searchKnowledge(
            @ToolParam(name = "query", description = "需要检索的完整问题")
            String query,
            @ToolParam(
                    name = "limit",
                    description = "最终返回的知识片段数量；不填写时采用知识库配置，最大 20",
                    required = false
            )
            Integer limit
    ) {
        if (!StringUtils.hasText(query)) {
            return "检索问题不能为空。";
        }

        List<AiKnowledgeBaseEntity> knowledgeBases =
                knowledgeBaseService.getAgentBindKnowledge(
                        agentId,
                        agentConfigId,
                        tenantId
                );
        if (knowledgeBases == null || knowledgeBases.isEmpty()) {
            return "当前智能体没有绑定可用的知识库。";
        }

        List<KnowledgeRetrieval> retrievals = Flux.fromIterable(knowledgeBases)
                .flatMapSequential(
                        knowledgeBase -> retrieveKnowledgeBase(
                                knowledgeBase,
                                query
                        ),
                        Math.min(knowledgeBases.size(), MAX_CONCURRENCY)
                )
                .collectList()
                .block();

        List<KnowledgeRetrieval> completed = retrievals == null
                ? List.of()
                : retrievals;
        boolean anySucceeded = completed.stream()
                .anyMatch(KnowledgeRetrieval::successful);
        if (!anySucceeded) {
            return "知识检索服务暂不可用，请稍后重试。";
        }
        List<KnowledgeHit> hits = completed.stream()
                .filter(KnowledgeRetrieval::successful)
                .flatMap(result -> result.hits().stream())
                .toList();

        List<KnowledgeHit> finalHits = mergeRoundRobin(
                hits,
                normalizeLimit(limit, knowledgeBases)
        );
        return formatResult(finalHits);
    }

    private Mono<KnowledgeRetrieval> retrieveKnowledgeBase(
            AiKnowledgeBaseEntity knowledgeBase,
            String query
    ) {
        return Mono.using(
                        () -> runtimeFactory.create(knowledgeBase),
                        runtime -> runtime.getKnowledge()
                                .retrieve(query, runtime.getRetrieveConfig())
                                .map(
                                        documents -> KnowledgeRetrieval.success(
                                                readyHits(runtime, documents)
                                        )
                                ),
                        KnowledgeRuntime::close
                )
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofSeconds(90))
                .onErrorResume(error -> {
                    log.warn(
                            "Knowledge retrieval failed, knowledgeBaseId={}, errorType={}",
                            knowledgeBase.getId(),
                            error.getClass().getSimpleName()
                    );
                    return Mono.just(KnowledgeRetrieval.failed());
                });
    }

    private static KnowledgeHit toHit(
            KnowledgeRuntime runtime,
            Document document,
            String content
    ) {
        Double rawScore = document.getScore();
        if (rawScore == null) {
            return null;
        }
        double similarity = rawScore;
        if (similarity < runtime.getScoreThreshold()) {
            return null;
        }
        return new KnowledgeHit(
                runtime.getKnowledgeBaseId(),
                runtime.getKnowledgeBaseName(),
                runtime.getCollectionName(),
                similarity,
                content
        );
    }

    List<KnowledgeHit> readyHits(
            KnowledgeRuntime runtime,
            List<Document> documents
    ) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        List<Long> candidateIds = documents.stream()
                .map(AgentKnowledgeSearchTool::platformChunkId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (candidateIds.isEmpty()) {
            return List.of();
        }
        Map<Long, String> readyContentById = new HashMap<>();
        for (AiKnowledgeChunkEntity chunk :
                chunkMapper.selectReadyChunks(
                        tenantId,
                        runtime.getKnowledgeBaseId(),
                        candidateIds
                )) {
            readyContentById.put(chunk.getId(), chunk.getContent());
        }
        return documents.stream()
                .filter(
                        document -> readyContentById.containsKey(
                                platformChunkId(document)
                        )
                )
                .map(
                        document -> toHit(
                                runtime,
                                document,
                                readyContentById.get(platformChunkId(document))
                        )
                )
                .filter(Objects::nonNull)
                .sorted(
                        Comparator.comparing(
                                KnowledgeHit::score,
                                Comparator.reverseOrder()
                        )
                )
                .limit(runtime.getResultLimit())
                .toList();
    }

    private static Long platformChunkId(Document document) {
        if (document == null || document.getMetadata() == null) {
            return null;
        }
        Object chunkId = document.getMetadata().getPayloadValue("chunkId");
        if (chunkId instanceof Number number) {
            return number.longValue();
        }
        try {
            return chunkId == null ? null : Long.valueOf(chunkId.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static List<KnowledgeHit> mergeRoundRobin(
            List<KnowledgeHit> hits,
            int limit
    ) {
        Map<Long, List<KnowledgeHit>> hitsByKnowledgeBase =
                new LinkedHashMap<>();
        for (KnowledgeHit hit : hits) {
            hitsByKnowledgeBase
                    .computeIfAbsent(
                            hit.knowledgeBaseId(),
                            ignored -> new ArrayList<>()
                    )
                    .add(hit);
        }
        hitsByKnowledgeBase.values().forEach(
                values -> values.sort(
                        Comparator.comparing(
                                KnowledgeHit::score,
                                Comparator.reverseOrder()
                        )
                )
        );

        List<KnowledgeHit> merged =
                new ArrayList<>(Math.min(hits.size(), limit));
        for (int index = 0; merged.size() < limit; index++) {
            boolean added = false;
            for (List<KnowledgeHit> knowledgeBaseHits :
                    hitsByKnowledgeBase.values()) {
                if (index < knowledgeBaseHits.size()) {
                    merged.add(knowledgeBaseHits.get(index));
                    added = true;
                    if (merged.size() == limit) {
                        return merged;
                    }
                }
            }
            if (!added) {
                return merged;
            }
        }
        return merged;
    }

    private static int normalizeLimit(
            Integer limit,
            List<AiKnowledgeBaseEntity> knowledgeBases
    ) {
        if (limit != null) {
            return Math.max(1, Math.min(limit, MAX_LIMIT));
        }
        int configuredLimit = knowledgeBases.stream()
                .map(AiKnowledgeBaseEntity::getTopK)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(DEFAULT_LIMIT);
        return Math.max(1, Math.min(configuredLimit, MAX_LIMIT));
    }

    private static String formatResult(List<KnowledgeHit> hits) {
        if (hits.isEmpty()) {
            return "没有检索到相关知识。";
        }

        StringBuilder result = new StringBuilder("检索到以下相关知识：\n\n");
        for (int index = 0; index < hits.size(); index++) {
            KnowledgeHit hit = hits.get(index);
            result.append("【结果 ").append(index + 1).append("】\n")
                    .append("知识库：")
                    .append(hit.knowledgeBaseName())
                    .append("\n")
                    .append("库内相关度：")
                    .append(String.format(Locale.ROOT, "%.4f", hit.score()))
                    .append("\n")
                    .append("内容：")
                    .append(hit.content())
                    .append("\n\n");
        }
        return result.toString();
    }

    record KnowledgeHit(
            Long knowledgeBaseId,
            String knowledgeBaseName,
            String collectionName,
            Double score,
            String content
    ) {
    }

    private record KnowledgeRetrieval(
            boolean successful,
            List<KnowledgeHit> hits
    ) {

        private static KnowledgeRetrieval success(List<KnowledgeHit> hits) {
            return new KnowledgeRetrieval(true, hits);
        }

        private static KnowledgeRetrieval failed() {
            return new KnowledgeRetrieval(false, List.of());
        }
    }
}
