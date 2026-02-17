package app.partsvibe.config;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class RequestContextAdvice {
    private final BuildVersionInfo buildVersionInfo;

    public RequestContextAdvice(BuildVersionInfo buildVersionInfo) {
        this.buildVersionInfo = buildVersionInfo;
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
        return currentAuthenticatedUser().isPresent();
    }

    @ModelAttribute("isAdminUser")
    public boolean isAdminUser() {
        return currentAuthenticatedUser().stream()
                .flatMap(authentication -> authentication.getAuthorities().stream())
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private Optional<Authentication> currentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return Optional.of(authentication);
    }
}
