package com.zw.agent.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.HarnessAgent;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CacheConfig {

    @Bean
    public Cache<String, HarnessAgent> agentCache() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterAccess(Duration.ofMinutes(60*24*30))
                .build();
    }

    @Bean
    public Cache<String, Toolkit> toolkitCache() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterAccess(Duration.ofMinutes(60*24*30))
                .build();
    }

    @Bean
    public Cache<String, PermissionContextState> userToolPermissionCache() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterAccess(Duration.ofMinutes(60*24*30))
                .build();
    }

}
