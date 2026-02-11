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
        log.info(
                "Outbox event worker initialized. enabled={}, pollIntervalMs={}, batchSize={}, poolSize={}, queueCapacity={}, maxAttempts={}",
                properties.isEnabled(),
                properties.getPollIntervalMs(),
                properties.getBatchSize(),
                properties.getPoolSize(),
                properties.getQueueCapacity(),
                properties.getMaxAttempts());
    }

    @Scheduled(fixedDelayString = "${app.events.worker.poll-interval-ms:1000}")
    public void pollAndDispatch() {
        if (!properties.isEnabled()) {
            log.debug("Outbox worker poll skipped because worker is disabled");
            return;
        }

        log.debug(
                "Outbox worker poll started. workerId={}, batchSize={}, poolSize={}, queueCapacity={}",
                workerId,
                properties.getBatchSize(),
                properties.getPoolSize(),
                properties.getQueueCapacity());

        int staleRecovered = claimService.requeueStaleProcessing(properties.getProcessingTimeoutMs());
        if (staleRecovered > 0) {
            staleRequeuedCounter.increment(staleRecovered);
            log.warn("Recovered stale PROCESSING outbox events. count={}", staleRecovered);
        } else {
            log.debug("No stale PROCESSING outbox events found");
        }

        int capacity = availableExecutorCapacity();
        if (capacity <= 0) {
            pollSkippedCounter.increment();
            log.debug("Outbox worker poll skipped due to no executor capacity. workerId={}", workerId);
            return;
        }

        int claimSize = Math.min(properties.getBatchSize(), capacity);
        log.debug(
                "Attempting to claim outbox events. workerId={}, claimSize={}, maxAttempts={}",
                workerId,
                claimSize,
                properties.getMaxAttempts());
        List<ClaimedOutboxEvent> claimed = claimService.claimBatch(claimSize, properties.getMaxAttempts(), workerId);
        if (claimed.isEmpty()) {
            log.debug("No outbox events claimed. workerId={}", workerId);
            return;
        }

        claimedCounter.increment(claimed.size());
        log.debug("Claimed outbox events. workerId={}, claimedCount={}", workerId, claimed.size());
        for (ClaimedOutboxEvent event : claimed) {
            try {
                log.debug(
                        "Submitting outbox event to executor. id={}, eventId={}, eventType={}, attemptCount={}",
                        event.id(),
                        event.eventId(),
                        event.eventType(),
                        event.attemptCount());
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
