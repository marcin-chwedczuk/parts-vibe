package app.partsvibe.config;

import app.partsvibe.shared.request.RequestIdProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_ATTRIBUTE = "requestId";
    private static final int MAX_REQUEST_ID_LENGTH = 64;
    private static final Pattern ALLOWED_REQUEST_ID_PATTERN = Pattern.compile("^[A-Za-z0-9-]+$");
    private final RequestIdProvider requestIdProvider;

    public RequestIdFilter(RequestIdProvider requestIdProvider) {
        this.requestIdProvider = requestIdProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = resolveRequestId(request.getHeader(REQUEST_ID_HEADER));

        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try (var ignored = requestIdProvider.withRequestId(requestId)) {
            filterChain.doFilter(request, response);
        }
    }

    private String resolveRequestId(String requestIdHeader) {
        if (requestIdHeader == null) {
            return UUID.randomUUID().toString();
        }

        String requestId = requestIdHeader.trim();
        if (requestId.isBlank()
                || requestId.length() > MAX_REQUEST_ID_LENGTH
                || !ALLOWED_REQUEST_ID_PATTERN.matcher(requestId).matches()) {
            return UUID.randomUUID().toString();
        }

        return requestId;
    }
}
