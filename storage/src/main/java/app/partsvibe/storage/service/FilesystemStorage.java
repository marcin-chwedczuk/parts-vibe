package app.partsvibe.storage.service;

import app.partsvibe.storage.api.StorageException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class FilesystemStorage {
    private final StoragePathResolver pathResolver;

    public FilesystemStorage(StoragePathResolver pathResolver) {
        this.pathResolver = pathResolver;
    }

    public Path writeBlob(UUID fileId, byte[] bytes) {
        return writeBytes(pathResolver.blobPath(fileId), bytes);
    }

    public Path writeThumbnail128(UUID fileId, byte[] bytes) {
        return writeBytes(pathResolver.thumbnail128Path(fileId), bytes);
    }

    public Path writeThumbnail512(UUID fileId, byte[] bytes) {
        return writeBytes(pathResolver.thumbnail512Path(fileId), bytes);
    }

    public InputStream openForRead(Path path) {
        try {
            return Files.newInputStream(path);
        } catch (IOException ex) {
            throw new StorageException("Failed to open stored file stream. path=" + path, ex);
        }
    }

    public boolean exists(Path path) {
        return Files.exists(path);
    }

    public void deleteFileDirectory(UUID fileId) {
        Path directory = pathResolver.fileDirectory(fileId);
        if (!Files.exists(directory)) {
            return;
        }

        try (var walk = Files.walk(directory)) {
            walk.sorted(Comparator.reverseOrder()).forEach(this::deleteSinglePath);
        } catch (IOException ex) {
            throw new StorageException("Failed to delete stored file directory. fileId=" + fileId, ex);
        }
    }

    private Path writeBytes(Path targetPath, byte[] bytes) {
        try {
            Path directory = targetPath.getParent();
            Files.createDirectories(directory);
            Path tmpFile = Files.createTempFile(directory, "upload-", ".tmp");
            try {
                Files.write(tmpFile, bytes);
                moveReplacing(tmpFile, targetPath);
            } finally {
                Files.deleteIfExists(tmpFile);
            }
            return targetPath;
        } catch (IOException ex) {
            throw new StorageException("Failed to write stored file. path=" + targetPath, ex);
        }
    }

    private void deleteSinglePath(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            throw new StorageException("Failed to delete stored file path. path=" + path, ex);
        }
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
