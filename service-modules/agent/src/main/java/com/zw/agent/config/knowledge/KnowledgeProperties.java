package com.zw.agent.config.knowledge;

import java.nio.file.Path;
import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@Data
@ConfigurationProperties(prefix = "knowledge")
public class KnowledgeProperties {

    private Source source = new Source();

    private Tasks tasks = new Tasks();

    @Data
    public static class Source {

        private Path root = Path.of("knowledge-uploads");

        private DataSize maxFileSize = DataSize.ofMegabytes(50);
    }

    @Data
    public static class Tasks {

        private Duration pollInterval = Duration.ofSeconds(2);

        private Duration leaseDuration = Duration.ofMinutes(30);

        private int concurrency = 2;

        private int claimBatchSize = 4;

        private int embeddingBatchSize = 20;
    }
}
