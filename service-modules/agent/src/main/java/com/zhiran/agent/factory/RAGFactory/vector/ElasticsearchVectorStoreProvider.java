package com.zhiran.agent.factory.RAGFactory.vector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiran.agent.config.vector.VectorStoreProperties;
import com.zhiran.agent.entity.AiKnowledgeBaseEntity;
import com.zhiran.agent.knowledge.KnowledgeOperationException;
import io.agentscope.core.rag.exception.VectorStoreException;
import io.agentscope.core.rag.store.ElasticsearchStore;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class ElasticsearchVectorStoreProvider
        implements VectorStoreProvider {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final VectorStoreProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ElasticsearchVectorStoreProvider(VectorStoreProperties properties) {
        this.properties = properties;
    }

    @Override
    public VectorStoreType type() {
        return VectorStoreType.ELASTICSEARCH;
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(
                properties.getElasticsearch().getUrl()
        );
    }

    @Override
    public VectorStoreSession create(
            AiKnowledgeBaseEntity knowledgeBase
    ) {
        String indexName = VectorStoreProviderSupport.collectionName(
                properties,
                knowledgeBase
        );
        VectorStoreProperties.Elasticsearch config =
                properties.getElasticsearch();
        ElasticsearchStore.Builder builder = ElasticsearchStore.builder()
                .url(config.getUrl())
                .indexName(indexName)
                .dimensions(knowledgeBase.getEmbeddingDimension())
                .disableSslVerification(
                        config.isDisableSslVerification()
                );
        if (StringUtils.hasText(config.getUsername())) {
            builder.username(config.getUsername());
        }
        if (StringUtils.hasText(config.getPassword())) {
            builder.password(config.getPassword());
        }
        try {
            ElasticsearchStore store = builder.build();
            return new VectorStoreSession(
                    store,
                    documentId -> deleteDocument(indexName, documentId)
            );
        } catch (VectorStoreException error) {
            throw new KnowledgeOperationException(
                    "无法创建 Elasticsearch Store",
                    error
            );
        }
    }

    @Override
    public void deleteCollection(AiKnowledgeBaseEntity knowledgeBase) {
        String indexName = VectorStoreProviderSupport.collectionName(
                properties,
                knowledgeBase
        );
        sendManagementRequest(
                "DELETE",
                "/" + indexName,
                null,
                true
        );
    }

    private Mono<Boolean> deleteDocument(
            String indexName,
            String documentId
    ) {
        return Mono.fromCallable(() -> {
                    String body = deleteByQueryBody(documentId);
                    sendManagementRequest(
                            "POST",
                            "/" + indexName
                                    + "/_delete_by_query?conflicts=proceed&refresh=true",
                            body,
                            true
                    );
                    return true;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private String deleteByQueryBody(String documentId) {
        try {
            return objectMapper.writeValueAsString(
                    Map.of(
                            "query",
                            Map.of(
                                    "term",
                                    Map.of("doc_id", documentId)
                            )
                    )
            );
        } catch (JsonProcessingException impossible) {
            throw new IllegalStateException(
                    "无法生成 Elasticsearch 删除请求",
                    impossible
            );
        }
    }

    private void sendManagementRequest(
            String method,
            String path,
            String body,
            boolean notFoundIsSuccess
    ) {
        VectorStoreProperties.Elasticsearch config =
                properties.getElasticsearch();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl(config.getUrl()) + path))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json");
        applyAuthentication(request, config);
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json")
                    .method(
                            method,
                            HttpRequest.BodyPublishers.ofString(body)
                    );
        }
        try {
            HttpResponse<String> response = httpClient(config).send(
                    request.build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return;
            }
            if (notFoundIsSuccess && status == 404) {
                return;
            }
            throw new KnowledgeOperationException(
                    "Elasticsearch 管理请求失败，HTTP " + status
            );
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new KnowledgeOperationException(
                    "Elasticsearch 管理请求被中断",
                    interrupted
            );
        } catch (IOException error) {
            throw new KnowledgeOperationException(
                    "无法访问 Elasticsearch",
                    error
            );
        }
    }

    private static HttpClient httpClient(
            VectorStoreProperties.Elasticsearch config
    ) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT);
        if (config.isDisableSslVerification()) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(
                        null,
                        new TrustManager[]{new TrustAllCertificates()},
                        new SecureRandom()
                );
                SSLParameters sslParameters = new SSLParameters();
                sslParameters.setEndpointIdentificationAlgorithm("");
                builder.sslContext(sslContext)
                        .sslParameters(sslParameters);
            } catch (GeneralSecurityException error) {
                throw new KnowledgeOperationException(
                        "无法初始化 Elasticsearch SSL 配置",
                        error
                );
            }
        }
        return builder.build();
    }

    private static void applyAuthentication(
            HttpRequest.Builder request,
            VectorStoreProperties.Elasticsearch config
    ) {
        if (!StringUtils.hasText(config.getUsername())) {
            return;
        }
        String password = config.getPassword() == null
                ? ""
                : config.getPassword();
        String credentials = config.getUsername() + ":" + password;
        request.header(
                "Authorization",
                "Basic " + Base64.getEncoder().encodeToString(
                        credentials.getBytes(StandardCharsets.UTF_8)
                )
        );
    }

    private static String baseUrl(String url) {
        String trimmed = url.trim();
        return trimmed.endsWith("/")
                ? trimmed.substring(0, trimmed.length() - 1)
                : trimmed;
    }

    private static final class TrustAllCertificates
            implements X509TrustManager {

        @Override
        public void checkClientTrusted(
                X509Certificate[] chain,
                String authType
        ) {
        }

        @Override
        public void checkServerTrusted(
                X509Certificate[] chain,
                String authType
        ) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
