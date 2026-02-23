package app.partsvibe.config;

import app.partsvibe.shared.cqrs.Mediator;
import app.partsvibe.shared.security.CurrentUserProvider;
import app.partsvibe.users.queries.auth.GetUserMenuQuery;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class RequestContextAdvice {
    private static final String PLACEHOLDER_IMAGE_URL = "/resources/images/placeholder.png";

    private final BuildVersionInfo buildVersionInfo;
    private final CurrentUserProvider currentUserProvider;
    private final Mediator mediator;

    public RequestContextAdvice(
            BuildVersionInfo buildVersionInfo, CurrentUserProvider currentUserProvider, Mediator mediator) {
        this.buildVersionInfo = buildVersionInfo;
        this.currentUserProvider = currentUserProvider;
        this.mediator = mediator;
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

    @ModelAttribute("currentUsername")
    public String currentUsername() {
        return currentUserProvider.currentUsername().orElse("");
    }

    @ModelAttribute("currentUserAvatarUrl")
    public String currentUserAvatarUrl() {
        return currentUserProvider.currentUserId().map(this::resolveAvatarUrl).orElse(PLACEHOLDER_IMAGE_URL);
    }

    private String resolveAvatarUrl(Long userId) {
        try {
            var menuData = mediator.executeQuery(new GetUserMenuQuery(userId));
            return menuData.avatarId() == null
                    ? PLACEHOLDER_IMAGE_URL
                    : "/storage/files/" + menuData.avatarId() + "/thumbnail/128";
        } catch (RuntimeException ex) {
            return PLACEHOLDER_IMAGE_URL;
        }
    }
}
