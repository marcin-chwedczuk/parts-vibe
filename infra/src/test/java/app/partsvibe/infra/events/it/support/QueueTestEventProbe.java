package app.partsvibe.infra.events.it.support;

import app.partsvibe.shared.events.model.EventMetadata;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class QueueTestEventProbe {
    private final Map<String, AtomicInteger> attemptsByKey = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> completionsByKey = new ConcurrentHashMap<>();
    private final Map<String, EventMetadata> lastMetadataByKey = new ConcurrentHashMap<>();
    private final Map<String, String> lastRequestIdInScopeByKey = new ConcurrentHashMap<>();
    private final AtomicInteger activeHandlers = new AtomicInteger(0);
    private final AtomicInteger maxParallelism = new AtomicInteger(0);

    public void reset() {
        attemptsByKey.clear();
        completionsByKey.clear();
        lastMetadataByKey.clear();
        lastRequestIdInScopeByKey.clear();
        activeHandlers.set(0);
        maxParallelism.set(0);
    }

    public int incrementAttempts(String key) {
        return attemptsByKey
                .computeIfAbsent(key, ignored -> new AtomicInteger(0))
                .incrementAndGet();
    }

    public void markCompleted(String key, EventMetadata metadata, String requestIdInScope) {
        completionsByKey.computeIfAbsent(key, ignored -> new AtomicInteger(0)).incrementAndGet();
        lastMetadataByKey.put(key, metadata);
        lastRequestIdInScopeByKey.put(key, requestIdInScope);
    }

    public int incrementActiveHandlers() {
        int current = activeHandlers.incrementAndGet();
        maxParallelism.getAndUpdate(previous -> Math.max(previous, current));
        return current;
    }

    public void decrementActiveHandlers() {
        activeHandlers.decrementAndGet();
    }

    public int attempts(String key) {
        return attemptsByKey.getOrDefault(key, new AtomicInteger(0)).get();
    }

    public int completions(String key) {
        return completionsByKey.getOrDefault(key, new AtomicInteger(0)).get();
    }

    public int totalCompletions() {
        return completionsByKey.values().stream().mapToInt(AtomicInteger::get).sum();
    }

    public int maxParallelism() {
        return maxParallelism.get();
    }

    public EventMetadata lastMetadata(String key) {
        return lastMetadataByKey.get(key);
    }

    public String lastRequestIdInScope(String key) {
        return lastRequestIdInScopeByKey.get(key);
    }
}
