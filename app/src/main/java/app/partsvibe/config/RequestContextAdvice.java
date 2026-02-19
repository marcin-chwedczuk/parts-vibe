package app.partsvibe.config;

import app.partsvibe.shared.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class RequestContextAdvice {
    private final BuildVersionInfo buildVersionInfo;
    private final CurrentUserProvider currentUserProvider;

    public RequestContextAdvice(BuildVersionInfo buildVersionInfo, CurrentUserProvider currentUserProvider) {
        this.buildVersionInfo = buildVersionInfo;
        this.currentUserProvider = currentUserProvider;
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        if (request == null) {
            return "/";
        }
        String path = request.getServletPath();
        return (path == null || path.isBlank()) ? "/" : path;
    }

    @ModelAttribute("appVersion")
    public String appVersion() {
        return buildVersionInfo.gitVersionLabel();
    }

    @ModelAttribute("isAuthenticatedUser")
    public boolean isAuthenticatedUser() {
        return currentUserProvider.isAuthenticated();
    }

    @ModelAttribute("isAdminUser")
    public boolean isAdminUser() {
        return currentUserProvider.hasRole("ROLE_ADMIN");
    }
}
