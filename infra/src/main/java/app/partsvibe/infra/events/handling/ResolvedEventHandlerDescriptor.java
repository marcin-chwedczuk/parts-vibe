package app.partsvibe.infra.events.handling;

import app.partsvibe.shared.events.model.Event;

public record ResolvedEventHandlerDescriptor(
        String eventName, int schemaVersion, String beanName, Class<? extends Event> payloadClass) {}
