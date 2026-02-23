package app.partsvibe.infra.events.it.support;

import app.partsvibe.shared.events.model.EventMetadata;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class QueueTestEventProbe {
    private static final String DEFAULT_HANDLER_MARKER = "primary";

    private final Map<String, AtomicInteger> attemptsByKey = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> completionsByKey = new ConcurrentHashMap<>();
    private final Map<String, EventMetadata> lastMetadataByKey = new ConcurrentHashMap<>();
    private final Map<String, String> lastRequestIdInScopeByKey = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> completionsByHandlerAndKey = new ConcurrentHashMap<>();
    private final Map<String, EventMetadata> lastMetadataByHandlerAndKey = new ConcurrentHashMap<>();
    private final Map<String, String> lastRequestIdByHandlerAndKey = new ConcurrentHashMap<>();
    private final AtomicInteger activeHandlers = new AtomicInteger(0);
    private final AtomicInteger maxParallelism = new AtomicInteger(0);

    public void reset() {
        attemptsByKey.clear();
        completionsByKey.clear();
        lastMetadataByKey.clear();
        lastRequestIdInScopeByKey.clear();
        completionsByHandlerAndKey.clear();
        lastMetadataByHandlerAndKey.clear();
        lastRequestIdByHandlerAndKey.clear();
        activeHandlers.set(0);
        maxParallelism.set(0);
    }

    public int incrementAttempts(String key) {
        return attemptsByKey
                .computeIfAbsent(key, ignored -> new AtomicInteger(0))
                .incrementAndGet();
    }

    public void markCompleted(String key, EventMetadata metadata, String requestIdInScope) {
        markCompleted(DEFAULT_HANDLER_MARKER, key, metadata, requestIdInScope);
    }

    public void markCompleted(String handlerMarker, String key, EventMetadata metadata, String requestIdInScope) {
        completionsByKey.computeIfAbsent(key, ignored -> new AtomicInteger(0)).incrementAndGet();
        lastMetadataByKey.put(key, metadata);
        lastRequestIdInScopeByKey.put(key, requestIdInScope);
        String mapKey = handlerKey(handlerMarker, key);
        completionsByHandlerAndKey
                .computeIfAbsent(mapKey, ignored -> new AtomicInteger(0))
                .incrementAndGet();
        lastMetadataByHandlerAndKey.put(mapKey, metadata);
        lastRequestIdByHandlerAndKey.put(mapKey, requestIdInScope);
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

    public int completions(String handlerMarker, String key) {
        return completionsByHandlerAndKey
                .getOrDefault(handlerKey(handlerMarker, key), new AtomicInteger(0))
                .get();
    }

    public EventMetadata lastMetadata(String handlerMarker, String key) {
        return lastMetadataByHandlerAndKey.get(handlerKey(handlerMarker, key));
    }

    public String lastRequestIdInScope(String handlerMarker, String key) {
        return lastRequestIdByHandlerAndKey.get(handlerKey(handlerMarker, key));
    }

    private static String handlerKey(String handlerMarker, String key) {
        return handlerMarker + "|" + key;
    }
}
