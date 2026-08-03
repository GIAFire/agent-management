package com.zhiran.agent.config.vector;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "rag.store")
public class VectorStoreProperties {

    private String type;

    private String collectionPrefix = "zhiran_rag";

    private Milvus milvus = new Milvus();

    private Elasticsearch elasticsearch = new Elasticsearch();

    private PgVector pgvector = new PgVector();

    private Qdrant qdrant = new Qdrant();

    @Data
    public static class Milvus {

        private String host;

        private String database = "default";

        private String username;

        private String password;

        private String token;

        private Duration connectTimeout = Duration.ofSeconds(10);
    }

    @Data
    public static class Elasticsearch {

        private String url;

        private String username;

        private String password;

        private boolean disableSslVerification;
    }

    @Data
    public static class PgVector {

        private String jdbcUrl;

        private String username;

        private String password;

        private String schema = "public";

        private Duration connectionTimeout = Duration.ofSeconds(30);
    }

    @Data
    public static class Qdrant {

        private String location;

        private String apiKey;

        private boolean checkCompatibility = true;
    }
}
