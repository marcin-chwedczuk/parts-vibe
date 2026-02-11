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
        log.debug(
                "Processing claimed outbox event started. id={}, eventId={}, eventType={}, attemptCount={}",
                event.id(),
                event.eventId(),
                event.eventType(),
                event.attemptCount());
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
                log.debug(
                        "Scheduled outbox event retry. id={}, eventId={}, eventType={}, attemptCount={}, nextAttemptAt={}",
                        event.id(),
                        event.eventId(),
                        event.eventType(),
                        event.attemptCount(),
                        nextAttemptAt);
            } else {
                log.warn(
                        "Outbox event reached max attempts and will remain FAILED. id={}, eventId={}, eventType={}, attemptCount={}",
                        event.id(),
                        event.eventId(),
                        event.eventType(),
                        event.attemptCount());
            }
            log.error(
                    "Outbox event processing failed. id={}, eventId={}, eventType={}, attemptCount={}, nextAttemptAt={}, errorSummary={}",
                    event.id(),
                    event.eventId(),
                    event.eventType(),
                    event.attemptCount(),
                    nextAttemptAt,
                    error,
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
