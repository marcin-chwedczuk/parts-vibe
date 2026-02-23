package app.partsvibe.storage.commands;

import app.partsvibe.shared.cqrs.Command;
import app.partsvibe.storage.api.DeleteFileResult;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DeleteStoredFileCommand(@NotNull UUID fileId) implements Command<DeleteFileResult> {}
