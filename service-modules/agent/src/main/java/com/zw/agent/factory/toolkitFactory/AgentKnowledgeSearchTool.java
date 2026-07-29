package com.zw.agent.factory.toolkitFactory;

import com.zw.agent.factory.RAGFactory.runTime.KnowledgeRuntime;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 当前智能体绑定知识库的统一检索入口。
 *
 * 每个知识库独立召回；不同 collection 的原始向量分数不参与横向排序，
 * 结果以轮询方式合并，避免某一个 collection 独占返回结果。
 */
public final class AgentKnowledgeSearchTool {

    private static final Logger log = LoggerFactory.getLogger(AgentKnowledgeSearchTool.class);
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 10;
    private static final int MAX_CONCURRENCY = 4;

    private final List<KnowledgeRuntime> knowledgeRuntimes;

    public AgentKnowledgeSearchTool(List<KnowledgeRuntime> knowledgeRuntimes) {
        this.knowledgeRuntimes = List.copyOf(knowledgeRuntimes);
    }

    @Tool(
            name = "search_agent_knowledge",
            description = "检索当前智能体绑定的全部知识库。回答业务规则、产品资料、操作说明或故障处理问题前，应先使用此工具核实知识。"
    )
    public String searchKnowledge(
            @ToolParam(name = "query", description = "需要检索的完整问题") String query,
            @ToolParam(name = "limit", description = "最终返回的知识片段数量，默认 5，最大 10", required = false) Integer limit
    ) {
        if (!StringUtils.hasText(query)) {
            return "检索问题不能为空。";
        }
        if (knowledgeRuntimes.isEmpty()) {
            return "当前智能体没有绑定可用的知识库。";
        }

        List<KnowledgeHit> hits = Flux.fromIterable(knowledgeRuntimes)
                .flatMapSequential(runtime -> runtime.getKnowledge()
                                .retrieve(query, runtime.getRetrieveConfig())
                                .flatMapMany(Flux::fromIterable)
                                .map(document -> toHit(runtime, document))
                                .onErrorResume(error -> {
                                    log.warn("Knowledge retrieval failed, knowledgeBaseId={}, collectionName={}",
                                            runtime.getKnowledgeBaseId(), runtime.getCollectionName(), error);
                                    return Flux.empty();
                                }),
                        Math.min(knowledgeRuntimes.size(), MAX_CONCURRENCY))
                .collectList()
                .block();

        List<KnowledgeHit> finalHits = mergeRoundRobin(
                hits == null ? List.of() : hits,
                normalizeLimit(limit)
        );
        return formatResult(finalHits);
    }

    private static KnowledgeHit toHit(KnowledgeRuntime runtime, Document document) {
        return new KnowledgeHit(
                runtime.getKnowledgeBaseId(),
                runtime.getKnowledgeBaseName(),
                runtime.getCollectionName(),
                document.getScore(),
                document.getMetadata().getContentText()
        );
    }

    static List<KnowledgeHit> mergeRoundRobin(List<KnowledgeHit> hits, int limit) {
        Map<Long, List<KnowledgeHit>> hitsByKnowledgeBase = new LinkedHashMap<>();
        for (KnowledgeHit hit : hits) {
            hitsByKnowledgeBase.computeIfAbsent(hit.knowledgeBaseId(), ignored -> new ArrayList<>()).add(hit);
        }

        List<KnowledgeHit> merged = new ArrayList<>(Math.min(hits.size(), limit));
        for (int index = 0; merged.size() < limit; index++) {
            boolean added = false;
            for (List<KnowledgeHit> knowledgeBaseHits : hitsByKnowledgeBase.values()) {
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

    private static int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    private static String formatResult(List<KnowledgeHit> hits) {
        if (hits.isEmpty()) {
            return "没有检索到相关知识。";
        }

        StringBuilder result = new StringBuilder("检索到以下相关知识：\n\n");
        for (int index = 0; index < hits.size(); index++) {
            KnowledgeHit hit = hits.get(index);
            result.append("【结果 ").append(index + 1).append("】\n")
                    .append("知识库：").append(hit.knowledgeBaseName()).append("\n");
            if (hit.score() != null) {
                result.append("库内相关度：")
                        .append(String.format(Locale.ROOT, "%.4f", hit.score()))
                        .append("\n");
            }
            result.append("内容：").append(hit.content()).append("\n\n");
        }
        return result.toString();
    }

    record KnowledgeHit(Long knowledgeBaseId, String knowledgeBaseName, String collectionName,
                        Double score, String content) {
    }
}
