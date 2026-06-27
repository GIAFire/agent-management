package com.zw.agent.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.agentscope.harness.agent.HarnessAgent;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AgentCacheConfig {

    @Bean
    public Cache<String, HarnessAgent> localAgentCache() {
        return Caffeine.newBuilder()
                .maximumSize(500)  // 最大缓存500个Agent实例
                .expireAfterAccess(Duration.ofMinutes(60*24*30)) // 三十天后Agent实例缓存失效
                .build();
    }
}
