package app.partsvibe.shared.antivirus;

import java.util.Optional;

public record ScanResult(Status status, Optional<String> message) {
    public enum Status {
        OK,
        MALWARE_FOUND
    }
}
