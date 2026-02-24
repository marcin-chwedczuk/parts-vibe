package app.partsvibe.storage.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.partsvibe.storage.api.StorageFileSizeLimitExceededException;
import app.partsvibe.storage.api.StorageObjectType;
import app.partsvibe.storage.api.StorageValidationException;
import app.partsvibe.storage.config.StorageProperties;
import org.junit.jupiter.api.Test;

class StorageRulesTest {
    private final StorageProperties properties = new StorageProperties();
    private final StorageRules rules = new StorageRules(properties);

    @Test
    void validateUploadAcceptsImageAtSizeLimitAndValidExtension() {
        long limit = properties.getLimits().getAvatarBytes();

        assertThatCode(() -> rules.validateUpload(StorageObjectType.USER_AVATAR_IMAGE, "avatar.jpeg", limit))
                .doesNotThrowAnyException();
    }

    @Test
    void validateUploadRejectsImageSizeAboveLimit() {
        long limit = properties.getLimits().getAvatarBytes();

        assertThatThrownBy(() -> rules.validateUpload(StorageObjectType.USER_AVATAR_IMAGE, "avatar.png", limit + 1))
                .isInstanceOf(StorageFileSizeLimitExceededException.class);
    }

    @Test
    void validateUploadRejectsImageWithInvalidExtension() {
        assertThatThrownBy(() -> rules.validateUpload(StorageObjectType.USER_AVATAR_IMAGE, "avatar.gif", 10))
                .isInstanceOf(StorageValidationException.class)
                .hasMessageContaining("Image extension is not allowed");
    }

    @Test
    void validateUploadRejectsImageWithoutExtension() {
        assertThatThrownBy(() -> rules.validateUpload(StorageObjectType.USER_AVATAR_IMAGE, "avatar", 10))
                .isInstanceOf(StorageValidationException.class)
                .hasMessageContaining("must include an extension");
    }

    @Test
    void validateDetectedMimeTypeAcceptsImagePngAndJpegCaseInsensitive() {
        assertThatCode(() -> rules.validateDetectedMimeType(StorageObjectType.USER_AVATAR_IMAGE, "image/png"))
                .doesNotThrowAnyException();
        assertThatCode(() -> rules.validateDetectedMimeType(StorageObjectType.USER_AVATAR_IMAGE, "IMAGE/JPEG"))
                .doesNotThrowAnyException();
    }

    @Test
    void validateDetectedMimeTypeRejectsUnsupportedImageMimeType() {
        assertThatThrownBy(() -> rules.validateDetectedMimeType(StorageObjectType.USER_AVATAR_IMAGE, "image/gif"))
                .isInstanceOf(StorageValidationException.class)
                .hasMessageContaining("Image MIME type is not allowed");
    }

    @Test
    void validateDetectedMimeTypeDoesNotRestrictAttachmentMimeType() {
        assertThatCode(() ->
                        rules.validateDetectedMimeType(StorageObjectType.PART_ATTACHMENT, "application/x-msdownload"))
                .doesNotThrowAnyException();
    }
}
