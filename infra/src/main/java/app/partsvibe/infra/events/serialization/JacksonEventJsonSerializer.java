package app.partsvibe.infra.events.serialization;

import app.partsvibe.infra.events.handling.EventDispatchException;
import app.partsvibe.shared.events.model.Event;
import app.partsvibe.shared.events.model.EventMetadata;
import app.partsvibe.shared.events.publishing.EventPublisherException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JacksonEventJsonSerializer implements EventJsonSerializer {
    private static final Logger log = LoggerFactory.getLogger(JacksonEventJsonSerializer.class);

    private final ObjectMapper objectMapper;

    public JacksonEventJsonSerializer() {
        this.objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // TODO: Do we really need this? Seems fishy?
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    @Override
    public String serialize(Event event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            String eventType = "unknown";
            int schemaVersion = -1;
            try {
                EventMetadata metadata = EventMetadata.fromEvent(event);
                eventType = metadata.eventName();
                schemaVersion = metadata.schemaVersion();
            } catch (RuntimeException ignored) {
                // Keep fallback metadata values when annotation lookup fails.
            }
            log.error(
                    "Event JSON serialization failed. eventId={}, eventName={}, schemaVersion={}, requestId={}",
                    event.eventId(),
                    eventType,
                    schemaVersion,
                    event.requestId(),
                    e);
            throw new EventPublisherException(
                    "Failed to serialize event payload. eventId=%s, eventName=%s, schemaVersion=%d"
                            .formatted(event.eventId(), eventType, schemaVersion),
                    e);
        }
    }

    @Override
    public <E extends Event> E deserialize(String payloadJson, Class<E> eventClass) {
        try {
            return objectMapper.readValue(payloadJson, eventClass);
        } catch (JsonProcessingException e) {
            throw new EventDispatchException(
                    "Failed to deserialize event payload. eventClass=%s".formatted(eventClass.getName()), e);
        }
    }
}
