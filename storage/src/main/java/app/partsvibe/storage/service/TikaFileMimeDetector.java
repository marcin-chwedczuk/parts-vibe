package app.partsvibe.storage.service;

import app.partsvibe.storage.api.StorageValidationException;
import app.partsvibe.storage.config.StorageProperties;
import java.util.Arrays;
import java.util.Locale;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

@Component
public class TikaFileMimeDetector implements FileMimeDetector {
    private final Tika tika = new Tika();
    private final StorageProperties properties;

    public TikaFileMimeDetector(StorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public String detect(byte[] bytes, String originalFilename) {
        // Feed Tika a bounded prefix to reduce parser attack surface and CPU/memory risk.
        byte[] sniffBytes = boundedSniffBytes(bytes);
        String mimeType = tika.detect(sniffBytes, originalFilename);
        if (mimeType == null || mimeType.isBlank()) {
            throw new StorageValidationException("Unable to detect file MIME type.");
        }
        return mimeType.toLowerCase(Locale.ROOT);
    }

    private byte[] boundedSniffBytes(byte[] bytes) {
        int maxSniffBytes = properties.getMimeDetection().getMaxSniffBytes();
        if (bytes.length <= maxSniffBytes) {
            return bytes;
        }
        // Content-type sniffing does not require the full payload.
        return Arrays.copyOf(bytes, maxSniffBytes);
    }
}
