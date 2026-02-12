package app.partsvibe.infra.events.handling;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("app.events.dispatcher")
@Validated
@Data
public class EventQueueDispatcherProperties {
    private boolean enabled = true;

    @Min(100)
    private long pollIntervalMs = 1000;

    @Min(1)
    private int maxAttempts = 10;

    @Min(1)
    private int threadPoolSize = 4;

    @Min(0)
    private int threadPoolQueueCapacity = 200;

    @Min(1)
    private long handlerTimeoutMs = 60000;

    @Min(1)
    private long processingTimeoutMs = 120000;

    @Min(1)
    private long backoffInitialMs = 1000;

    @DecimalMin("1.0")
    private double backoffMultiplier = 2.0;

    @Min(1)
    private long backoffMaxMs = 300000;

    @Min(1000)
    private long staleRecoveryIntervalMs = 60000;

    @Min(1000)
    private long retentionCleanupIntervalMs = 3600000;

    @Min(1)
    private int retentionDeleteBatchSize = 500;

    @Min(1)
    private int doneRetentionDays = 30;

    @Min(1)
    private int failedRetentionDays = 90;
}
