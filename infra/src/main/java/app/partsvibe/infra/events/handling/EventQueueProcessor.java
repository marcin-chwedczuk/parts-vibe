package app.partsvibe.infra.events.handling;

import app.partsvibe.infra.events.jpa.ClaimedEventQueueEntry;
import app.partsvibe.infra.events.jpa.EventQueueRepository;
import app.partsvibe.shared.time.TimeProvider;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventQueueProcessor {
    private static final Logger log = LoggerFactory.getLogger(EventQueueProcessor.class);

    private final EventDispatcher eventDispatcher;
    private final EventQueueRepository eventQueueRepository;
    private final EventQueueWorkerProperties properties;
    private final TimeProvider timeProvider;
    private final Counter processedCounter;
    private final Counter doneCounter;
    private final Counter failedCounter;
    private final Counter retryScheduledCounter;

    public EventQueueProcessor(
            EventDispatcher eventDispatcher,
            EventQueueRepository eventQueueRepository,
            EventQueueWorkerProperties properties,
            TimeProvider timeProvider,
            MeterRegistry meterRegistry) {
        this.eventDispatcher = eventDispatcher;
        this.eventQueueRepository = eventQueueRepository;
        this.properties = properties;
        this.timeProvider = timeProvider;
        this.processedCounter = meterRegistry.counter("app.events.worker.processed");
        this.doneCounter = meterRegistry.counter("app.events.worker.done");
        this.failedCounter = meterRegistry.counter("app.events.worker.failed");
        this.retryScheduledCounter = meterRegistry.counter("app.events.worker.retry.scheduled");
    }

    public void dispatch(ClaimedEventQueueEntry event) {
        log.debug(
                "Dispatching claimed event queue entry started. id={}, eventId={}, eventType={}, schemaVersion={}, attemptCount={}",
                event.id(),
                event.eventId(),
                event.eventType(),
                event.schemaVersion(),
                event.attemptCount());
        eventDispatcher.dispatch(event.eventType(), event.schemaVersion(), event.payload());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDone(ClaimedEventQueueEntry event) {
        processedCounter.increment();
        eventQueueRepository.markDone(event.id(), timeProvider.now());
        doneCounter.increment();
        log.info(
                "Event queue entry processed successfully. id={}, eventId={}, eventType={}, schemaVersion={}, attemptCount={}",
                event.id(),
                event.eventId(),
                event.eventType(),
                event.schemaVersion(),
                event.attemptCount());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(ClaimedEventQueueEntry event, Throwable errorCause) {
        processedCounter.increment();
        Instant nextAttemptAt = timeProvider.now().plusMillis(computeBackoffMs(event.attemptCount()));
        String error = truncatedError(errorCause);
        eventQueueRepository.markFailed(event.id(), nextAttemptAt, error, timeProvider.now());
        failedCounter.increment();
        if (event.attemptCount() < properties.getMaxAttempts()) {
            retryScheduledCounter.increment();
            log.debug(
                    "Scheduled event queue retry. id={}, eventId={}, eventType={}, schemaVersion={}, attemptCount={}, nextAttemptAt={}",
                    event.id(),
                    event.eventId(),
                    event.eventType(),
                    event.schemaVersion(),
                    event.attemptCount(),
                    nextAttemptAt);
        } else {
            log.warn(
                    "Event queue entry reached max attempts and will remain FAILED. id={}, eventId={}, eventType={}, schemaVersion={}, attemptCount={}",
                    event.id(),
                    event.eventId(),
                    event.eventType(),
                    event.schemaVersion(),
                    event.attemptCount());
        }
        log.error(
                "Event queue processing failed. id={}, eventId={}, eventType={}, schemaVersion={}, attemptCount={}, nextAttemptAt={}, errorSummary={}",
                event.id(),
                event.eventId(),
                event.eventType(),
                event.schemaVersion(),
                event.attemptCount(),
                nextAttemptAt,
                error,
                errorCause);
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
