package app.partsvibe.infra.events.jpa;

public enum OutboxEventStatus {
    NEW,
    PROCESSING,
    DONE,
    FAILED
}
