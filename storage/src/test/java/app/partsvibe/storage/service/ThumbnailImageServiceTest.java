package app.partsvibe.storage.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.partsvibe.storage.api.StorageValidationException;
import app.partsvibe.storage.config.StorageProperties;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ThumbnailImageServiceTest {
    @Test
    void createThumbnailRejectsImagesExceedingConfiguredDimensions() throws Exception {
        StorageProperties properties = defaultProperties();
        properties.getImageProcessing().setMaxDimensionPx(5);
        ThumbnailImageService service = new ThumbnailImageService(properties);

        byte[] source = toPngBytes(10, 10);

        assertThrows(StorageValidationException.class, () -> service.createThumbnail(source, 128, "png"));
    }

    @Test
    void createThumbnailProducesImageForValidInput() throws Exception {
        StorageProperties properties = defaultProperties();
        ThumbnailImageService service = new ThumbnailImageService(properties);

        byte[] source = toPngBytes(40, 20);
        byte[] thumbnail = service.createThumbnail(source, 128, "png");

        BufferedImage decoded = ImageIO.read(new java.io.ByteArrayInputStream(thumbnail));
        assertArrayEquals(new int[] {128, 128}, new int[] {decoded.getWidth(), decoded.getHeight()});
    }

    @Test
    void createThumbnailRejectsInvalidTargetBoxSize() throws Exception {
        StorageProperties properties = defaultProperties();
        ThumbnailImageService service = new ThumbnailImageService(properties);
        byte[] source = toPngBytes(10, 10);

        assertThrows(StorageValidationException.class, () -> service.createThumbnail(source, 0, "png"));
    }

    @Test
    void createThumbnailRejectsImageExceedingPixelLimit() throws Exception {
        StorageProperties properties = defaultProperties();
        properties.getImageProcessing().setMaxPixels(90);
        ThumbnailImageService service = new ThumbnailImageService(properties);
        byte[] source = toPngBytes(10, 10);

        assertThrows(StorageValidationException.class, () -> service.createThumbnail(source, 128, "png"));
    }

    @Test
    void createThumbnailRejectsImageExceedingDecodedBytesLimit() throws Exception {
        StorageProperties properties = defaultProperties();
        properties.getImageProcessing().setMaxDecodedBytes(300);
        ThumbnailImageService service = new ThumbnailImageService(properties);
        byte[] source = toPngBytes(10, 10);

        assertThrows(StorageValidationException.class, () -> service.createThumbnail(source, 128, "png"));
    }

    @Test
    void createThumbnailRejectsImageExceedingAspectRatioLimit() throws Exception {
        StorageProperties properties = defaultProperties();
        properties.getImageProcessing().setMaxAspectRatio(3);
        ThumbnailImageService service = new ThumbnailImageService(properties);
        byte[] source = toPngBytes(40, 10);

        assertThrows(StorageValidationException.class, () -> service.createThumbnail(source, 128, "png"));
    }

    private static StorageProperties defaultProperties() {
        return new StorageProperties();
    }

    private static byte[] toPngBytes(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            boolean encoded = ImageIO.write(image, "png", output);
            if (!encoded) {
                throw new IllegalStateException("Failed to encode test PNG.");
            }
            return output.toByteArray();
        }
    }
}
