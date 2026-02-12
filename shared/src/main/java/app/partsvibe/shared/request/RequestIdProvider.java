package app.partsvibe.shared.request;

import java.util.Optional;

public interface RequestIdProvider {
    Optional<String> current();

    default String currentOrElse(String fallback) {
        return current().orElse(fallback);
    }

    void set(String requestId);

    void clear();

    default Scope withRequestId(String requestId) {
        String previousRequestId = current().orElse(null);
        set(requestId);
        return () -> {
            if (previousRequestId == null) {
                clear();
            } else {
                set(previousRequestId);
            }
        };
    }

    @FunctionalInterface
    interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
