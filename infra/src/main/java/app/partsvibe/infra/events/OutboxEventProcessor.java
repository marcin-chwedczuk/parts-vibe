package app.partsvibe.infra.events;

import app.partsvibe.shared.events.EventDispatcher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxEventProcessor {
    private static final Logger log = LoggerFactory.getLogger(OutboxEventProcessor.class);

    private final EventDispatcher eventDispatcher;
    private final OutboxEventRepository outboxEventRepository;
    private final EventWorkerProperties properties;
    private final Counter processedCounter;
    private final Counter doneCounter;
    private final Counter failedCounter;
    private final Counter retryScheduledCounter;

    public OutboxEventProcessor(
            EventDispatcher eventDispatcher,
            OutboxEventRepository outboxEventRepository,
            EventWorkerProperties properties,
            MeterRegistry meterRegistry) {
        this.eventDispatcher = eventDispatcher;
        this.outboxEventRepository = outboxEventRepository;
        this.properties = properties;
        this.processedCounter = meterRegistry.counter("app.events.worker.processed");
        this.doneCounter = meterRegistry.counter("app.events.worker.done");
        this.failedCounter = meterRegistry.counter("app.events.worker.failed");
        this.retryScheduledCounter = meterRegistry.counter("app.events.worker.retry.scheduled");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processClaimedEvent(ClaimedOutboxEvent event) {
        processedCounter.increment();
        try {
            eventDispatcher.dispatch(event.eventType(), event.payload());
            outboxEventRepository.markDone(event.id(), Instant.now());
            doneCounter.increment();
            log.info(
                    "Outbox event processed successfully. id={}, eventId={}, eventType={}, attemptCount={}",
                    event.id(),
                    event.eventId(),
                    event.eventType(),
                    event.attemptCount());
        } catch (RuntimeException ex) {
            Instant nextAttemptAt = Instant.now().plusMillis(computeBackoffMs(event.attemptCount()));
            String error = truncatedError(ex);
            outboxEventRepository.markFailed(event.id(), nextAttemptAt, error, Instant.now());
            failedCounter.increment();
            if (event.attemptCount() < properties.getMaxAttempts()) {
                retryScheduledCounter.increment();
            }
            log.error(
                    "Outbox event processing failed. id={}, eventId={}, eventType={}, attemptCount={}, nextAttemptAt={}",
                    event.id(),
                    event.eventId(),
                    event.eventType(),
                    event.attemptCount(),
                    nextAttemptAt,
                    ex);
        }
    }

    private long computeBackoffMs(int attemptCount) {
        double scaled = properties.getBackoffInitialMs()
                * Math.pow(properties.getBackoffMultiplier(), Math.max(0, attemptCount - 1));
        long backoff = Math.round(scaled);
        return Math.min(backoff, properties.getBackoffMaxMs());
    }

    private static String truncatedError(Throwable throwable) {
        String text = throwable.getMessage();
        if (text == null || text.isBlank()) {
            text = throwable.getClass().getSimpleName();
        }
        if (text.length() <= 2000) {
            return text;
        }
        return text.substring(0, 2000);
    }
}
