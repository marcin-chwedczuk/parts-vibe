package app.partsvibe.storage.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("app.storage")
@Validated
@Data
public class StorageProperties {
    @NotBlank
    private String rootDir = "./.parts-vibe-storage";

    @Valid
    private final Limits limits = new Limits();

    @Data
    public static class Limits {
        @Min(1)
        private long avatarBytes = 102_400;

        @Min(1)
        private long categoryPartImageBytes = 307_200;

        @Min(1)
        private long galleryImageBytes = 4_194_304;

        @Min(1)
        private long attachmentBytes = 10_485_760;
    }
}
