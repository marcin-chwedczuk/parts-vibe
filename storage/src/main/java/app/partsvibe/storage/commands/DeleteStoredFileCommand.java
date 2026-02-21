package app.partsvibe.storage.commands;

import app.partsvibe.shared.cqrs.Command;
import app.partsvibe.shared.cqrs.NoResult;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DeleteStoredFileCommand(@NotNull UUID fileId) implements Command<NoResult> {}
