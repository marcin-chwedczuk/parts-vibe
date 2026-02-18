package app.partsvibe.users.web;

import app.partsvibe.shared.cqrs.Mediator;
import app.partsvibe.shared.utils.StringUtils;
import app.partsvibe.users.queries.usermanagement.GetUserManagementGridQuery;
import app.partsvibe.users.queries.usermanagement.GetUserManagementGridQueryResult;
import app.partsvibe.users.web.form.HiddenField;
import app.partsvibe.users.web.form.PageLink;
import app.partsvibe.users.web.form.UserGridRow;
import app.partsvibe.users.web.form.UserManagementFilters;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UsersController {
    private static final Set<String> ALLOWED_ROLES = Set.of("ROLE_USER", "ROLE_ADMIN");
    private static final Logger log = LoggerFactory.getLogger(UsersController.class);

    private final Mediator mediator;

    public UsersController(Mediator mediator) {
        this.mediator = mediator;
    }

    @GetMapping
    public String userManagement(@ModelAttribute("filters") UserManagementFilters filters, Model model) {
        filters.sanitize();
        sanitizeRoleFilters(filters);

        GetUserManagementGridQueryResult result = mediator.executeQuery(new GetUserManagementGridQuery(
                filters.getUsername(),
                filters.getEnabled(),
                List.copyOf(filters.getRoles()),
                filters.getPage(),
                filters.getSize(),
                filters.getSortBy(),
                filters.getSortDir()));

        List<UserGridRow> pagedUsers = result.rows().stream()
                .map(row -> new UserGridRow(row.id(), row.username(), row.enabled(), row.roles()))
                .toList();

        int totalPages = result.totalPages();
        filters.setPage(result.currentPage());
        List<Integer> pageNumbers = buildPageNumbers(totalPages);

        model.addAttribute("users", pagedUsers);
        model.addAttribute("availableRoles", ALLOWED_ROLES.stream().sorted().toList());
        model.addAttribute("pageSizes", UserManagementFilters.allowedPageSizes());
        model.addAttribute("pageNumbers", pageNumbers);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalRows", result.totalRows());
        model.addAttribute("startRow", result.startRow());
        model.addAttribute("endRow", result.endRow());
        model.addAttribute("sortUsername", filters.buildSortLink(GetUserManagementGridQuery.SORT_BY_USERNAME));
        model.addAttribute("sortEnabled", filters.buildSortLink(GetUserManagementGridQuery.SORT_BY_ENABLED));
        model.addAttribute("paginationFirstUrl", buildPageUrl(filters, 1));
        model.addAttribute("paginationLastUrl", buildPageUrl(filters, totalPages));
        model.addAttribute("paginationPageLinks", buildPaginationPageLinks(filters, pageNumbers));
        model.addAttribute("hiddenFieldsForActions", buildHiddenFieldsForActions(filters));
        model.addAttribute("hiddenFieldsForPageSize", buildHiddenFieldsForPageSize(filters));

        log.info("Admin user management page requested");
        return "admin/users";
    }

    @PostMapping("/{userId}/do-delete")
    public String deleteUser(
            @PathVariable("userId") Long userId,
            @ModelAttribute UserManagementFilters filters,
            RedirectAttributes redirectAttributes) {
        filters.sanitize();
        sanitizeRoleFilters(filters);
        // TODO: Delete user in application service/repository layer.
        log.info("Admin requested user delete. userId={}", userId);
        redirectAttributes.addFlashAttribute("actionMessageCode", "admin.users.action.deleted");
        return "redirect:" + filters.toUserManagementUrl();
    }

    @PostMapping("/{userId}/do-lock")
    public String lockUser(
            @PathVariable("userId") Long userId,
            @ModelAttribute UserManagementFilters filters,
            RedirectAttributes redirectAttributes) {
        filters.sanitize();
        sanitizeRoleFilters(filters);
        // TODO: Lock user in application service/repository layer.
        log.info("Admin requested user lock. userId={}", userId);
        redirectAttributes.addFlashAttribute("actionMessageCode", "admin.users.action.locked");
        return "redirect:" + filters.toUserManagementUrl();
    }

    @PostMapping("/{userId}/do-unlock")
    public String unlockUser(
            @PathVariable("userId") Long userId,
            @ModelAttribute UserManagementFilters filters,
            RedirectAttributes redirectAttributes) {
        filters.sanitize();
        sanitizeRoleFilters(filters);
        // TODO: Unlock user in application service/repository layer.
        log.info("Admin requested user unlock. userId={}", userId);
        redirectAttributes.addFlashAttribute("actionMessageCode", "admin.users.action.unlocked");
        return "redirect:" + filters.toUserManagementUrl();
    }

    private List<Integer> buildPageNumbers(int totalPages) {
        int pageCount = Math.min(totalPages, 10);
        return IntStream.rangeClosed(1, pageCount).boxed().toList();
    }

    private List<PageLink> buildPaginationPageLinks(UserManagementFilters filters, List<Integer> pageNumbers) {
        List<PageLink> links = new ArrayList<>();
        for (Integer pageNumber : pageNumbers) {
            links.add(new PageLink(pageNumber, buildPageUrl(filters, pageNumber)));
        }
        return links;
    }

    private String buildPageUrl(UserManagementFilters filters, int page) {
        UserManagementFilters pageState = UserManagementFilters.copyOf(filters);
        pageState.setPage(page);
        return pageState.toUserManagementUrl();
    }

    private List<HiddenField> buildHiddenFieldsForActions(UserManagementFilters filters) {
        List<HiddenField> fields = buildHiddenFieldsBase(filters);
        fields.add(new HiddenField("page", String.valueOf(filters.getPage())));
        fields.add(new HiddenField("size", String.valueOf(filters.getSize())));
        return fields;
    }

    private List<HiddenField> buildHiddenFieldsForPageSize(UserManagementFilters filters) {
        return buildHiddenFieldsBase(filters);
    }

    private List<HiddenField> buildHiddenFieldsBase(UserManagementFilters filters) {
        List<HiddenField> fields = new ArrayList<>();
        fields.add(new HiddenField("username", StringUtils.normalizeToEmpty(filters.getUsername())));
        fields.add(new HiddenField("enabled", filters.getEnabled()));
        fields.add(new HiddenField("sortBy", filters.getSortBy()));
        fields.add(new HiddenField("sortDir", filters.getSortDir()));
        for (String role : filters.getRoles()) {
            fields.add(new HiddenField("roles", role));
        }
        return fields;
    }

    private void sanitizeRoleFilters(UserManagementFilters filters) {
        if (filters.getRoles() == null) {
            filters.setRoles(new ArrayList<>());
        }
        filters.setRoles(filters.getRoles().stream()
                .filter(ALLOWED_ROLES::contains)
                .distinct()
                .toList());
    }
}
