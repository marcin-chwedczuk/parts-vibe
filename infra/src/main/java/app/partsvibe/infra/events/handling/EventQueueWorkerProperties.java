package app.partsvibe.infra.events.handling;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.events.worker")
@Data
public class EventQueueWorkerProperties {
    private boolean enabled = false;
    private long pollIntervalMs = 1000;
    private long staleRecoveryIntervalMs = 60000;
    private long retentionCleanupIntervalMs = 3600000;
    private int batchSize = 20;
    private int poolSize = 4;
    private int queueCapacity = 200;
    private int retentionDeleteBatchSize = 500;
    private int doneRetentionDays = 30;
    private int failedRetentionDays = 90;
    private long processingTimeoutMs = 120000;
    private int maxAttempts = 10;
    private long backoffInitialMs = 1000;
    private double backoffMultiplier = 2.0;
    private long backoffMaxMs = 300000;
    private long handlerTimeoutMs = 60000;
}
