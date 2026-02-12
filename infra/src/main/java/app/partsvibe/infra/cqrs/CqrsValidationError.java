package app.partsvibe.infra.cqrs;

public record CqrsValidationError(String field, String message) {}
