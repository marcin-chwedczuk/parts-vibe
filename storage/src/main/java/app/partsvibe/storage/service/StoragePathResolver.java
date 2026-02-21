package app.partsvibe.storage.service;

import app.partsvibe.storage.config.StorageProperties;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StoragePathResolver {
    public static final String BLOB_FILENAME = "blob";
    public static final String THUMBNAIL_128_FILENAME = "thumbnail-128";
    public static final String THUMBNAIL_512_FILENAME = "thumbnail-512";

    private final Path rootDirectory;

    @Autowired
    public StoragePathResolver(StorageProperties properties) {
        this(Path.of(properties.getRootDir()).toAbsolutePath().normalize());
    }

    StoragePathResolver(Path rootDirectory) {
        this.rootDirectory = rootDirectory.normalize();
    }

    public Path rootDirectory() {
        return rootDirectory;
    }

    public Path fileDirectory(UUID fileId) {
        String compact = fileId.toString().replace("-", "").toUpperCase(Locale.ROOT);
        return rootDirectory
                .resolve(compact.substring(0, 2))
                .resolve(compact.substring(2, 4))
                .resolve(compact.substring(4, 6))
                .resolve(compact);
    }

    public Path blobPath(UUID fileId) {
        return fileDirectory(fileId).resolve(BLOB_FILENAME);
    }

    public Path thumbnail128Path(UUID fileId) {
        return fileDirectory(fileId).resolve(THUMBNAIL_128_FILENAME);
    }

    public Path thumbnail512Path(UUID fileId) {
        return fileDirectory(fileId).resolve(THUMBNAIL_512_FILENAME);
    }
}
