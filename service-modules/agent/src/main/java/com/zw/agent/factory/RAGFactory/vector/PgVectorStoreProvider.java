package com.zw.agent.factory.RAGFactory.vector;

import com.zw.agent.config.vector.VectorStoreProperties;
import com.zw.agent.entity.AiKnowledgeBaseEntity;
import com.zw.agent.knowledge.KnowledgeOperationException;
import io.agentscope.core.rag.exception.VectorStoreException;
import io.agentscope.core.rag.store.PgVectorStore;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Properties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PgVectorStoreProvider implements VectorStoreProvider {

    private final VectorStoreProperties properties;

    public PgVectorStoreProvider(VectorStoreProperties properties) {
        this.properties = properties;
    }

    @Override
    public VectorStoreType type() {
        return VectorStoreType.PGVECTOR;
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(properties.getPgvector().getJdbcUrl());
    }

    @Override
    public VectorStoreSession create(
            AiKnowledgeBaseEntity knowledgeBase
    ) {
        String tableName = VectorStoreProviderSupport.collectionName(
                properties,
                knowledgeBase
        );
        VectorStoreProperties.PgVector config = properties.getPgvector();
        PgVectorStore.Builder builder = PgVectorStore.builder()
                .jdbcUrl(config.getJdbcUrl())
                .tableName(tableName)
                .schema(schema(config))
                .dimensions(knowledgeBase.getEmbeddingDimension())
                .distanceType(PgVectorStore.DistanceType.COSINE)
                .connectionTimeoutMs(
                        config.getConnectionTimeout().toMillis()
                );
        if (StringUtils.hasText(config.getUsername())) {
            builder.username(config.getUsername());
        }
        if (StringUtils.hasText(config.getPassword())) {
            builder.password(config.getPassword());
        }
        try {
            return new VectorStoreSession(builder.build());
        } catch (VectorStoreException error) {
            throw new KnowledgeOperationException(
                    "无法创建 PgVector Store",
                    error
            );
        }
    }

    @Override
    public void deleteCollection(AiKnowledgeBaseEntity knowledgeBase) {
        String tableName = VectorStoreProviderSupport.collectionName(
                properties,
                knowledgeBase
        );
        VectorStoreProperties.PgVector config = properties.getPgvector();
        Properties connectionProperties = new Properties();
        if (StringUtils.hasText(config.getUsername())) {
            connectionProperties.setProperty("user", config.getUsername());
        }
        if (StringUtils.hasText(config.getPassword())) {
            connectionProperties.setProperty("password", config.getPassword());
        }
        connectionProperties.setProperty(
                "connectTimeout",
                String.valueOf(timeoutSeconds(config.getConnectionTimeout()))
        );
        String sql = "DROP TABLE IF EXISTS "
                + quoteIdentifier(schema(config))
                + "."
                + quoteIdentifier(tableName);
        try (Connection connection = DriverManager.getConnection(
                config.getJdbcUrl(),
                connectionProperties
        ); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException error) {
            throw new KnowledgeOperationException(
                    "无法删除 PgVector table",
                    error
            );
        }
    }

    private static String schema(VectorStoreProperties.PgVector config) {
        return StringUtils.hasText(config.getSchema())
                ? config.getSchema()
                : "public";
    }

    private static long timeoutSeconds(Duration timeout) {
        return Math.max(1L, timeout.toSeconds());
    }

    private static String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
