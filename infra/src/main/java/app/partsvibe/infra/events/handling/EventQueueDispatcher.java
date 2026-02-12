package app.partsvibe.infra.events.handling;

import app.partsvibe.infra.events.jpa.ClaimedEventQueueEntry;
import app.partsvibe.infra.events.jpa.EventQueueRepository;
import app.partsvibe.shared.time.TimeProvider;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventQueueDispatcher {
    private static final Logger log = LoggerFactory.getLogger(EventQueueDispatcher.class);

    private final EventQueueDispatcherProperties properties;
    private final EventQueueRepository eventQueueRepository;
    private final EventQueueConsumer eventQueueConsumer;
    private final TimeProvider timeProvider;
    private final Executor eventQueueExecutor;
    private final ScheduledExecutorService eventQueueTimeoutScheduler;
    private final Semaphore inFlightSlots;
    private final String workerId;
    private final Counter claimedCounter;
    private final Counter timeoutCancelledCounter;

    public EventQueueDispatcher(
            EventQueueDispatcherProperties properties,
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
        this.inFlightSlots = new Semaphore(properties.getThreadPoolSize());
        this.workerId = "worker-" + UUID.randomUUID();

        this.claimedCounter = meterRegistry.counter("app.event-queue.events.claimed");
        this.timeoutCancelledCounter = meterRegistry.counter("app.event-queue.events.timed-out");

        log.info("Event queue dispatcher initialized. properties={}", properties);
    }

    @Scheduled(fixedDelayString = "${app.events.dispatcher.poll-interval-ms:1000}")
    public void pollAndDispatch() {
        if (!properties.isEnabled()) {
            log.debug("Event queue dispatcher poll skipped because worker is disabled");
            return;
        }

        int capacity = inFlightSlots.availablePermits();
        if (capacity <= 0) {
            log.debug("Event queue dispatcher poll skipped due to no in-flight capacity. workerId={}", workerId);
            return;
        }

        int claimSize = capacity;
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

        CompletableFuture<Void> future;
        try {
            future = CompletableFuture.runAsync(() -> eventQueueConsumer.consume(entry), eventQueueExecutor);
        } catch (RejectedExecutionException ex) {
            inFlightSlots.release();
            releaseEventClaim(entry, ex);
            return;
        }

        AtomicReference<ScheduledFuture<?>> timeoutTaskRef = new AtomicReference<>();
        future.whenComplete((ignored, throwable) -> {
            ScheduledFuture<?> timeoutTask = timeoutTaskRef.get();
            if (timeoutTask != null) {
                timeoutTask.cancel(false);
            }
            inFlightSlots.release();

            if (throwable != null) {
                log.debug(
                        "Event queue task completed exceptionally (failure already handled by consumer or timeout fallback). entry={}, errorType={}",
                        entry.toStringWithoutPayload(),
                        throwable.getClass().getSimpleName());
            }
        });

        long timeoutMs = properties.getHandlerTimeoutMs();
        if (timeoutMs > 0) {
            try {
                ScheduledFuture<?> timeoutTask = eventQueueTimeoutScheduler.schedule(
                        () -> {
                            if (future.isDone()) {
                                return;
                            }
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
                timeoutTaskRef.set(timeoutTask);
            } catch (RuntimeException ex) {
                // Timeout watchdog is best-effort. If scheduler rejects, keep processing and rely on stale recovery.
                log.warn(
                        "Timeout watchdog scheduling failed. entry={}, timeoutMs={}",
                        entry.toStringWithoutPayload(),
                        timeoutMs,
                        ex);
            }
        }

        log.debug("Submitted event queue entry to executor. entry={}", entry.toStringWithoutPayload());
    }

    private void releaseEventClaim(ClaimedEventQueueEntry entry, RejectedExecutionException ex) {
        Instant now = timeProvider.now();
        int updated = eventQueueRepository.releaseForRetry(entry.id(), now, now);
        if (updated > 0) {
            log.warn(
                    "Event queue entry released for retry after executor rejection. entry={}",
                    entry.toStringWithoutPayload(),
                    ex);
        } else {
            log.debug(
                    "Executor rejected task but event queue entry was not released (status changed concurrently). entry={}",
                    entry.toStringWithoutPayload(),
                    ex);
        }
    }
}
