package app.partsvibe.storage.queries;

import app.partsvibe.shared.cqrs.Query;
import app.partsvibe.storage.api.StorageFileVariant;
import jakarta.validation.constraints.NotNull;
import java.nio.file.Path;
import java.util.UUID;

public record ResolveFileQuery(@NotNull UUID fileId, @NotNull StorageFileVariant variant)
        implements Query<ResolveFileQuery.FileResource> {
    public record FileResource(Path path, String mimeType, long sizeBytes) {}
}
