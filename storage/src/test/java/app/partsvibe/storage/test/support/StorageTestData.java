package app.partsvibe.storage.test.support;

import app.partsvibe.storage.api.StorageObjectType;
import app.partsvibe.storage.domain.StoredFile;
import app.partsvibe.storage.domain.StoredFileKind;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.UUID;
import javax.imageio.ImageIO;

public final class StorageTestData {
    private StorageTestData() {}

    public static byte[] pngBytes(int width, int height) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            boolean encoded = ImageIO.write(image, "png", output);
            if (!encoded) {
                throw new IllegalStateException("Failed to encode PNG test image.");
            }
            return output.toByteArray();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Failed to create PNG test image bytes.", ex);
        }
    }

    public static StoredFile pendingImageFile(
            UUID fileId, StorageObjectType objectType, String fileName, long sizeBytes) {
        return new StoredFile(fileId, objectType, StoredFileKind.IMAGE, fileName, sizeBytes, Instant.now(), "it-user");
    }

    public static StoredFile pendingBlobFile(
            UUID fileId, StorageObjectType objectType, String fileName, long sizeBytes) {
        return new StoredFile(fileId, objectType, StoredFileKind.BLOB, fileName, sizeBytes, Instant.now(), "it-user");
    }
}
