package app.partsvibe.infra.events.handling;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.events.worker")
@Getter
@Setter
public class EventQueueWorkerProperties {
    private boolean enabled = false;
    private long pollIntervalMs = 1000;
    private int batchSize = 20;
    private int poolSize = 4;
    private int queueCapacity = 200;
    private long processingTimeoutMs = 120000;
    private int maxAttempts = 10;
    private long backoffInitialMs = 1000;
    private double backoffMultiplier = 2.0;
    private long backoffMaxMs = 300000;
    private long handlerTimeoutMs = 60000;
}
