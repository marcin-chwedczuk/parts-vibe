package app.partsvibe.infra.events.handling;

import app.partsvibe.infra.events.jpa.ClaimedEventQueueEntry;
import app.partsvibe.infra.events.jpa.EventQueueRepository;
import app.partsvibe.shared.time.TimeProvider;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventQueueDispatcher {
    private static final Logger log = LoggerFactory.getLogger(EventQueueDispatcher.class);

    private final EventQueueWorkerProperties properties;
    private final EventQueueRepository eventQueueRepository;
    private final EventQueueConsumer eventQueueConsumer;
    private final TimeProvider timeProvider;
    private final Executor eventQueueExecutor;
    private final ScheduledExecutorService eventQueueTimeoutScheduler;
    private final Semaphore inFlightSlots;
    private final AtomicInteger inFlightTasks;
    private final String workerId;
    private final Counter claimedCounter;
    private final Counter pollSkippedCounter;
    private final Counter executorRejectedCounter;
    private final Counter timeoutCancelledCounter;

    public EventQueueDispatcher(
            EventQueueWorkerProperties properties,
            EventQueueRepository eventQueueRepository,
            EventQueueConsumer eventQueueConsumer,
            TimeProvider timeProvider,
            @Qualifier("eventQueueExecutor") Executor eventQueueExecutor,
            @Qualifier("eventQueueTimeoutScheduler") ScheduledExecutorService eventQueueTimeoutScheduler,
            MeterRegistry meterRegistry) {
        this.properties = properties;
        this.eventQueueRepository = eventQueueRepository;
        this.eventQueueConsumer = eventQueueConsumer;
        this.timeProvider = timeProvider;
        this.eventQueueExecutor = eventQueueExecutor;
        this.eventQueueTimeoutScheduler = eventQueueTimeoutScheduler;
        this.inFlightSlots = new Semaphore(properties.getPoolSize() + properties.getQueueCapacity());
        this.inFlightTasks = new AtomicInteger(0);
        this.workerId = "worker-" + UUID.randomUUID();

        this.claimedCounter = meterRegistry.counter("app.events.worker.claimed");
        this.pollSkippedCounter = meterRegistry.counter("app.events.worker.poll.skipped");
        this.executorRejectedCounter = meterRegistry.counter("app.events.worker.executor.rejected");
        this.timeoutCancelledCounter = meterRegistry.counter("app.events.worker.timeout.cancelled");

        meterRegistry.gauge("app.events.worker.inflight", inFlightTasks, AtomicInteger::get);
        meterRegistry.gauge("app.events.worker.permits.available", inFlightSlots, Semaphore::availablePermits);

        log.info(
                "Event queue dispatcher initialized. enabled={}, pollIntervalMs={}, batchSize={}, poolSize={}, queueCapacity={}, maxAttempts={}, handlerTimeoutMs={}",
                properties.isEnabled(),
                properties.getPollIntervalMs(),
                properties.getBatchSize(),
                properties.getPoolSize(),
                properties.getQueueCapacity(),
                properties.getMaxAttempts(),
                properties.getHandlerTimeoutMs());
    }

    @Scheduled(fixedDelayString = "${app.events.worker.poll-interval-ms:1000}")
    public void pollAndDispatch() {
        if (!properties.isEnabled()) {
            log.debug("Event queue dispatcher poll skipped because worker is disabled");
            return;
        }

        int capacity = inFlightSlots.availablePermits();
        if (capacity <= 0) {
            pollSkippedCounter.increment();
            log.debug("Event queue dispatcher poll skipped due to no in-flight capacity. workerId={}", workerId);
            return;
        }

        int claimSize = Math.min(properties.getBatchSize(), capacity);
        List<ClaimedEventQueueEntry> claimed = eventQueueRepository.claimBatchForProcessing(
                claimSize, properties.getMaxAttempts(), workerId, timeProvider.now());
        if (claimed.isEmpty()) {
            log.debug("No event queue entries claimed. workerId={}", workerId);
            return;
        }

        claimedCounter.increment(claimed.size());
        log.debug("Claimed event queue entries. workerId={}, claimedCount={}", workerId, claimed.size());
        for (ClaimedEventQueueEntry entry : claimed) {
            submitEvent(entry);
        }
    }

    private void submitEvent(ClaimedEventQueueEntry entry) {
        inFlightSlots.acquireUninterruptibly();
        inFlightTasks.incrementAndGet();

        try {
            CompletableFuture<Void> future =
                    CompletableFuture.runAsync(() -> eventQueueConsumer.consume(entry), eventQueueExecutor);

            long timeoutMs = properties.getHandlerTimeoutMs();
            java.util.concurrent.ScheduledFuture<?> timeoutTask = null;
            if (timeoutMs > 0) {
                timeoutTask = eventQueueTimeoutScheduler.schedule(
                        () -> {
                            eventQueueConsumer.markTimedOutIfProcessing(entry, timeoutMs);
                            if (future.cancel(true)) {
                                timeoutCancelledCounter.increment();
                                log.warn(
                                        "Cancelled timed out event queue task. entry={}, timeoutMs={}",
                                        entry.toStringWithoutPayload(),
                                        timeoutMs);
                            }
                        },
                        timeoutMs,
                        TimeUnit.MILLISECONDS);
            }

            java.util.concurrent.ScheduledFuture<?> finalTimeoutTask = timeoutTask;
            future.whenComplete((ignored, throwable) -> {
                if (finalTimeoutTask != null) {
                    finalTimeoutTask.cancel(false);
                }
                inFlightTasks.decrementAndGet();
                inFlightSlots.release();

                if (throwable != null) {
                    log.debug(
                            "Event queue task completed exceptionally (failure already handled by consumer or timeout fallback). entry={}, errorType={}",
                            entry.toStringWithoutPayload(),
                            throwable.getClass().getSimpleName());
                }
            });

            log.debug(
                    "Submitted event queue entry to executor. entry={}", entry.toStringWithoutPayload());
        } catch (RejectedExecutionException ex) {
            inFlightTasks.decrementAndGet();
            inFlightSlots.release();
            executorRejectedCounter.increment();
            log.error(
                    "Failed to submit event queue entry to executor. entry={}",
                    entry.toStringWithoutPayload(),
                    ex);
            eventQueueConsumer.markFailedIfProcessing(
                    entry, new IllegalStateException("Event queue executor rejected submitted task", ex));
        }
    }
}
