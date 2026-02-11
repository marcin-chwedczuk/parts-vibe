package app.partsvibe.infra.events.serialization;

import app.partsvibe.shared.events.handling.EventDispatchException;
import app.partsvibe.shared.events.model.Event;
import app.partsvibe.shared.events.publishing.EventPublisherException;
import app.partsvibe.shared.events.serialization.EventJsonSerializer;
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
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    @Override
    public String serialize(Event event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error(
                    "Event JSON serialization failed. eventId={}, eventType={}, requestId={}",
                    event.eventId(),
                    event.eventType(),
                    event.requestId(),
                    e);
            throw new EventPublisherException(
                    "Failed to serialize event payload. eventId=%s, eventType=%s"
                            .formatted(event.eventId(), event.eventType()),
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
