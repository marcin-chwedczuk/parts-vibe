package app.partsvibe.infra.events.handling;

import app.partsvibe.infra.events.jpa.ClaimedEventQueueEntry;
import app.partsvibe.infra.events.jpa.EventQueueEntry;
import app.partsvibe.infra.events.jpa.EventQueueRepository;
import app.partsvibe.infra.utils.ThrowableUtils;
import app.partsvibe.shared.time.TimeProvider;
import app.partsvibe.shared.utils.StringUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
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
    private final String dispatcherId;
    private final Counter claimedCounter;
    private final Counter doneCounter;
    private final Counter failedCounter;
    private final Counter timeoutCancelledCounter;
    private final DurationMetrics queueLagMetrics;
    private final DurationMetrics processingDurationMetrics;
    private final ExponentialBackoffCalculator backoffCalculator;

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
        this.dispatcherId = "dispatcher-" + UUID.randomUUID();

        this.claimedCounter = meterRegistry.counter("app.event-queue.events.claimed");
        this.doneCounter = meterRegistry.counter("app.event-queue.events.done");
        this.failedCounter = meterRegistry.counter("app.event-queue.events.failed");
        this.timeoutCancelledCounter = meterRegistry.counter("app.event-queue.events.timed-out");

        this.queueLagMetrics = new DurationMetrics(meterRegistry, "app.event-queue.lag");
        this.processingDurationMetrics = new DurationMetrics(meterRegistry, "app.event-queue.processing-time");
        this.backoffCalculator = new ExponentialBackoffCalculator(
                properties.getBackoffInitialMs(), properties.getBackoffMultiplier(), properties.getBackoffMaxMs());

        log.info("Event queue dispatcher initialized. properties={}", properties);
    }

    @Scheduled(fixedDelayString = "${app.events.dispatcher.poll-interval-ms:1000}")
    public void pollAndDispatch() {
        if (!properties.isEnabled()) {
            log.debug("Event queue dispatcher poll skipped because dispatcher is disabled");
            return;
        }

        var capacity = inFlightSlots.availablePermits();
        if (capacity <= 0) {
            log.debug(
                    "Event queue dispatcher poll skipped due to no in-flight capacity. dispatcherId={}", dispatcherId);
            return;
        }

        var claimedAt = timeProvider.now();
        var claimed = eventQueueRepository.claimEntriesForProcessing(
                capacity, properties.getMaxAttempts(), dispatcherId, claimedAt);
        if (claimed.isEmpty()) {
            log.debug("No event queue entries claimed. dispatcherId={}", dispatcherId);
            return;
        }

        claimedCounter.increment(claimed.size());
        log.debug("Claimed event queue entries. dispatcherId={}, claimedCount={}", dispatcherId, claimed.size());
        for (ClaimedEventQueueEntry entry : claimed) {
            recordQueueLag(entry, claimedAt);
            submitEvent(entry);
        }
    }

    private void submitEvent(ClaimedEventQueueEntry event) {
        inFlightSlots.acquireUninterruptibly();
        var processingStartedAt = timeProvider.now();

        CompletableFuture<Void> future;
        try {
            future = CompletableFuture.runAsync(() -> eventQueueConsumer.handle(event), eventQueueExecutor);
        } catch (RejectedExecutionException ex) {
            inFlightSlots.release();
            releaseClaimedEntry(event, ex);
            return;
        }

        AtomicReference<ScheduledFuture<?>> timeoutTaskRef = new AtomicReference<>();
        future.whenComplete((ignored, throwable) -> {
            try {
                ScheduledFuture<?> timeoutTask = timeoutTaskRef.get();
                if (timeoutTask != null) {
                    timeoutTask.cancel(false);
                }
                processingDurationMetrics.recordDurationBetween(processingStartedAt, timeProvider.now());

                if (future.isCancelled()) {
                    log.debug("Event queue task was cancelled. event={}", event.toStringWithoutPayload());
                } else if (throwable == null) {
                    markDone(event);
                } else {
                    Throwable cause = ThrowableUtils.unwrapCompletionThrowable(throwable);
                    markFailed(event, cause);
                    log.debug(
                            "Event queue task completed exceptionally. event={}, errorType={}",
                            event.toStringWithoutPayload(),
                            cause.getClass().getSimpleName());
                }
            } finally {
                inFlightSlots.release();
            }
        });

        var timeoutMs = properties.getHandlerTimeoutMs();
        if (timeoutMs > 0) {
            try {
                ScheduledFuture<?> timeoutTask = eventQueueTimeoutScheduler.schedule(
                        () -> {
                            if (future.isDone()) {
                                return;
                            }
                            markTimedOut(event, timeoutMs);
                            if (future.cancel(true)) {
                                timeoutCancelledCounter.increment();
                                log.warn(
                                        "Cancelled timed out event queue task. event={}, timeoutMs={}",
                                        event.toStringWithoutPayload(),
                                        timeoutMs);
                            }
                        },
                        timeoutMs,
                        TimeUnit.MILLISECONDS);
                timeoutTaskRef.set(timeoutTask);
            } catch (Exception ex) {
                // Timeout watchdog is best-effort. If scheduler fails, keep processing and rely on stale recovery.
                log.warn(
                        "Timeout watchdog scheduling failed. event={}, timeoutMs={}",
                        event.toStringWithoutPayload(),
                        timeoutMs,
                        ex);
            }
        }

        log.debug("Submitted event queue event to executor. event={}", event.toStringWithoutPayload());
    }

    private void markDone(ClaimedEventQueueEntry entry) {
        var now = timeProvider.now();
        var updated = eventQueueRepository.markEntryAsDone(entry.id(), now);
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
        var now = timeProvider.now();
        var nextAttemptAt = now.plusMillis(backoffCalculator.computeDelayMs(entry.attemptCount()));
        var error = truncatedError(errorCause);
        var updated = eventQueueRepository.markEntryAsFailed(entry.id(), nextAttemptAt, error, now);
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

    private void releaseClaimedEntry(ClaimedEventQueueEntry entry, RejectedExecutionException ex) {
        var now = timeProvider.now();
        var updated = eventQueueRepository.releaseClaimedEntry(entry.id(), now, now);
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

    private static String truncatedError(Throwable throwable) {
        return StringUtils.truncate(ThrowableUtils.asString(throwable), EventQueueEntry.LAST_ERROR_MAX_LENGTH);
    }
}
