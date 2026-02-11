package app.partsvibe.infra.events;

import app.partsvibe.shared.events.Event;
import app.partsvibe.shared.events.EventJsonSerializer;
import app.partsvibe.shared.events.EventPublisher;
import app.partsvibe.shared.events.EventPublisherException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaEventPublisher implements EventPublisher {
    private static final Logger log = LoggerFactory.getLogger(JpaEventPublisher.class);
    private static final Pattern EVENT_TYPE_PATTERN = Pattern.compile("^[a-z0-9]+(?:_[a-z0-9]+)*$");

    private final OutboxEventRepository repository;
    private final EventJsonSerializer eventJsonSerializer;
    private final Counter publishAttemptsCounter;
    private final Counter publishSuccessCounter;
    private final Counter publishErrorsCounter;

    public JpaEventPublisher(
            OutboxEventRepository repository, EventJsonSerializer eventJsonSerializer, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.eventJsonSerializer = eventJsonSerializer;
        this.publishAttemptsCounter = meterRegistry.counter("app.events.publish.attempts");
        this.publishSuccessCounter = meterRegistry.counter("app.events.publish.success");
        this.publishErrorsCounter = meterRegistry.counter("app.events.publish.errors");
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(Event event) {
        publishAttemptsCounter.increment();
        try {
            validate(event);
            String payloadJson = eventJsonSerializer.serialize(event);
            OutboxEventEntity entity = OutboxEventEntity.newEvent(
                    event.eventId(),
                    event.eventType(),
                    event.schemaVersion(),
                    event.occurredAt(),
                    event.requestId(),
                    payloadJson);
            repository.save(entity);
            publishSuccessCounter.increment();
            log.info(
                    "Published event to outbox. eventId={}, eventType={}, schemaVersion={}, requestId={}",
                    event.eventId(),
                    event.eventType(),
                    event.schemaVersion(),
                    event.requestId());
        } catch (EventPublisherException e) {
            publishErrorsCounter.increment();
            throw e;
        } catch (RuntimeException e) {
            publishErrorsCounter.increment();
            String eventType = (event == null) ? "unknown" : String.valueOf(event.eventType());
            String eventId = (event == null) ? "unknown" : String.valueOf(event.eventId());
            throw new EventPublisherException(
                    "Failed to publish event. eventId=%s, eventType=%s".formatted(eventId, eventType), e);
        }
    }

    private static void validate(Event event) {
        if (event == null) {
            throw new EventPublisherException("Event must not be null.");
        }
        if (event.eventId() == null) {
            throw new EventPublisherException("Event eventId must not be null.");
        }
        if (event.occurredAt() == null) {
            throw new EventPublisherException("Event occurredAt must not be null.");
        }
        if (isBlank(event.requestId())) {
            throw new EventPublisherException("Event requestId must not be blank.");
        }
        if (isBlank(event.eventType())) {
            throw new EventPublisherException("Event eventType must not be blank.");
        }
        if (!EVENT_TYPE_PATTERN.matcher(event.eventType()).matches()) {
            throw new EventPublisherException(
                    "Event eventType must be snake_case. eventType=%s".formatted(event.eventType()));
        }
        if (event.schemaVersion() <= 0) {
            throw new EventPublisherException("Event schemaVersion must be greater than 0.");
        }
    }

    private static boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }
}
