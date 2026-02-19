package app.partsvibe.users.web;

import app.partsvibe.shared.cqrs.Mediator;
import app.partsvibe.shared.cqrs.PageResult;
import app.partsvibe.shared.utils.StringUtils;
import app.partsvibe.users.commands.usermanagement.CannotDeleteCurrentUserException;
import app.partsvibe.users.commands.usermanagement.CannotDeleteLastActiveAdminException;
import app.partsvibe.users.commands.usermanagement.DeleteUserCommand;
import app.partsvibe.users.commands.usermanagement.DeleteUserCommandResult;
import app.partsvibe.users.queries.usermanagement.SearchUsersQuery;
import app.partsvibe.users.web.form.HiddenField;
import app.partsvibe.users.web.form.PageLink;
import app.partsvibe.users.web.form.UserFilters;
import app.partsvibe.users.web.form.UserForm;
import app.partsvibe.users.web.form.UserRow;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
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
    private static final Object[] NO_MESSAGE_ARGS = new Object[0];

    private final Mediator mediator;
    private final UserWebMapper userWebMapper;

    public UsersController(Mediator mediator, UserWebMapper userWebMapper) {
        this.mediator = mediator;
        this.userWebMapper = userWebMapper;
    }

    @InitBinder("form")
    void initUserFormBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, "username", new StringTrimmerEditor(true));
    }

    @GetMapping
    public String userManagement(@ModelAttribute("filters") UserFilters filters, Model model) {
        filters.sanitize();
        sanitizeRoleFilters(filters);

        SearchUsersQuery query = SearchUsersQuery.builder()
                .usernameContains(filters.getUsernameContains())
                .enabledIs(filters.getEnabledIs())
                .rolesContainAll(List.copyOf(filters.getRolesContainAll()))
                .currentPage(filters.getPage())
                .pageSize(filters.getSize())
                .sortBy(filters.getSortBy())
                .sortDir(filters.getSortDir())
                .build();

        PageResult<SearchUsersQuery.UserRow> result = mediator.executeQuery(query);

        List<UserRow> pagedUsers = userWebMapper.toRows(result.items());

        int totalPages = result.totalPages();
        filters.setPage(result.currentPage());
        List<Integer> pageNumbers = buildPageNumbers(totalPages);

        model.addAttribute("users", pagedUsers);
        model.addAttribute("availableRoles", ALLOWED_ROLES.stream().sorted().toList());
        model.addAttribute("pageSizes", UserFilters.allowedPageSizes());
        model.addAttribute("pageNumbers", pageNumbers);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalRows", result.totalRows());
        model.addAttribute("startRow", result.startRow());
        model.addAttribute("endRow", result.endRow());
        model.addAttribute("sortUsername", filters.buildSortLink(SearchUsersQuery.SORT_BY_USERNAME));
        model.addAttribute("sortEnabled", filters.buildSortLink(SearchUsersQuery.SORT_BY_ENABLED));
        model.addAttribute("paginationFirstUrl", buildPageUrl(filters, 1));
        model.addAttribute("paginationLastUrl", buildPageUrl(filters, totalPages));
        model.addAttribute("paginationPageLinks", buildPaginationPageLinks(filters, pageNumbers));
        model.addAttribute("hiddenFieldsForActions", buildHiddenFieldsForActions(filters));
        model.addAttribute("hiddenFieldsForPageSize", buildHiddenFieldsForPageSize(filters));

        log.info("Admin user management page requested");
        return "admin/users";
    }

    @GetMapping("/{userId}")
    public String viewUser(
            @PathVariable("userId") Long userId, @ModelAttribute("filters") UserFilters filters, Model model) {
        filters.sanitize();
        sanitizeRoleFilters(filters);
        model.addAttribute("user", fakeUser(userId));
        model.addAttribute("hiddenFieldsForActions", buildHiddenFieldsForActions(filters));
        return "admin/user-view";
    }

    @GetMapping("/{userId}/edit")
    public String editUser(
            @PathVariable("userId") Long userId, @ModelAttribute("filters") UserFilters filters, Model model) {
        filters.sanitize();
        sanitizeRoleFilters(filters);
        if (!model.containsAttribute("form")) {
            UserRow fakeUser = fakeUser(userId);
            UserForm form = new UserForm();
            form.setUsername(fakeUser.username());
            form.setEnabled(fakeUser.enabled());
            model.addAttribute("form", form);
        }
        model.addAttribute("userId", userId);
        addUserFormPageModel(filters, model);
        return "admin/user-edit";
    }

    @GetMapping("/create")
    public String createUser(@ModelAttribute("filters") UserFilters filters, Model model) {
        filters.sanitize();
        sanitizeRoleFilters(filters);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new UserForm());
        }
        addUserFormPageModel(filters, model);
        return "admin/user-create";
    }

    @PostMapping("/create")
    public String saveUserCreate(
            @ModelAttribute UserFilters filters,
            @Valid @ModelAttribute("form") UserForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        filters.sanitize();
        sanitizeRoleFilters(filters);
        if (bindingResult.hasErrors()) {
            addUserFormPageModel(filters, model);
            return "admin/user-create";
        }

        String canonicalUsername = form.getUsername().toLowerCase(Locale.ROOT);
        setActionMessage(redirectAttributes, "admin.users.action.created", "alert-success", canonicalUsername);
        return "redirect:" + filters.toUserManagementUrl();
    }

    @PostMapping("/{userId}/edit")
    public String saveUserEdit(
            @PathVariable("userId") Long userId,
            @ModelAttribute UserFilters filters,
            @Valid @ModelAttribute("form") UserForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        filters.sanitize();
        sanitizeRoleFilters(filters);
        if (bindingResult.hasErrors()) {
            model.addAttribute("userId", userId);
            addUserFormPageModel(filters, model);
            return "admin/user-edit";
        }
        // TODO: Update user via command handler.
        setActionMessage(
                redirectAttributes,
                "admin.users.action.updated",
                "alert-success",
                form.getUsername().toLowerCase(Locale.ROOT));
        return "redirect:" + filters.toUserViewUrl(userId);
    }

    @PostMapping("/{userId}/do-delete")
    public String deleteUser(
            @PathVariable("userId") Long userId,
            @ModelAttribute UserFilters filters,
            RedirectAttributes redirectAttributes) {
        filters.sanitize();
        sanitizeRoleFilters(filters);

        try {
            DeleteUserCommandResult result = mediator.executeCommand(new DeleteUserCommand(userId));

            if (StringUtils.hasText(result.deletedUsername())) {
                setActionMessage(
                        redirectAttributes,
                        "admin.users.action.deleted.withUsername",
                        "alert-success",
                        result.deletedUsername());
            } else {
                setActionMessage(redirectAttributes, "admin.users.action.deleted", "alert-success");
            }
            log.info("Admin requested user delete. userId={}", userId);
        } catch (CannotDeleteCurrentUserException ex) {
            setActionMessage(redirectAttributes, "admin.users.action.deleteCurrentUserDenied", "alert-danger");
            log.warn("Delete user denied. userId={}, reason=self-delete", userId);
        } catch (CannotDeleteLastActiveAdminException ex) {
            setActionMessage(redirectAttributes, "admin.users.action.deleteLastActiveAdminDenied", "alert-danger");
            log.warn("Delete user denied. userId={}, reason=last-active-admin", userId);
        }

        return "redirect:" + filters.toUserManagementUrl();
    }

    @PostMapping("/{userId}/do-lock")
    public String lockUser(
            @PathVariable("userId") Long userId,
            @ModelAttribute UserFilters filters,
            RedirectAttributes redirectAttributes) {
        filters.sanitize();
        sanitizeRoleFilters(filters);
        // TODO: Lock user in application service/repository layer.
        log.info("Admin requested user lock. userId={}", userId);
        setActionMessage(redirectAttributes, "admin.users.action.locked", "alert-success");
        return "redirect:" + filters.toUserManagementUrl();
    }

    @PostMapping("/{userId}/do-unlock")
    public String unlockUser(
            @PathVariable("userId") Long userId,
            @ModelAttribute UserFilters filters,
            RedirectAttributes redirectAttributes) {
        filters.sanitize();
        sanitizeRoleFilters(filters);
        // TODO: Unlock user in application service/repository layer.
        log.info("Admin requested user unlock. userId={}", userId);
        setActionMessage(redirectAttributes, "admin.users.action.unlocked", "alert-success");
        return "redirect:" + filters.toUserManagementUrl();
    }

    private void setActionMessage(
            RedirectAttributes redirectAttributes, String messageCode, String alertClass, Object... messageArgs) {
        redirectAttributes.addFlashAttribute("actionMessageCode", messageCode);
        redirectAttributes.addFlashAttribute("actionMessageArgs", messageArgs == null ? NO_MESSAGE_ARGS : messageArgs);
        redirectAttributes.addFlashAttribute("actionMessageLevel", alertClass);
    }

    private UserRow fakeUser(Long userId) {
        return new UserRow(userId, "bob", true, List.of("ROLE_USER"));
    }

    private void addUserFormPageModel(UserFilters filters, Model model) {
        model.addAttribute("hiddenFieldsForActions", buildHiddenFieldsForActions(filters));
    }

    private List<Integer> buildPageNumbers(int totalPages) {
        int pageCount = Math.min(totalPages, 10);
        return IntStream.rangeClosed(1, pageCount).boxed().toList();
    }

    private List<PageLink> buildPaginationPageLinks(UserFilters filters, List<Integer> pageNumbers) {
        List<PageLink> links = new ArrayList<>();
        for (Integer pageNumber : pageNumbers) {
            links.add(new PageLink(pageNumber, buildPageUrl(filters, pageNumber)));
        }
        return links;
    }

    private String buildPageUrl(UserFilters filters, int page) {
        UserFilters pageState = UserFilters.copyOf(filters);
        pageState.setPage(page);
        return pageState.toUserManagementUrl();
    }

    private List<HiddenField> buildHiddenFieldsForActions(UserFilters filters) {
        List<HiddenField> fields = buildHiddenFieldsBase(filters);
        fields.add(new HiddenField("page", String.valueOf(filters.getPage())));
        fields.add(new HiddenField("size", String.valueOf(filters.getSize())));
        return fields;
    }

    private List<HiddenField> buildHiddenFieldsForPageSize(UserFilters filters) {
        return buildHiddenFieldsBase(filters);
    }

    private List<HiddenField> buildHiddenFieldsBase(UserFilters filters) {
        List<HiddenField> fields = new ArrayList<>();
        fields.add(new HiddenField("usernameContains", StringUtils.normalizeToEmpty(filters.getUsernameContains())));
        fields.add(new HiddenField(
                "enabledIs", filters.getEnabledIs() == null ? "" : String.valueOf(filters.getEnabledIs())));
        fields.add(new HiddenField("sortBy", filters.getSortBy()));
        fields.add(new HiddenField("sortDir", filters.getSortDir()));
        for (String role : filters.getRolesContainAll()) {
            fields.add(new HiddenField("rolesContainAll", role));
        }
        return fields;
    }

    private void sanitizeRoleFilters(UserFilters filters) {
        if (filters.getRolesContainAll() == null) {
            filters.setRolesContainAll(new ArrayList<>());
        }
        filters.setRolesContainAll(filters.getRolesContainAll().stream()
                .filter(ALLOWED_ROLES::contains)
                .distinct()
                .toList());
    }
}
