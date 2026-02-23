package app.partsvibe.storage.service;

import app.partsvibe.storage.api.StorageFileSizeLimitExceededException;
import app.partsvibe.storage.api.StorageObjectType;
import app.partsvibe.storage.api.StorageValidationException;
import app.partsvibe.storage.config.StorageProperties;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class StorageRules {
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg");
    private static final Set<String> IMAGE_MIME_TYPES = Set.of("image/png", "image/jpeg");

    private final StorageProperties properties;

    public StorageRules(StorageProperties properties) {
        this.properties = properties;
    }

    public void validateUpload(StorageObjectType objectType, String originalFilename, long sizeBytes) {
        validateSize(objectType, sizeBytes);
        if (objectType.isImage()) {
            validateImageExtension(originalFilename);
        }
    }

    public void validateDetectedMimeType(StorageObjectType objectType, String mimeType) {
        if (objectType.isImage() && !IMAGE_MIME_TYPES.contains(mimeType.toLowerCase(Locale.ROOT))) {
            throw new StorageValidationException("Image MIME type is not allowed. mimeType=" + mimeType);
        }
    }

    private void validateSize(StorageObjectType objectType, long sizeBytes) {
        long maxSize = maxAllowedSize(objectType);
        if (sizeBytes > maxSize) {
            throw new StorageFileSizeLimitExceededException(objectType, maxSize, sizeBytes);
        }
    }

    private long maxAllowedSize(StorageObjectType objectType) {
        return switch (objectType) {
            case USER_AVATAR_IMAGE -> properties.getLimits().getAvatarBytes();
            case CATEGORY_IMAGE, PART_IMAGE -> properties.getLimits().getCategoryPartImageBytes();
            case PART_GALLERY_IMAGE -> properties.getLimits().getGalleryImageBytes();
            case PART_ATTACHMENT -> properties.getLimits().getAttachmentBytes();
        };
    }

    private void validateImageExtension(String originalFilename) {
        String extension = fileExtensionOf(originalFilename);
        if (!IMAGE_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new StorageValidationException(
                    "Image extension is not allowed. allowed=png,jpg,jpeg, fileName=" + originalFilename);
        }
    }

    private static String fileExtensionOf(String originalFilename) {
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot <= 0 || lastDot == originalFilename.length() - 1) {
            throw new StorageValidationException(
                    "Uploaded file must include an extension. fileName=" + originalFilename);
        }
        return originalFilename.substring(lastDot + 1);
    }
}
