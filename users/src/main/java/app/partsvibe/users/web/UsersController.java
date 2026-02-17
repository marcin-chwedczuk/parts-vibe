package app.partsvibe.users.web;

import app.partsvibe.users.web.form.UserGridRow;
import app.partsvibe.users.web.form.UserManagementFilters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequestMapping("/admin/users")
public class UsersController {
    private static final List<Integer> ALLOWED_PAGE_SIZES = List.of(10, 25, 50);
    private static final Set<String> ALLOWED_ROLES = Set.of("ROLE_USER", "ROLE_ADMIN");
    private static final Set<String> ALLOWED_SORT_BY = Set.of("none", "username", "enabled");
    private static final Set<String> ALLOWED_SORT_DIR = Set.of("asc", "desc");
    private static final Logger log = LoggerFactory.getLogger(UsersController.class);

    @GetMapping
    public String userManagement(@ModelAttribute("filters") UserManagementFilters filters, Model model) {
        sanitizeFilters(filters);
        List<UserGridRow> allUsers = buildDummyUsers();
        List<UserGridRow> filteredUsers = applyFilters(allUsers, filters);
        List<UserGridRow> pagedUsers = applyPagination(filteredUsers, filters);

        model.addAttribute("users", pagedUsers);
        model.addAttribute("availableRoles", ALLOWED_ROLES.stream().sorted().toList());
        model.addAttribute("pageNumbers", buildPageNumbers(filteredUsers.size(), filters.getSize()));
        model.addAttribute("totalPages", computeTotalPages(filteredUsers.size(), filters.getSize()));
        model.addAttribute("totalRows", filteredUsers.size());
        int startRow = filteredUsers.isEmpty() ? 0 : ((filters.getPage() - 1) * filters.getSize()) + 1;
        int endRow = filteredUsers.isEmpty() ? 0 : startRow + pagedUsers.size() - 1;
        model.addAttribute("startRow", startRow);
        model.addAttribute("endRow", endRow);

        log.info("Admin user management page requested");
        return "admin/users";
    }

    @PostMapping("/{userId}/do-delete")
    public String deleteUser(
            @PathVariable("userId") Long userId,
            @ModelAttribute UserManagementFilters filters,
            RedirectAttributes redirectAttributes) {
        sanitizeFilters(filters);
        // TODO: Delete user in application service/repository layer.
        log.info("Admin requested user delete. userId={}", userId);
        redirectAttributes.addFlashAttribute("actionMessageCode", "admin.users.action.deleted");
        return "redirect:" + buildUserManagementRedirect(filters);
    }

    @PostMapping("/{userId}/do-lock")
    public String lockUser(
            @PathVariable("userId") Long userId,
            @ModelAttribute UserManagementFilters filters,
            RedirectAttributes redirectAttributes) {
        sanitizeFilters(filters);
        // TODO: Lock user in application service/repository layer.
        log.info("Admin requested user lock. userId={}", userId);
        redirectAttributes.addFlashAttribute("actionMessageCode", "admin.users.action.locked");
        return "redirect:" + buildUserManagementRedirect(filters);
    }

    @PostMapping("/{userId}/do-unlock")
    public String unlockUser(
            @PathVariable("userId") Long userId,
            @ModelAttribute UserManagementFilters filters,
            RedirectAttributes redirectAttributes) {
        sanitizeFilters(filters);
        // TODO: Unlock user in application service/repository layer.
        log.info("Admin requested user unlock. userId={}", userId);
        redirectAttributes.addFlashAttribute("actionMessageCode", "admin.users.action.unlocked");
        return "redirect:" + buildUserManagementRedirect(filters);
    }

    private List<UserGridRow> applyFilters(List<UserGridRow> allUsers, UserManagementFilters filters) {
        String username = filters.getUsername() == null
                ? ""
                : filters.getUsername().trim().toLowerCase(Locale.ROOT);
        Set<String> selectedRoles = new HashSet<>(filters.getRoles());

        return allUsers.stream()
                .filter(user -> username.isBlank()
                        || user.username().toLowerCase(Locale.ROOT).contains(username))
                .filter(user -> matchEnabled(user, filters.getEnabled()))
                .filter(user -> user.roles().containsAll(selectedRoles))
                .sorted(buildSortComparator(filters))
                .toList();
    }

    private boolean matchEnabled(UserGridRow user, String enabledFilter) {
        if ("enabled".equals(enabledFilter)) {
            return user.enabled();
        }
        if ("disabled".equals(enabledFilter)) {
            return !user.enabled();
        }
        return true;
    }

    private List<UserGridRow> applyPagination(List<UserGridRow> users, UserManagementFilters filters) {
        int totalPages = computeTotalPages(users.size(), filters.getSize());
        int page = Math.min(filters.getPage(), totalPages);
        filters.setPage(page);
        int start = (page - 1) * filters.getSize();
        int end = Math.min(start + filters.getSize(), users.size());
        if (start >= users.size()) {
            return List.of();
        }
        return users.subList(start, end);
    }

    private int computeTotalPages(int totalRows, int size) {
        return Math.max(1, (int) Math.ceil(totalRows / (double) size));
    }

    private List<Integer> buildPageNumbers(int totalRows, int size) {
        int totalPages = computeTotalPages(totalRows, size);
        int pageCount = Math.min(totalPages, 10);
        return IntStream.rangeClosed(1, pageCount).boxed().toList();
    }

    private void sanitizeFilters(UserManagementFilters filters) {
        if (filters.getPage() < 1) {
            filters.setPage(1);
        }
        if (!ALLOWED_PAGE_SIZES.contains(filters.getSize())) {
            filters.setSize(10);
        }
        if (filters.getEnabled() == null
                || (!filters.getEnabled().equals("all")
                        && !filters.getEnabled().equals("enabled")
                        && !filters.getEnabled().equals("disabled"))) {
            filters.setEnabled("all");
        }
        if (filters.getRoles() == null) {
            filters.setRoles(new ArrayList<>());
        }
        filters.setRoles(filters.getRoles().stream()
                .filter(ALLOWED_ROLES::contains)
                .distinct()
                .toList());
        if (!ALLOWED_SORT_BY.contains(filters.getSortBy())) {
            filters.setSortBy("none");
        }
        if (!ALLOWED_SORT_DIR.contains(filters.getSortDir())) {
            filters.setSortDir("asc");
        }
    }

    private String buildUserManagementRedirect(UserManagementFilters filters) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/users")
                .queryParam("page", filters.getPage())
                .queryParam("size", filters.getSize())
                .queryParam("enabled", filters.getEnabled())
                .queryParam("sortBy", filters.getSortBy())
                .queryParam("sortDir", filters.getSortDir());

        if (StringUtils.hasText(filters.getUsername())) {
            builder.queryParam("username", filters.getUsername().trim());
        }
        for (String role : filters.getRoles()) {
            builder.queryParam("roles", role);
        }
        return builder.build().toUriString();
    }

    private Comparator<UserGridRow> buildSortComparator(UserManagementFilters filters) {
        if ("none".equals(filters.getSortBy())) {
            return Comparator.comparing(UserGridRow::id);
        }

        Comparator<UserGridRow> comparator;
        if ("enabled".equals(filters.getSortBy())) {
            comparator = Comparator.comparing(UserGridRow::enabled);
        } else {
            comparator = Comparator.comparing(user -> user.username().toLowerCase(Locale.ROOT));
        }

        if ("desc".equals(filters.getSortDir())) {
            comparator = comparator.reversed();
        }

        return comparator.thenComparing(UserGridRow::id);
    }

    private List<UserGridRow> buildDummyUsers() {
        List<UserGridRow> users = new ArrayList<>();
        for (long id = 1; id <= 97; id++) {
            boolean enabled = id % 4 != 0;
            List<String> roles = id % 3 == 0 ? List.of("ROLE_USER", "ROLE_ADMIN") : List.of("ROLE_USER");
            users.add(new UserGridRow(id, "user" + id, enabled, roles));
        }
        users.add(new UserGridRow(201L, "admin", true, List.of("ROLE_USER", "ROLE_ADMIN")));
        users.add(new UserGridRow(202L, "user", true, List.of("ROLE_USER")));
        return users;
    }
}
