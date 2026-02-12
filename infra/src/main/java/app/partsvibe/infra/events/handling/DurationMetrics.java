package app.partsvibe.infra.events.handling;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public class DurationMetrics {
    private final DistributionSummary summary;
    private final AtomicLong lastMs;

    public DurationMetrics(MeterRegistry meterRegistry, String metricPrefix) {
        this.summary = DistributionSummary.builder(metricPrefix + ".ms")
                .baseUnit("milliseconds")
                .register(meterRegistry);
        this.lastMs = new AtomicLong(0);
        meterRegistry.gauge(metricPrefix + ".last.ms", lastMs, AtomicLong::get);
    }

    public void recordDurationBetween(Instant from, Instant to) {
        long valueMs = Math.max(0L, to.toEpochMilli() - from.toEpochMilli());
        recordDurationMs(valueMs);
    }

    public void recordDurationMs(long valueMs) {
        long nonNegative = Math.max(0L, valueMs);
        summary.record(nonNegative);
        lastMs.set(nonNegative);
    }
}
