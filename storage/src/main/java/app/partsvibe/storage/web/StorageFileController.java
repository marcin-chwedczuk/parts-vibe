package app.partsvibe.storage.web;

import app.partsvibe.shared.cqrs.Mediator;
import app.partsvibe.storage.api.StorageFileVariant;
import app.partsvibe.storage.errors.StoredFileNotFoundException;
import app.partsvibe.storage.queries.ResolveFileQuery;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class StorageFileController {
    private final Mediator mediator;

    public StorageFileController(Mediator mediator) {
        this.mediator = mediator;
    }

    @GetMapping("/storage/files/{fileId}")
    public ResponseEntity<InputStreamResource> readOriginal(@PathVariable UUID fileId) {
        return read(fileId, StorageFileVariant.ORIGINAL);
    }

    @GetMapping("/storage/files/{fileId}/thumbnail/{size}")
    public ResponseEntity<InputStreamResource> readThumbnail(@PathVariable UUID fileId, @PathVariable int size) {
        StorageFileVariant variant =
                switch (size) {
                    case 128 -> StorageFileVariant.THUMBNAIL_128;
                    case 512 -> StorageFileVariant.THUMBNAIL_512;
                    default -> null;
                };
        if (variant == null) {
            return ResponseEntity.notFound().build();
        }
        return read(fileId, variant);
    }

    private ResponseEntity<InputStreamResource> read(UUID fileId, StorageFileVariant variant) {
        try {
            ResolveFileQuery.FileResource resource = mediator.executeQuery(new ResolveFileQuery(fileId, variant));
            InputStreamResource body = new InputStreamResource(java.nio.file.Files.newInputStream(resource.path()));
            return ResponseEntity.ok()
                    .contentLength(resource.sizeBytes())
                    .contentType(parseMediaType(resource.mimeType()))
                    .body(body);
        } catch (StoredFileNotFoundException | java.io.IOException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    private static MediaType parseMediaType(String mimeType) {
        try {
            return MediaType.parseMediaType(mimeType);
        } catch (Exception ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
