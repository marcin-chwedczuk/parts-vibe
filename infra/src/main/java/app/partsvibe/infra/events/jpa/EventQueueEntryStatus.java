package app.partsvibe.infra.events.jpa;

public enum EventQueueEntryStatus {
    NEW,
    PROCESSING,
    DONE,
    FAILED
}
