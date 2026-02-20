package app.partsvibe.testsupport.web;

import app.partsvibe.shared.request.RequestIdProvider;
import java.util.Optional;

public class InMemoryRequestIdProvider implements RequestIdProvider {
    private final ThreadLocal<String> requestId = new ThreadLocal<>();

    @Override
    public Optional<String> current() {
        return Optional.ofNullable(requestId.get());
    }

    @Override
    public void set(String requestId) {
        this.requestId.set(requestId);
    }

    @Override
    public void clear() {
        requestId.remove();
    }
}
