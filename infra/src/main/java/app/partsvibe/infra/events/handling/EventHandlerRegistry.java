package app.partsvibe.infra.events.handling;

import java.util.List;

public interface EventHandlerRegistry {
    List<ResolvedEventHandlerDescriptor> handlersFor(String eventName, int schemaVersion);
}
