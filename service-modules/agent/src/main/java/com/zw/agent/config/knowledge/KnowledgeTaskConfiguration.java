package com.zw.agent.config.knowledge;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableScheduling
public class KnowledgeTaskConfiguration {

    @Bean("knowledgeTaskExecutor")
    public Executor knowledgeTaskExecutor(KnowledgeProperties properties) {
        KnowledgeProperties.Tasks taskProperties = properties.getTasks();
        int concurrency = Math.max(1, taskProperties.getConcurrency());
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(concurrency);
        executor.setMaxPoolSize(concurrency);
        // MySQL 是唯一任务队列；本地执行器只接受已有真实执行槽位的任务。
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("knowledge-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }
}
