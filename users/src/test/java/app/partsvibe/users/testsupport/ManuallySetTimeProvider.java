package app.partsvibe.users.testsupport;

import app.partsvibe.shared.time.TimeProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public class ManuallySetTimeProvider implements TimeProvider {
    public static final Instant DEFAULT_TIME = Instant.parse("2024-01-01T00:00:00Z");

    private final AtomicReference<Instant> now = new AtomicReference<>(DEFAULT_TIME);

    @Override
    public Instant now() {
        return now.get();
    }

    public void setNow(Instant now) {
        this.now.set(now);
    }

    public void moveMinutes(long minutes) {
        move(Duration.ofMinutes(minutes));
    }

    public void move(Duration delta) {
        now.updateAndGet(current -> current.plus(delta));
    }

    public void reset() {
        now.set(DEFAULT_TIME);
    }
}
