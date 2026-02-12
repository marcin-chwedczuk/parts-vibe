package app.partsvibe.config;

import app.partsvibe.shared.request.RequestIdProvider;
import java.util.Optional;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class HttpRequestIdProvider implements RequestIdProvider {
    private static final String MDC_KEY = "requestId";
    private final ThreadLocal<String> requestId = new ThreadLocal<>();

    @Override
    public Optional<String> current() {
        return Optional.ofNullable(requestId.get());
    }

    @Override
    public void set(String requestId) {
        this.requestId.set(requestId);
        MDC.put(MDC_KEY, requestId);
    }

    @Override
    public void clear() {
        requestId.remove();
        MDC.remove(MDC_KEY);
    }
}
