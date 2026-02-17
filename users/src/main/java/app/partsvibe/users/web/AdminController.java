package app.partsvibe.users.web;

import app.partsvibe.shared.cqrs.Mediator;
import app.partsvibe.users.commands.admin.TriggerRetentionCleanupCommand;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
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
@RequestMapping("/admin")
public class AdminController {
    private static final List<Integer> ALLOWED_PAGE_SIZES = List.of(10, 25, 50);
    private static final Set<String> ALLOWED_ROLES = Set.of("ROLE_USER", "ROLE_ADMIN");
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final Mediator mediator;

    public AdminController(Mediator mediator) {
        this.mediator = mediator;
    }

    @GetMapping
    public String adminHome() {
        log.info("Admin dashboard requested");
        return "admin";
    }

    @GetMapping("/users")
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

        log.info("Admin user management page requested");
        return "admin/users";
    }

    @PostMapping("/users/{userId}/do-delete")
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

    @PostMapping("/users/{userId}/do-lock")
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

    @PostMapping("/users/{userId}/do-unlock")
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

    @PostMapping("/event-queue/retention-cleanup")
    public String triggerRetentionCleanup(RedirectAttributes redirectAttributes) {
        UUID eventId =
                mediator.executeCommand(new TriggerRetentionCleanupCommand()).eventId();

        log.info("Retention cleanup trigger event published from admin page. eventId={}", eventId);
        redirectAttributes.addFlashAttribute("retentionCleanupTriggered", true);
        redirectAttributes.addFlashAttribute("retentionCleanupEventId", eventId);

        return "redirect:/admin";
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
                .sorted(Comparator.comparing(UserGridRow::username))
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
    }

    private String buildUserManagementRedirect(UserManagementFilters filters) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/users")
                .queryParam("page", filters.getPage())
                .queryParam("size", filters.getSize())
                .queryParam("enabled", filters.getEnabled());

        if (StringUtils.hasText(filters.getUsername())) {
            builder.queryParam("username", filters.getUsername().trim());
        }
        for (String role : filters.getRoles()) {
            builder.queryParam("roles", role);
        }
        return builder.build().toUriString();
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

    public static class UserManagementFilters {
        private String username = "";
        private String enabled = "all";
        private List<String> roles = new ArrayList<>();
        private int page = 1;
        private int size = 10;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEnabled() {
            return enabled;
        }

        public void setEnabled(String enabled) {
            this.enabled = enabled;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }
    }

    public record UserGridRow(Long id, String username, boolean enabled, List<String> roles) {
        public String rolesDisplay() {
            return String.join(", ", roles);
        }
    }
}
