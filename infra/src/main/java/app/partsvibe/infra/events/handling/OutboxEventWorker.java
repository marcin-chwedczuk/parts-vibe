package app.partsvibe.infra.events.handling;

import app.partsvibe.infra.events.jpa.ClaimedOutboxEvent;
import app.partsvibe.shared.time.TimeProvider;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final TimeProvider timeProvider;
    private final ThreadPoolTaskExecutor executor;
    private final CompletionService<EventExecutionOutcome> completionService;
    private final Semaphore inFlightSlots;
    private final ConcurrentMap<Future<EventExecutionOutcome>, RunningTask> runningTasks;
    private final String workerId;
    private final Counter claimedCounter;
    private final Counter staleRequeuedCounter;
    private final Counter pollSkippedCounter;
    private final Counter executorRejectedCounter;
    private final Counter timeoutCancelledCounter;

    public OutboxEventWorker(
            EventWorkerProperties properties,
            OutboxEventClaimService claimService,
            OutboxEventProcessor processor,
            TimeProvider timeProvider,
            @Qualifier("outboxEventExecutor") ThreadPoolTaskExecutor executor,
            MeterRegistry meterRegistry) {
        this.properties = properties;
        this.claimService = claimService;
        this.processor = processor;
        this.timeProvider = timeProvider;
        this.executor = executor;
        Executor completionExecutor =
                executor.getThreadPoolExecutor() != null ? executor.getThreadPoolExecutor() : executor;
        this.completionService = new ExecutorCompletionService<>(completionExecutor);
        this.inFlightSlots = new Semaphore(properties.getPoolSize() + properties.getQueueCapacity());
        this.runningTasks = new ConcurrentHashMap<>();
        this.workerId = "worker-" + UUID.randomUUID();
        this.claimedCounter = meterRegistry.counter("app.events.worker.claimed");
        this.staleRequeuedCounter = meterRegistry.counter("app.events.worker.stale.requeued");
        this.pollSkippedCounter = meterRegistry.counter("app.events.worker.poll.skipped");
        this.executorRejectedCounter = meterRegistry.counter("app.events.worker.executor.rejected");
        this.timeoutCancelledCounter = meterRegistry.counter("app.events.worker.timeout.cancelled");
        meterRegistry.gauge("app.events.worker.executor.queue.size", executor, e -> e.getThreadPoolExecutor()
                .getQueue()
                .size());
        meterRegistry.gauge("app.events.worker.executor.active", executor, ThreadPoolTaskExecutor::getActiveCount);
        meterRegistry.gauge("app.events.worker.inflight", runningTasks, Map::size);
        meterRegistry.gauge("app.events.worker.permits.available", inFlightSlots, Semaphore::availablePermits);
        log.info(
                "Outbox event worker initialized. enabled={}, pollIntervalMs={}, batchSize={}, poolSize={}, queueCapacity={}, maxAttempts={}, handlerTimeoutMs={}",
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
            log.debug("Outbox worker poll skipped because worker is disabled");
            return;
        }

        log.debug(
                "Outbox worker poll started. workerId={}, batchSize={}, poolSize={}, queueCapacity={}",
                workerId,
                properties.getBatchSize(),
                properties.getPoolSize(),
                properties.getQueueCapacity());

        drainCompletedTasks();
        cancelTimedOutTasks();

        int staleRecovered = claimService.requeueStaleProcessing(properties.getProcessingTimeoutMs());
        if (staleRecovered > 0) {
            staleRequeuedCounter.increment(staleRecovered);
            log.warn("Recovered stale PROCESSING outbox events. count={}", staleRecovered);
        } else {
            log.debug("No stale PROCESSING outbox events found");
        }

        int capacity = availableInFlightCapacity();
        if (capacity <= 0) {
            pollSkippedCounter.increment();
            log.debug("Outbox worker poll skipped due to no in-flight capacity. workerId={}", workerId);
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
            submitEvent(event);
        }
    }

    private void submitEvent(ClaimedOutboxEvent event) {
        inFlightSlots.acquireUninterruptibly();
        try {
            log.debug(
                    "Submitting outbox event to executor. id={}, eventId={}, eventType={}, attemptCount={}",
                    event.id(),
                    event.eventId(),
                    event.eventType(),
                    event.attemptCount());
            Future<EventExecutionOutcome> future = completionService.submit(() -> executeEvent(event));
            runningTasks.put(future, new RunningTask(event, timeProvider.now()));
        } catch (RuntimeException ex) {
            inFlightSlots.release();
            executorRejectedCounter.increment();
            log.error(
                    "Failed to submit outbox event to executor. id={}, eventId={}, eventType={}",
                    event.id(),
                    event.eventId(),
                    event.eventType(),
                    ex);
            processor.markFailed(event, ex);
        }
    }

    private EventExecutionOutcome executeEvent(ClaimedOutboxEvent event) {
        try {
            processor.dispatch(event);
            return EventExecutionOutcome.success();
        } catch (RuntimeException ex) {
            return EventExecutionOutcome.failure(ex);
        }
    }

    private void drainCompletedTasks() {
        Future<EventExecutionOutcome> completed;
        while ((completed = completionService.poll()) != null) {
            RunningTask task = runningTasks.remove(completed);
            if (task == null) {
                log.debug("Completed outbox task not found in running map");
                continue;
            }
            try {
                handleCompletedTask(completed, task);
            } finally {
                inFlightSlots.release();
            }
        }
    }

    private void handleCompletedTask(Future<EventExecutionOutcome> completed, RunningTask task) {
        ClaimedOutboxEvent event = task.event();
        if (completed.isCancelled()) {
            Throwable cancellationReason = task.timeoutCancellationRequested().get()
                    ? new IllegalStateException(
                            "Event handler timed out after %d ms".formatted(properties.getHandlerTimeoutMs()))
                    : new IllegalStateException("Event handler task was cancelled");
            processor.markFailed(event, cancellationReason);
            log.warn(
                    "Outbox event task cancelled. id={}, eventId={}, eventType={}, timeoutCancellation={}",
                    event.id(),
                    event.eventId(),
                    event.eventType(),
                    task.timeoutCancellationRequested().get());
            return;
        }

        try {
            EventExecutionOutcome outcome = completed.get();
            if (outcome.failure() == null) {
                processor.markDone(event);
                return;
            }
            processor.markFailed(event, outcome.failure());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            processor.markFailed(event, ex);
            log.warn(
                    "Interrupted while draining completed outbox task. id={}, eventId={}, eventType={}",
                    event.id(),
                    event.eventId(),
                    event.eventType(),
                    ex);
        } catch (ExecutionException ex) {
            Throwable rootCause = ex.getCause() == null ? ex : ex.getCause();
            processor.markFailed(event, rootCause);
        }
    }

    private void cancelTimedOutTasks() {
        long timeoutMs = properties.getHandlerTimeoutMs();
        if (timeoutMs <= 0) {
            return;
        }

        Instant now = timeProvider.now();
        for (Map.Entry<Future<EventExecutionOutcome>, RunningTask> entry : runningTasks.entrySet()) {
            Future<EventExecutionOutcome> future = entry.getKey();
            RunningTask task = entry.getValue();
            long runningMs = Duration.between(task.startedAt(), now).toMillis();
            if (runningMs <= timeoutMs) {
                continue;
            }
            if (!task.timeoutCancellationRequested().compareAndSet(false, true)) {
                continue;
            }
            boolean cancelled = future.cancel(true);
            if (cancelled) {
                timeoutCancelledCounter.increment();
                log.warn(
                        "Cancelled timed out outbox event task. id={}, eventId={}, eventType={}, runningMs={}, timeoutMs={}",
                        task.event().id(),
                        task.event().eventId(),
                        task.event().eventType(),
                        runningMs,
                        timeoutMs);
            } else {
                log.debug(
                        "Timed out outbox task could not be cancelled because it already completed. id={}, eventId={}, eventType={}",
                        task.event().id(),
                        task.event().eventId(),
                        task.event().eventType());
            }
        }
    }

    private int availableInFlightCapacity() {
        return inFlightSlots.availablePermits();
    }

    private record EventExecutionOutcome(Throwable failure) {
        private static EventExecutionOutcome success() {
            return new EventExecutionOutcome(null);
        }

        private static EventExecutionOutcome failure(Throwable throwable) {
            return new EventExecutionOutcome(throwable);
        }
    }

    private record RunningTask(
            ClaimedOutboxEvent event, Instant startedAt, AtomicBoolean timeoutCancellationRequested) {
        private RunningTask(ClaimedOutboxEvent event, Instant startedAt) {
            this(event, startedAt, new AtomicBoolean(false));
        }
    }
}
