package app.partsvibe.storage.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StorageUploadRequest(
        @NotNull StorageObjectType objectType,
        @NotBlank @Size(max = 256) String originalFilename,
        @NotNull @Size(min = 1) byte[] content) {}
