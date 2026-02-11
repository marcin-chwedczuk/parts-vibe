package app.partsvibe.shared.time;

import java.time.Instant;

public interface TimeProvider {
    Instant now();
}
