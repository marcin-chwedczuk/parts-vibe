package app.partsvibe.infra.events;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.events.worker")
public class EventWorkerProperties {
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public long getProcessingTimeoutMs() {
        return processingTimeoutMs;
    }

    public void setProcessingTimeoutMs(long processingTimeoutMs) {
        this.processingTimeoutMs = processingTimeoutMs;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getBackoffInitialMs() {
        return backoffInitialMs;
    }

    public void setBackoffInitialMs(long backoffInitialMs) {
        this.backoffInitialMs = backoffInitialMs;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public void setBackoffMultiplier(double backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
    }

    public long getBackoffMaxMs() {
        return backoffMaxMs;
    }

    public void setBackoffMaxMs(long backoffMaxMs) {
        this.backoffMaxMs = backoffMaxMs;
    }

    public long getHandlerTimeoutMs() {
        return handlerTimeoutMs;
    }

    public void setHandlerTimeoutMs(long handlerTimeoutMs) {
        this.handlerTimeoutMs = handlerTimeoutMs;
    }
}
