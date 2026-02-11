package app.partsvibe.config;

import jakarta.servlet.http.HttpServletRequest;
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
}
