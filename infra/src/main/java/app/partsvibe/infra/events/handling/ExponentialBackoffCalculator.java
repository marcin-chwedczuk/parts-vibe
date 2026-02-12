package app.partsvibe.infra.events.handling;

public final class ExponentialBackoffCalculator {
    private final long initialDelayMs;
    private final double multiplier;
    private final long maxDelayMs;

    public ExponentialBackoffCalculator(long initialDelayMs, double multiplier, long maxDelayMs) {
        this.initialDelayMs = initialDelayMs;
        this.multiplier = multiplier;
        this.maxDelayMs = maxDelayMs;
    }

    public long computeDelayMs(int attemptCount) {
        double scaled = initialDelayMs * Math.pow(multiplier, Math.max(0, attemptCount - 1));
        long backoffMs = Math.round(scaled);
        return Math.min(backoffMs, maxDelayMs);
    }
}
