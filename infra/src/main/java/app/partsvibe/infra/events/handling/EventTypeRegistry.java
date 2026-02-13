package app.partsvibe.infra.events.handling;

import java.util.List;

public interface EventTypeRegistry {
    List<ResolvedEventHandlerDescriptor> handlersFor(String eventType, int schemaVersion);
}
