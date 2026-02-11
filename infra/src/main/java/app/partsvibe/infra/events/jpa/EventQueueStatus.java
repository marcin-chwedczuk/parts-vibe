package app.partsvibe.infra.events.jpa;

public enum EventQueueStatus {
    NEW,
    PROCESSING,
    DONE,
    FAILED
}
