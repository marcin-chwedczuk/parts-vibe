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
import java.util.concurrent.CompletionException;
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
    private final Counter doneCounter;
    private final Counter failedCounter;
    private final Counter timeoutCancelledCounter;
    private final DurationMetrics queueLagMetrics;
    private final DurationMetrics processingDurationMetrics;

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
        this.doneCounter = meterRegistry.counter("app.event-queue.events.done");
        this.failedCounter = meterRegistry.counter("app.event-queue.events.failed");
        this.timeoutCancelledCounter = meterRegistry.counter("app.event-queue.events.timed-out");

        this.queueLagMetrics = new DurationMetrics(meterRegistry, "app.event-queue.events.lag");
        this.processingDurationMetrics = new DurationMetrics(meterRegistry, "app.event-queue.events.processing-time");

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
        Instant claimedAt = timeProvider.now();
        List<ClaimedEventQueueEntry> claimed = eventQueueRepository.claimBatchForProcessing(
                claimSize, properties.getMaxAttempts(), workerId, claimedAt);
        if (claimed.isEmpty()) {
            log.debug("No event queue entries claimed. workerId={}", workerId);
            return;
        }

        claimedCounter.increment(claimed.size());
        log.debug("Claimed event queue entries. workerId={}, claimedCount={}", workerId, claimed.size());
        for (ClaimedEventQueueEntry entry : claimed) {
            recordQueueLag(entry, claimedAt);
            submitEvent(entry);
        }
    }

    private void submitEvent(ClaimedEventQueueEntry entry) {
        inFlightSlots.acquireUninterruptibly();
        Instant processingStartedAt = timeProvider.now();

        CompletableFuture<Void> future;
        try {
            future = CompletableFuture.runAsync(() -> eventQueueConsumer.handle(entry), eventQueueExecutor);
        } catch (RejectedExecutionException ex) {
            inFlightSlots.release();
            releaseEventClaim(entry, ex);
            return;
        }

        AtomicReference<ScheduledFuture<?>> timeoutTaskRef = new AtomicReference<>();
        future.whenComplete((ignored, throwable) -> {
            try {
                ScheduledFuture<?> timeoutTask = timeoutTaskRef.get();
                if (timeoutTask != null) {
                    timeoutTask.cancel(false);
                }

                if (throwable == null) {
                    markDone(entry);
                } else if (future.isCancelled()) {
                    log.debug("Event queue task was cancelled. entry={}", entry.toStringWithoutPayload());
                } else {
                    Throwable cause = unwrapCompletionThrowable(throwable);
                    markFailed(entry, cause);
                    log.debug(
                            "Event queue task completed exceptionally. entry={}, errorType={}",
                            entry.toStringWithoutPayload(),
                            cause.getClass().getSimpleName());
                }
            } finally {
                inFlightSlots.release();
                processingDurationMetrics.recordDurationBetween(processingStartedAt, timeProvider.now());
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
                            markTimedOut(entry, timeoutMs);
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
            } catch (Exception ex) {
                // Timeout watchdog is best-effort. If scheduler fails, keep processing and rely on stale recovery.
                log.warn(
                        "Timeout watchdog scheduling failed. entry={}, timeoutMs={}",
                        entry.toStringWithoutPayload(),
                        timeoutMs,
                        ex);
            }
        }

        log.debug("Submitted event queue entry to executor. entry={}", entry.toStringWithoutPayload());
    }

    private void markDone(ClaimedEventQueueEntry entry) {
        Instant now = timeProvider.now();
        int updated = eventQueueRepository.markDone(entry.id(), now);
        if (updated > 0) {
            doneCounter.increment();
            log.info("Event queue entry processed successfully. entry={}", entry.toStringWithoutPayload());
        } else {
            log.debug(
                    "Skipping DONE transition because event queue entry is no longer PROCESSING. entry={}",
                    entry.toStringWithoutPayload());
        }
    }

    private void markFailed(ClaimedEventQueueEntry entry, Throwable errorCause) {
        Instant now = timeProvider.now();
        Instant nextAttemptAt = now.plusMillis(computeBackoffMs(entry.attemptCount()));
        String error = truncatedError(errorCause);
        int updated = eventQueueRepository.markFailed(entry.id(), nextAttemptAt, error, now);
        if (updated > 0) {
            failedCounter.increment();
            if (entry.attemptCount() < properties.getMaxAttempts()) {
                log.debug(
                        "Scheduled event queue retry. entry={}, nextAttemptAt={}",
                        entry.toStringWithoutPayload(),
                        nextAttemptAt);
            } else {
                log.warn(
                        "Event queue entry reached max attempts and will remain FAILED. entry={}",
                        entry.toStringWithoutPayload());
            }
            log.error(
                    "Event queue processing failed. entry={}, nextAttemptAt={}, errorSummary={}",
                    entry.toStringWithoutPayload(),
                    nextAttemptAt,
                    error,
                    errorCause);
        } else {
            log.debug(
                    "Skipping FAILED transition because event queue entry is no longer PROCESSING. entry={}, errorSummary={}",
                    entry.toStringWithoutPayload(),
                    error);
        }
    }

    private void markTimedOut(ClaimedEventQueueEntry entry, long timeoutMs) {
        markFailed(entry, new IllegalStateException("Event handler timed out after %d ms".formatted(timeoutMs)));
    }

    private void recordQueueLag(ClaimedEventQueueEntry entry, Instant claimedAt) {
        queueLagMetrics.recordDurationBetween(entry.occurredAt(), claimedAt);
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

    private long computeBackoffMs(int attemptCount) {
        double scaled = properties.getBackoffInitialMs()
                * Math.pow(properties.getBackoffMultiplier(), Math.max(0, attemptCount - 1));
        long backoff = Math.round(scaled);
        return Math.min(backoff, properties.getBackoffMaxMs());
    }

    private static Throwable unwrapCompletionThrowable(Throwable throwable) {
        if (throwable instanceof CompletionException completionException && completionException.getCause() != null) {
            return completionException.getCause();
        }
        return throwable;
    }

    private static String truncatedError(Throwable throwable) {
        String text = throwable == null ? null : throwable.getMessage();
        String rootCauseSummary = rootCauseSummary(throwable);
        if (text == null || text.isBlank()) {
            text = rootCauseSummary;
        } else if (!text.contains(rootCauseSummary)) {
            text = text + " | rootCause=" + rootCauseSummary;
        }
        if (text.length() <= 2000) {
            return text;
        }
        return text.substring(0, 2000);
    }

    private static String rootCauseSummary(Throwable throwable) {
        if (throwable == null) {
            return "<no-cause>";
        }
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String rootMessage = root.getMessage();
        if (rootMessage == null || rootMessage.isBlank()) {
            rootMessage = "<no-message>";
        }
        return root.getClass().getSimpleName() + ": " + rootMessage;
    }
}
