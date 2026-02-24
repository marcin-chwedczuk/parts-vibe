package app.partsvibe.storage.service;

import app.partsvibe.shared.error.ApplicationException;
import app.partsvibe.storage.api.StorageValidationException;
import app.partsvibe.storage.config.StorageProperties;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Component;

@Component
public class ThumbnailImageService {
    private final StorageProperties properties;

    public ThumbnailImageService(StorageProperties properties) {
        this.properties = properties;
        // Keep ImageIO in-memory to avoid writing attacker-controlled decode intermediates to disk.
        ImageIO.setUseCache(false);
    }

    public byte[] createThumbnail(byte[] sourceImage, int boxSize, String outputFormat) {
        if (boxSize <= 0) {
            throw new StorageValidationException("Thumbnail size must be greater than zero.");
        }

        ImageMetadata metadata = readImageMetadata(sourceImage);
        validateImageMetadata(metadata.width(), metadata.height());
        BufferedImage input = readImage(sourceImage, boxSize, metadata);
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

    private ImageMetadata readImageMetadata(byte[] sourceImage) {
        // Read only dimensions first. This avoids full raster allocation for decompression-bomb inputs.
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(sourceImage))) {
            ImageReader reader = firstReader(stream);
            try {
                reader.setInput(stream, true, true);
                return new ImageMetadata(reader.getWidth(0), reader.getHeight(0));
            } finally {
                reader.dispose();
            }
        } catch (IOException ex) {
            throw new ApplicationException("Failed to decode image for thumbnail generation.", ex);
        } catch (RuntimeException ex) {
            throw new StorageValidationException("Uploaded image cannot be parsed for thumbnail generation.");
        }
    }

    private BufferedImage readImage(byte[] sourceImage, int boxSize, ImageMetadata metadata) {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(sourceImage))) {
            ImageReader reader = firstReader(stream);
            try {
                reader.setInput(stream, true, true);
                ImageReadParam param = reader.getDefaultReadParam();
                int subsampling = calculateSubsampling(metadata.width(), metadata.height(), boxSize);
                if (subsampling > 1) {
                    // Decode a downsampled raster directly from source to reduce peak memory usage.
                    param.setSourceSubsampling(subsampling, subsampling, 0, 0);
                }
                BufferedImage image = reader.read(0, param);
                if (image == null) {
                    throw new StorageValidationException(
                            "Uploaded image format is not supported for thumbnail generation.");
                }
                return image;
            } finally {
                reader.dispose();
            }
        } catch (IOException ex) {
            throw new ApplicationException("Failed to decode image for thumbnail generation.", ex);
        } catch (RuntimeException ex) {
            throw new StorageValidationException("Uploaded image cannot be parsed for thumbnail generation.");
        }
    }

    private ImageReader firstReader(ImageInputStream stream) {
        if (stream == null) {
            throw new StorageValidationException("Uploaded image cannot be parsed for thumbnail generation.");
        }
        Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
        if (!readers.hasNext()) {
            throw new StorageValidationException("Uploaded image format is not supported for thumbnail generation.");
        }
        return readers.next();
    }

    private void validateImageMetadata(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new StorageValidationException("Uploaded image dimensions are invalid.");
        }

        var imageLimits = properties.getImageProcessing();

        if (width > imageLimits.getMaxDimensionPx() || height > imageLimits.getMaxDimensionPx()) {
            throw new StorageValidationException("Uploaded image dimensions exceed allowed limits.");
        }

        long pixels = (long) width * (long) height;
        if (pixels > imageLimits.getMaxPixels()) {
            throw new StorageValidationException("Uploaded image pixel count exceeds allowed limits.");
        }

        long decodedBytes = pixels * 4L;
        // RGBA worst-case estimate; protects memory even when compressed input is small.
        if (decodedBytes > imageLimits.getMaxDecodedBytes()) {
            throw new StorageValidationException("Uploaded image decoded size exceeds allowed limits.");
        }

        int longer = Math.max(width, height);
        int shorter = Math.min(width, height);
        if ((double) longer / (double) shorter > imageLimits.getMaxAspectRatio()) {
            throw new StorageValidationException("Uploaded image aspect ratio exceeds allowed limits.");
        }
    }

    private static int calculateSubsampling(int width, int height, int boxSize) {
        int longestSide = Math.max(width, height);
        int targetLongestSide = Math.max(1, boxSize * 2);
        // Keep decode roughly near output scale while preserving enough detail for quality downscale.
        return Math.max(1, longestSide / targetLongestSide);
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

    private record ImageMetadata(int width, int height) {}
}
