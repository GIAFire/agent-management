package com.zw.agent.factory.RAGFactory;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.embeddings.CreateEmbeddingResponse;
import com.openai.models.embeddings.Embedding;
import com.openai.models.embeddings.EmbeddingCreateParams;
import io.agentscope.core.embedding.EmbeddingException;
import io.agentscope.core.embedding.EmbeddingModel;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ModelArts MaaS 文本向量模型。
 *
 * 与 OpenAI Embeddings 协议兼容，但不会发送 dimensions 参数。
 * 适用于不支持 Matryoshka 可变维度的 BGE-M3。
 */
public final class ModelArtsTextEmbedding implements EmbeddingModel {

    private final OpenAIClient client;

    private final String modelName;

    /**
     * 模型原生维度，仅用于本地声明和返回结果校验。
     * 不会发送给 ModelArts。
     */
    private final int dimensions;

    private ModelArtsTextEmbedding(
            String baseUrl,
            String apiKey,
            String modelName,
            int dimensions,
            Map<String, String> headers
    ) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl cannot be blank");
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey cannot be blank");
        }

        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException("modelName cannot be blank");
        }

        if (dimensions <= 0) {
            throw new IllegalArgumentException(
                    "dimensions must be positive: " + dimensions
            );
        }

        OpenAIOkHttpClient.Builder clientBuilder =
                OpenAIOkHttpClient.builder()
                        .baseUrl(normalizeBaseUrl(baseUrl))
                        .apiKey(apiKey);

        if (headers != null && !headers.isEmpty()) {
            headers.forEach((name, value) -> {
                if (name != null
                        && !name.isBlank()
                        && value != null
                        && !value.isBlank()) {

                    clientBuilder.putHeader(name, value);
                }
            });
        }

        this.client = clientBuilder.build();
        this.modelName = modelName;
        this.dimensions = dimensions;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Mono<double[]> embed(ContentBlock block) {
        if (!(block instanceof TextBlock textBlock)) {
            return Mono.error(
                    new EmbeddingException(
                            "ModelArtsTextEmbedding only supports TextBlock",
                            modelName,
                            "modelarts"
                    )
            );
        }

        String text = textBlock.getText();

        if (text == null || text.isBlank()) {
            return Mono.error(
                    new EmbeddingException(
                            "Embedding text cannot be blank",
                            modelName,
                            "modelarts"
                    )
            );
        }

        return Mono.fromCallable(() -> doEmbed(text))
                .onErrorMap(error -> {
                    if (error instanceof EmbeddingException) {
                        return error;
                    }

                    return new EmbeddingException(
                            "Failed to generate ModelArts embedding: "
                                    + error.getMessage(),
                            error,
                            modelName,
                            "modelarts"
                    );
                });
    }

    private double[] doEmbed(String text) {
        /*
         * 关键点：
         * 不调用 .dimensions(dimensions)。
         */
        EmbeddingCreateParams params = EmbeddingCreateParams.builder()
                .model(modelName)
                .inputOfArrayOfStrings(List.of(text))
                .encodingFormat(
                        EmbeddingCreateParams.EncodingFormat.FLOAT
                )
                .build();

        CreateEmbeddingResponse response =
                client.embeddings().create(params);

        if (response == null
                || response.data() == null
                || response.data().isEmpty()) {

            throw new EmbeddingException(
                    "ModelArts returned empty embedding response",
                    modelName,
                    "modelarts"
            );
        }

        Embedding embedding = response.data().getFirst();

        if (embedding == null
                || embedding.embedding() == null
                || embedding.embedding().isEmpty()) {

            throw new EmbeddingException(
                    "ModelArts returned empty embedding vector",
                    modelName,
                    "modelarts"
            );
        }

        List<Float> values = embedding.embedding();

        if (values.size() != dimensions) {
            throw new EmbeddingException(
                    "Embedding dimension mismatch, expected="
                            + dimensions
                            + ", actual="
                            + values.size(),
                    modelName,
                    "modelarts"
            );
        }

        double[] result = new double[values.size()];

        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }

        return result;
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public int getDimensions() {
        return dimensions;
    }

    /**
     * 数据库既可以暂时保存 /v1，也可以保存完整的 /v1/embeddings。
     * 这里统一归一化成 OpenAI SDK 所需的 API 根地址。
     */
    private static String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl.trim();

        while (normalized.endsWith("/")) {
            normalized =
                    normalized.substring(0, normalized.length() - 1);
        }

        if (normalized.endsWith("/embeddings")) {
            normalized = normalized.substring(
                    0,
                    normalized.length() - "/embeddings".length()
            );
        }

        return normalized;
    }

    public static final class Builder {

        private String baseUrl;

        private String apiKey;

        private String modelName;

        private int dimensions;

        private final Map<String, String> headers = new LinkedHashMap<>();

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder dimensions(int dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        /**
         * 增加单个固定请求头。
         */
        public Builder header(String name, String value) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("header name cannot be blank");
            }

            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(
                        "header value cannot be blank: " + name
                );
            }

            this.headers.put(name, value);
            return this;
        }

        /**
         * 批量增加固定请求头。
         */
        public Builder headers(Map<String, String> headers) {
            if (headers != null) {
                headers.forEach(this::header);
            }

            return this;
        }

        public ModelArtsTextEmbedding build() {
            return new ModelArtsTextEmbedding(
                    baseUrl,
                    apiKey,
                    modelName,
                    dimensions,
                    Map.copyOf(headers)
            );
        }
    }
}
