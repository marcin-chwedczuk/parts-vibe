package app.partsvibe.storage.service;

import app.partsvibe.shared.error.ApplicationException;
import app.partsvibe.storage.errors.StorageValidationException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

@Component
public class ThumbnailImageService {
    public byte[] createThumbnail(byte[] sourceImage, int boxSize, String outputFormat) {
        BufferedImage input = readImage(sourceImage);
        int width = input.getWidth();
        int height = input.getHeight();

        double scale = Math.min((double) boxSize / width, (double) boxSize / height);
        int scaledWidth = Math.max(1, (int) Math.round(width * scale));
        int scaledHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage thumbnail = new BufferedImage(boxSize, boxSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = thumbnail.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int x = (boxSize - scaledWidth) / 2;
            int y = (boxSize - scaledHeight) / 2;
            graphics.drawImage(input, x, y, scaledWidth, scaledHeight, null);
        } finally {
            graphics.dispose();
        }

        return writeImage(thumbnail, outputFormat);
    }

    private BufferedImage readImage(byte[] sourceImage) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(sourceImage));
            if (image == null) {
                throw new StorageValidationException(
                        "Uploaded image format is not supported for thumbnail generation.");
            }
            return image;
        } catch (IOException ex) {
            throw new ApplicationException("Failed to decode image for thumbnail generation.", ex);
        }
    }

    private byte[] writeImage(BufferedImage image, String outputFormat) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            boolean encoded = ImageIO.write(image, outputFormat, output);
            if (!encoded) {
                throw new StorageValidationException(
                        "Image format is not supported for thumbnail encoding. format=" + outputFormat);
            }
            return output.toByteArray();
        } catch (IOException ex) {
            throw new ApplicationException("Failed to encode thumbnail image.", ex);
        }
    }
}
