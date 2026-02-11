package app.partsvibe.shared.events.model;

public record EventMetadata(String eventType, int schemaVersion) {
    public static EventMetadata fromEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event must not be null.");
        }
        return fromEventClass(event.getClass());
    }

    public static EventMetadata fromEventClass(Class<? extends Event> eventClass) {
        EventTypeName annotation = eventClass.getAnnotation(EventTypeName.class);
        if (annotation == null || annotation.value().isBlank()) {
            throw new IllegalStateException(
                    "Event class must be annotated with @EventTypeName and non-blank value: " + eventClass.getName());
        }
        if (annotation.schemaVersion() <= 0) {
            throw new IllegalStateException(
                    "Event schemaVersion must be greater than 0. eventClass=%s, schemaVersion=%d"
                            .formatted(eventClass.getName(), annotation.schemaVersion()));
        }
        return new EventMetadata(annotation.value(), annotation.schemaVersion());
    }
}
