package com.zw.agent.factory.stateStoreFactory;

import com.zw.agent.entity.DTO.AgentConfigDTO;
import io.agentscope.core.state.JsonFileAgentStateStore;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.extensions.mysql.state.MysqlAgentStateStore;
import io.agentscope.extensions.redis.state.RedisAgentStateStore;
import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.RedisClient;
import io.lettuce.core.cluster.RedisClusterClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@RequiredArgsConstructor
@Component
public class StateStoreFactory {

    static final String REDIS_KEY_PREFIX = "agent-scope:state:";
    static final String MYSQL_TABLE_NAME = "ai_state_store";

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    private final ConcurrentMap<Long, AgentStateStore> localStores = new ConcurrentHashMap<>();
    private final Set<Long> missingTypeWarnings = ConcurrentHashMap.newKeySet();
    private final Object redisStoreMonitor = new Object();
    private final Object mysqlStoreMonitor = new Object();

    private volatile AgentStateStore redisStore;
    private volatile AgentStateStore mysqlStore;

    public AgentStateStore buildStateStore(AgentConfigDTO config) {
        if (config == null) {
            throw new IllegalArgumentException("智能体配置不能为空");
        }
        Long agentId = config.getAgentId();
        if (agentId == null) {
            throw new IllegalArgumentException("智能体配置中的 agentId 不能为空");
        }

        StateStoreType type = config.getStateStoreType();
        if (type == null) {
            type = StateStoreType.LOCAL_FILE;
            if (missingTypeWarnings.add(agentId)) {
                log.warn("智能体 {} 未配置会话状态存储，使用 LOCAL_FILE", agentId);
            }
        }

        return switch (type) {
            case LOCAL_FILE -> buildLocalStore(agentId);
            case REDIS -> buildRedisStore();
            case MYSQL -> buildMysqlStore();
        };
    }

    private AgentStateStore buildLocalStore(Long agentId) {
        return localStores.computeIfAbsent(
                agentId,
                id -> {
                    Path stateDirectory = resolveStateHome().resolve(String.valueOf(id));
                    log.info("为智能体 {} 初始化本地会话状态存储: {}", id, stateDirectory);
                    return new JsonFileAgentStateStore(stateDirectory);
                }
        );
    }

    private AgentStateStore buildRedisStore() {
        AgentStateStore current = redisStore;
        if (current != null) {
            return current;
        }
        synchronized (redisStoreMonitor) {
            if (redisStore == null) {
                redisStore = createRedisStore();
                log.info("已初始化 Redis 会话状态存储，keyPrefix={}", REDIS_KEY_PREFIX);
            }
            return redisStore;
        }
    }

    private AgentStateStore createRedisStore() {
        if (!(redisConnectionFactory instanceof LettuceConnectionFactory lettuceFactory)) {
            throw new IllegalStateException(
                    "REDIS 会话状态存储要求使用 LettuceConnectionFactory，当前类型为 "
                            + redisConnectionFactory.getClass().getName()
            );
        }

        AbstractRedisClient nativeClient = lettuceFactory.getRequiredNativeClient();
        RedisAgentStateStore.Builder builder = RedisAgentStateStore.builder()
                .keyPrefix(REDIS_KEY_PREFIX);
        if (nativeClient instanceof RedisClient redisClient) {
            return builder.lettuceClient(redisClient).build();
        }
        if (nativeClient instanceof RedisClusterClient clusterClient) {
            return builder.lettuceClusterClient(clusterClient).build();
        }
        throw new IllegalStateException(
                "不支持的 Lettuce 客户端类型: " + nativeClient.getClass().getName()
        );
    }

    private AgentStateStore buildMysqlStore() {
        AgentStateStore current = mysqlStore;
        if (current != null) {
            return current;
        }
        synchronized (mysqlStoreMonitor) {
            if (mysqlStore == null) {
                String databaseName = resolveCurrentDatabaseName();
                mysqlStore = new MysqlAgentStateStore(
                        dataSource,
                        databaseName,
                        MYSQL_TABLE_NAME,
                        false
                );
                log.info(
                        "已初始化 MySQL 会话状态存储，database={}, table={}",
                        databaseName,
                        MYSQL_TABLE_NAME
                );
            }
            return mysqlStore;
        }
    }

    private String resolveCurrentDatabaseName() {
        try (Connection connection = dataSource.getConnection()) {
            String databaseName = connection.getCatalog();
            if (databaseName == null || databaseName.isBlank()) {
                databaseName = connection.getSchema();
            }
            if (databaseName == null || databaseName.isBlank()) {
                throw new IllegalStateException("无法从主数据源解析当前 MySQL 数据库名");
            }
            return databaseName.trim();
        } catch (SQLException exception) {
            throw new IllegalStateException("无法连接主数据源以初始化 MySQL 会话状态存储", exception);
        }
    }

    private Path resolveStateHome() {
        String override = System.getProperty("agentscope.state.home");
        Path stateHome = override != null && !override.isBlank()
                ? Path.of(override)
                : Path.of(System.getProperty("user.home"), ".agentscope", "state");
        return stateHome.toAbsolutePath().normalize();
    }
}
