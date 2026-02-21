package app.partsvibe.storage.service;

import app.partsvibe.storage.errors.StorageValidationException;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

@Component
public class TikaFileMimeDetector implements FileMimeDetector {
    private final Tika tika = new Tika();

    @Override
    public String detect(byte[] bytes, String originalFilename) {
        String mimeType = tika.detect(bytes, originalFilename);
        if (mimeType == null || mimeType.isBlank()) {
            throw new StorageValidationException("Unable to detect file MIME type.");
        }
        return mimeType;
    }
}
