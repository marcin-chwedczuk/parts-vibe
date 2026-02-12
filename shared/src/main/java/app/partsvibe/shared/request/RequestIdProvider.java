package app.partsvibe.shared.request;

import java.util.Optional;

public interface RequestIdProvider {
    Optional<String> current();

    default String currentOrElse(String fallback) {
        return current().orElse(fallback);
    }

    void set(String requestId);

    void clear();
}
