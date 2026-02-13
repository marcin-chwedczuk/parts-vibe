package app.partsvibe.shared.events.model;

public record EventMetadata(String eventName, int schemaVersion) {
    public static EventMetadata fromEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event must not be null.");
        }
        return fromEventClass(event.getClass());
    }

    public static EventMetadata fromEventClass(Class<? extends Event> eventClass) {
        IntegrationEvent annotation = eventClass.getAnnotation(IntegrationEvent.class);
        if (annotation == null || annotation.name().isBlank()) {
            throw new IllegalStateException(
                    "Event class must be annotated with @PublishableEvent and non-blank name: " + eventClass.getName());
        }
        if (annotation.version() <= 0) {
            throw new IllegalStateException(
                    "Event schemaVersion must be greater than 0. eventClass=%s, schemaVersion=%d"
                            .formatted(eventClass.getName(), annotation.version()));
        }
        return new EventMetadata(annotation.name(), annotation.version());
    }
}
