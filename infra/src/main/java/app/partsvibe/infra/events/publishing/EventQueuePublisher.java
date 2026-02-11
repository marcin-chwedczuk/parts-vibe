package app.partsvibe.infra.events.publishing;

import app.partsvibe.infra.events.jpa.EventQueueEntry;
import app.partsvibe.infra.events.jpa.EventQueueRepository;
import app.partsvibe.infra.events.serialization.EventJsonSerializer;
import app.partsvibe.shared.events.model.Event;
import app.partsvibe.shared.events.model.EventMetadata;
import app.partsvibe.shared.events.publishing.EventPublisher;
import app.partsvibe.shared.events.publishing.EventPublisherException;
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
public class EventQueuePublisher implements EventPublisher {
    private static final Logger log = LoggerFactory.getLogger(EventQueuePublisher.class);
    private static final Pattern EVENT_TYPE_PATTERN = Pattern.compile("^[a-z0-9]+(?:_[a-z0-9]+)*$");

    private final EventQueueRepository repository;
    private final EventJsonSerializer eventJsonSerializer;
    private final Counter publishAttemptsCounter;
    private final Counter publishSuccessCounter;
    private final Counter publishErrorsCounter;

    public EventQueuePublisher(
            EventQueueRepository repository, EventJsonSerializer eventJsonSerializer, MeterRegistry meterRegistry) {
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
            EventMetadata metadata = validate(event);
            String payloadJson = eventJsonSerializer.serialize(event);
            EventQueueEntry entity = EventQueueEntry.newEvent(
                    event.eventId(),
                    metadata.eventType(),
                    metadata.schemaVersion(),
                    event.occurredAt(),
                    event.requestId(),
                    payloadJson);
            repository.save(entity);
            publishSuccessCounter.increment();
            log.info(
                    "Published event to event queue. eventId={}, eventType={}, schemaVersion={}, requestId={}",
                    event.eventId(),
                    metadata.eventType(),
                    metadata.schemaVersion(),
                    event.requestId());
        } catch (EventPublisherException e) {
            publishErrorsCounter.increment();
            throw e;
        } catch (RuntimeException e) {
            publishErrorsCounter.increment();
            String eventType = "unknown";
            int schemaVersion = -1;
            if (event != null) {
                try {
                    EventMetadata metadata = EventMetadata.fromEvent(event);
                    eventType = metadata.eventType();
                    schemaVersion = metadata.schemaVersion();
                } catch (RuntimeException ignored) {
                    // Fallback to unknown values when metadata extraction fails.
                }
            }
            String eventId = (event == null) ? "unknown" : String.valueOf(event.eventId());
            throw new EventPublisherException(
                    "Failed to publish event. eventId=%s, eventType=%s, schemaVersion=%d"
                            .formatted(eventId, eventType, schemaVersion),
                    e);
        }
    }

    private static EventMetadata validate(Event event) {
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
        EventMetadata metadata = EventMetadata.fromEvent(event);
        if (isBlank(metadata.eventType())) {
            throw new EventPublisherException("Event eventType must not be blank.");
        }
        if (!EVENT_TYPE_PATTERN.matcher(metadata.eventType()).matches()) {
            throw new EventPublisherException(
                    "Event eventType must be snake_case. eventType=%s".formatted(metadata.eventType()));
        }
        if (metadata.schemaVersion() <= 0) {
            throw new EventPublisherException("Event schemaVersion must be greater than 0.");
        }
        return metadata;
    }

    private static boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }
}
