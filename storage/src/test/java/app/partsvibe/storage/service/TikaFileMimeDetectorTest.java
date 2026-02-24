package app.partsvibe.storage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.partsvibe.storage.config.StorageProperties;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class TikaFileMimeDetectorTest {
    @Test
    void detectUsesBoundedBytesAndReturnsLowerCaseMimeType() throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.getMimeDetection().setMaxSniffBytes(512);
        TikaFileMimeDetector detector = new TikaFileMimeDetector(properties);

        byte[] pngBytes = toPngBytes(8, 8);
        String mimeType = detector.detect(pngBytes, "sample.PNG");

        assertEquals("image/png", mimeType);
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
