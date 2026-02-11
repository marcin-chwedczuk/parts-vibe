package app.partsvibe.infra.events;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventWorker {
    private static final Logger log = LoggerFactory.getLogger(OutboxEventWorker.class);

    private final EventWorkerProperties properties;
    private final OutboxEventClaimService claimService;
    private final OutboxEventProcessor processor;
    private final ThreadPoolTaskExecutor executor;
    private final String workerId;
    private final Counter claimedCounter;
    private final Counter staleRequeuedCounter;
    private final Counter pollSkippedCounter;
    private final Counter executorRejectedCounter;

    public OutboxEventWorker(
            EventWorkerProperties properties,
            OutboxEventClaimService claimService,
            OutboxEventProcessor processor,
            @Qualifier("outboxEventExecutor") ThreadPoolTaskExecutor executor,
            MeterRegistry meterRegistry) {
        this.properties = properties;
        this.claimService = claimService;
        this.processor = processor;
        this.executor = executor;
        this.workerId = "worker-" + UUID.randomUUID();
        this.claimedCounter = meterRegistry.counter("app.events.worker.claimed");
        this.staleRequeuedCounter = meterRegistry.counter("app.events.worker.stale.requeued");
        this.pollSkippedCounter = meterRegistry.counter("app.events.worker.poll.skipped");
        this.executorRejectedCounter = meterRegistry.counter("app.events.worker.executor.rejected");
        meterRegistry.gauge("app.events.worker.executor.queue.size", executor, e -> e.getThreadPoolExecutor()
                .getQueue()
                .size());
        meterRegistry.gauge("app.events.worker.executor.active", executor, ThreadPoolTaskExecutor::getActiveCount);
    }

    @Scheduled(fixedDelayString = "${app.events.worker.poll-interval-ms:1000}")
    public void pollAndDispatch() {
        if (!properties.isEnabled()) {
            return;
        }

        int staleRecovered = claimService.requeueStaleProcessing(properties.getProcessingTimeoutMs());
        if (staleRecovered > 0) {
            staleRequeuedCounter.increment(staleRecovered);
            log.warn("Recovered stale PROCESSING outbox events. count={}", staleRecovered);
        }

        int capacity = availableExecutorCapacity();
        if (capacity <= 0) {
            pollSkippedCounter.increment();
            return;
        }

        int claimSize = Math.min(properties.getBatchSize(), capacity);
        List<ClaimedOutboxEvent> claimed = claimService.claimBatch(claimSize, properties.getMaxAttempts(), workerId);
        if (claimed.isEmpty()) {
            return;
        }

        claimedCounter.increment(claimed.size());
        for (ClaimedOutboxEvent event : claimed) {
            try {
                executor.execute(() -> processor.processClaimedEvent(event));
            } catch (RuntimeException ex) {
                executorRejectedCounter.increment();
                log.error(
                        "Failed to submit outbox event to executor. id={}, eventId={}, eventType={}",
                        event.id(),
                        event.eventId(),
                        event.eventType(),
                        ex);
                processor.processClaimedEvent(event);
            }
        }
    }

    private int availableExecutorCapacity() {
        if (executor.getThreadPoolExecutor() == null) {
            return properties.getBatchSize();
        }
        int queueRemaining = executor.getThreadPoolExecutor().getQueue().remainingCapacity();
        int active = executor.getActiveCount();
        int threadSlots = Math.max(0, properties.getPoolSize() - active);
        return queueRemaining + threadSlots;
    }
}
