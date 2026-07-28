package com.zw.agent.config.milvus;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@Validated
@ConfigurationProperties(prefix = "rag.store.milvus")
public class MilvusProperties {

    /**
     * Milvus Proxy 地址
     */
    private String host;

    /**
     * Milvus 数据库名称。
     */
    private String database = "default";

    /**
     * 用户名。
     */
    private String username;

    /**
     * 密码。
     */
    private String password;

    /**
     * Token。
     *
     */
    private String token;

    /**
     * 建立连接超时。
     */
    private Duration connectTimeout = Duration.ofSeconds(10);

    /**
     * keep-alive 响应超时。
     */
    private Duration keepAliveTimeout = Duration.ofSeconds(10);
}
