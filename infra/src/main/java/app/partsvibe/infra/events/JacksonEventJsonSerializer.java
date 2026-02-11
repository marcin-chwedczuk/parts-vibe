package app.partsvibe.infra.events;

import app.partsvibe.shared.events.Event;
import app.partsvibe.shared.events.EventJsonSerializer;
import app.partsvibe.shared.events.EventPublisherException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JacksonEventJsonSerializer implements EventJsonSerializer {
    private static final Logger log = LoggerFactory.getLogger(JacksonEventJsonSerializer.class);

    private final ObjectMapper objectMapper;

    public JacksonEventJsonSerializer() {
        this.objectMapper = JsonMapper.builder()
                .findAndAddModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
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
}
