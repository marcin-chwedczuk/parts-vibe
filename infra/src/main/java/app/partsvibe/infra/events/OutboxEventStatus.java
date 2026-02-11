package app.partsvibe.infra.events;

public enum OutboxEventStatus {
    NEW,
    PROCESSING,
    DONE,
    FAILED
}
