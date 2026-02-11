package app.partsvibe.config;

import app.partsvibe.shared.request.RequestIdProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class HttpRequestIdProvider implements RequestIdProvider {
    private static final String FALLBACK_REQUEST_ID = "unknown";

    @Override
    public String requestId() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            HttpServletRequest request = servletRequestAttributes.getRequest();
            Object attribute = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
            if (attribute instanceof String requestId && !requestId.isBlank()) {
                return requestId;
            }
        }
        return FALLBACK_REQUEST_ID;
    }
}
