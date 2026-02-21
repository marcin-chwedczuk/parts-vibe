package app.partsvibe.storage.commands;

import app.partsvibe.shared.cqrs.Command;
import app.partsvibe.storage.api.StorageObjectType;
import app.partsvibe.storage.api.StorageUploadResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UploadStoredFileCommand(
        @NotNull StorageObjectType objectType,
        @NotBlank @Size(max = 256) String originalFilename,
        @NotNull @Size(min = 1) byte[] content)
        implements Command<StorageUploadResult> {}
