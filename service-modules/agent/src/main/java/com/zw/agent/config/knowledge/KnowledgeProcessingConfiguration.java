package com.zw.agent.config.knowledge;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class KnowledgeProcessingConfiguration {

    @Bean("knowledgeDispatchExecutor")
    public Executor knowledgeDispatchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setThreadNamePrefix("knowledge-dispatch-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }

    @Bean("knowledgeProcessingExecutor")
    public Executor knowledgeProcessingExecutor(KnowledgeProperties properties) {
        KnowledgeProperties.Processing processing = properties.getProcessing();
        int concurrency = Math.max(1, processing.getConcurrency());
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(concurrency);
        executor.setMaxPoolSize(concurrency);
        executor.setQueueCapacity(
                Math.max(0, processing.getQueueCapacity())
        );
        executor.setThreadNamePrefix("knowledge-processing-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }
}
