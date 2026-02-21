package app.partsvibe.users.web;

import app.partsvibe.uicomponents.breadcrumbs.BreadcrumbItemData;
import app.partsvibe.uicomponents.breadcrumbs.BreadcrumbsData;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "app.partsvibe.users.web")
public class AdminBreadcrumbsAdvice {
    private final MessageSource messageSource;

    public AdminBreadcrumbsAdvice(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ModelAttribute
    public void addAdminBreadcrumbs(Model model, HttpServletRequest request, Locale locale) {
        if (model.containsAttribute("breadcrumbs")) {
            return;
        }

        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/admin")) {
            return;
        }

        String adminLabel = messageSource.getMessage("nav.admin", null, locale);
        String usersLabel = messageSource.getMessage("nav.admin.users", null, locale);

        if (path.startsWith("/admin/users")) {
            model.addAttribute(
                    "breadcrumbs",
                    new BreadcrumbsData(List.of(
                            new BreadcrumbItemData(adminLabel, "/admin", false),
                            new BreadcrumbItemData(usersLabel, null, true))));
            return;
        }

        model.addAttribute("breadcrumbs", new BreadcrumbsData(List.of(new BreadcrumbItemData(adminLabel, null, true))));
    }
}
