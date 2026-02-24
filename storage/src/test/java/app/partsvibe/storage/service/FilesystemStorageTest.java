package app.partsvibe.storage.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.partsvibe.storage.api.StorageException;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FilesystemStorageTest {
    @Test
    void shouldWriteBlobAndDeleteWholeDirectory() throws IOException {
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            StoragePathResolver pathResolver = new StoragePathResolver(fs.getPath("/storage"));
            FilesystemStorage storage = new FilesystemStorage(pathResolver);
            UUID fileId = UUID.fromString("aabbc7d1-1111-2222-3333-444455556666");

            storage.writeBlob(fileId, "hello".getBytes());
            storage.writeThumbnail128(fileId, "mini".getBytes());

            byte[] blobBytes = Files.readAllBytes(pathResolver.blobPath(fileId));
            assertArrayEquals("hello".getBytes(), blobBytes);
            assertTrue(Files.exists(pathResolver.thumbnail128Path(fileId)));

            storage.deleteFileDirectory(fileId);
            assertFalse(Files.exists(pathResolver.fileDirectory(fileId)));
        }
    }

    @Test
    void deleteFileDirectoryShouldIgnoreMissingDirectory() throws IOException {
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            StoragePathResolver pathResolver = new StoragePathResolver(fs.getPath("/storage"));
            FilesystemStorage storage = new FilesystemStorage(pathResolver);

            assertDoesNotThrow(() -> storage.deleteFileDirectory(UUID.randomUUID()));
        }
    }

    @Test
    void openForReadShouldThrowStorageExceptionWhenPathDoesNotExist() throws IOException {
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            StoragePathResolver pathResolver = new StoragePathResolver(fs.getPath("/storage"));
            FilesystemStorage storage = new FilesystemStorage(pathResolver);

            assertThrows(StorageException.class, () -> storage.openForRead(pathResolver.blobPath(UUID.randomUUID())));
        }
    }
}
