package app.partsvibe.infra.events.handling;

import app.partsvibe.infra.events.jpa.ClaimedEventQueueEntry;
import app.partsvibe.infra.events.jpa.EventQueueRepository;
import app.partsvibe.infra.events.serialization.EventJsonSerializer;
import app.partsvibe.shared.events.handling.EventHandler;
import app.partsvibe.shared.events.model.Event;
import app.partsvibe.shared.time.TimeProvider;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ClassUtils;

@Component
public class EventQueueConsumer {
    private static final Logger log = LoggerFactory.getLogger(EventQueueConsumer.class);

    private final EventTypeRegistry eventTypeRegistry;
    private final EventJsonSerializer eventJsonSerializer;
    private final EventQueueRepository eventQueueRepository;
    private final ListableBeanFactory beanFactory;
    private final EventQueueDispatcherProperties properties;
    private final TimeProvider timeProvider;
    private final TransactionTemplate requiresNewTx;
    private final Counter dispatchAttemptsCounter;
    private final Counter dispatchSuccessCounter;
    private final Counter dispatchErrorsCounter;
    private final Counter processedCounter;
    private final Counter doneCounter;
    private final Counter failedCounter;
    private final Counter retryScheduledCounter;

    public EventQueueConsumer(
            EventTypeRegistry eventTypeRegistry,
            EventJsonSerializer eventJsonSerializer,
            EventQueueRepository eventQueueRepository,
            ListableBeanFactory beanFactory,
            EventQueueDispatcherProperties properties,
            TimeProvider timeProvider,
            PlatformTransactionManager transactionManager,
            MeterRegistry meterRegistry) {
        this.eventTypeRegistry = eventTypeRegistry;
        this.eventJsonSerializer = eventJsonSerializer;
        this.eventQueueRepository = eventQueueRepository;
        this.beanFactory = beanFactory;
        this.properties = properties;
        this.timeProvider = timeProvider;
        this.requiresNewTx = new TransactionTemplate(transactionManager);
        this.requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.dispatchAttemptsCounter = meterRegistry.counter("app.events.dispatch.attempts");
        this.dispatchSuccessCounter = meterRegistry.counter("app.events.dispatch.success");
        this.dispatchErrorsCounter = meterRegistry.counter("app.events.dispatch.errors");
        this.processedCounter = meterRegistry.counter("app.events.worker.processed");
        this.doneCounter = meterRegistry.counter("app.events.worker.done");
        this.failedCounter = meterRegistry.counter("app.events.worker.failed");
        this.retryScheduledCounter = meterRegistry.counter("app.events.worker.retry.scheduled");
    }

    public void consume(ClaimedEventQueueEntry entry) {
        dispatchAttemptsCounter.increment();
        try {
            dispatchToHandlers(entry);
            dispatchSuccessCounter.increment();
            markDone(entry);
        } catch (RuntimeException ex) {
            dispatchErrorsCounter.increment();
            markFailed(entry, ex);
            throw ex;
        } catch (Error ex) {
            dispatchErrorsCounter.increment();
            markFailed(entry, ex);
            throw ex;
        }
    }

    public void markTimedOutIfProcessing(ClaimedEventQueueEntry entry, long timeoutMs) {
        Throwable timeoutError = new IllegalStateException("Event handler timed out after %d ms".formatted(timeoutMs));
        markFailedIfProcessing(entry, timeoutError);
    }

    public void markFailedIfProcessing(ClaimedEventQueueEntry entry, Throwable errorCause) {
        markFailed(entry, errorCause);
    }

    private void dispatchToHandlers(ClaimedEventQueueEntry entry) {
        Class<? extends Event> eventClass = eventTypeRegistry.eventClassFor(entry.eventType(), entry.schemaVersion());
        Event event = eventJsonSerializer.deserialize(entry.payload(), eventClass);
        List<ResolvedHandler> handlers = resolveHandlers(eventClass);

        log.debug(
                "Dispatching event queue entry to handlers. entry={}, handlersCount={}",
                entry.toStringWithoutPayload(),
                handlers.size());

        for (ResolvedHandler resolvedHandler : handlers) {
            String handlerClassName = handlerClassName(resolvedHandler.handler());
            log.debug(
                    "Invoking event handler. entry={}, handlerBeanName={}, handlerClass={}",
                    entry.toStringWithoutPayload(),
                    resolvedHandler.beanName(),
                    handlerClassName);
            try {
                invokeHandler(resolvedHandler.handler(), event);
            } catch (RuntimeException ex) {
                String causeMessage = safeMessage(ex);
                log.error(
                        "Event handler execution failed. entry={}, handlerBeanName={}, handlerClass={}, errorType={}, errorMessage={}",
                        entry.toStringWithoutPayload(),
                        resolvedHandler.beanName(),
                        handlerClassName,
                        ex.getClass().getSimpleName(),
                        causeMessage,
                        ex);
                throw new EventDispatchException(
                        "Event handler failed. eventId=%s, eventName=%s, schemaVersion=%d, handlerBeanName=%s, handlerClass=%s, cause=%s: %s"
                                .formatted(
                                        entry.eventId(),
                                        entry.eventType(),
                                        entry.schemaVersion(),
                                        resolvedHandler.beanName(),
                                        handlerClassName,
                                        ex.getClass().getSimpleName(),
                                        causeMessage),
                        ex);
            }

            if (Thread.currentThread().isInterrupted()) {
                throw new EventDispatchException(
                        "Event handling thread interrupted. eventId=%s, eventName=%s, schemaVersion=%d"
                                .formatted(entry.eventId(), entry.eventType(), entry.schemaVersion()));
            }
        }
    }

    private void markDone(ClaimedEventQueueEntry entry) {
        requiresNewTx.executeWithoutResult(status -> {
            processedCounter.increment();
            int updated = eventQueueRepository.markDone(entry.id(), timeProvider.now());
            if (updated > 0) {
                doneCounter.increment();
                log.info("Event queue entry processed successfully. entry={}", entry.toStringWithoutPayload());
            } else {
                log.debug(
                        "Skipping DONE transition because event queue entry is no longer PROCESSING. entry={}",
                        entry.toStringWithoutPayload());
            }
        });
    }

    private void markFailed(ClaimedEventQueueEntry entry, Throwable errorCause) {
        requiresNewTx.executeWithoutResult(status -> {
            processedCounter.increment();
            Instant now = timeProvider.now();
            Instant nextAttemptAt = now.plusMillis(computeBackoffMs(entry.attemptCount()));
            String error = truncatedError(errorCause);
            int updated = eventQueueRepository.markFailed(entry.id(), nextAttemptAt, error, now);
            if (updated > 0) {
                failedCounter.increment();
                if (entry.attemptCount() < properties.getMaxAttempts()) {
                    retryScheduledCounter.increment();
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
        });
    }

    private long computeBackoffMs(int attemptCount) {
        double scaled = properties.getBackoffInitialMs()
                * Math.pow(properties.getBackoffMultiplier(), Math.max(0, attemptCount - 1));
        long backoff = Math.round(scaled);
        return Math.min(backoff, properties.getBackoffMaxMs());
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

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return "<no-message>";
        }
        return message;
    }

    private static String handlerClassName(EventHandler<? extends Event> handler) {
        return ClassUtils.getUserClass(handler.getClass()).getName();
    }

    @SuppressWarnings("unchecked")
    private List<ResolvedHandler> resolveHandlers(Class<? extends Event> eventClass) {
        ResolvableType handlerType = ResolvableType.forClassWithGenerics(EventHandler.class, eventClass);
        String[] beanNames = beanFactory.getBeanNamesForType(EventHandler.class, true, false);
        List<ResolvedHandler> handlers = new ArrayList<>();
        for (String beanName : beanNames) {
            if (!beanFactory.isTypeMatch(beanName, handlerType)) {
                continue;
            }
            EventHandler<? extends Event> handler = (EventHandler<? extends Event>) beanFactory.getBean(beanName);
            handlers.add(new ResolvedHandler(beanName, handler));
        }
        return handlers;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Event> void invokeHandler(EventHandler<? extends Event> rawHandler, Event event) {
        EventHandler<E> typedHandler = (EventHandler<E>) rawHandler;
        typedHandler.handle((E) event);
    }

    private record ResolvedHandler(String beanName, EventHandler<? extends Event> handler) {}
}
