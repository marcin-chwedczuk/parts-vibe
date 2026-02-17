package app.partsvibe.users.web.form;

import app.partsvibe.shared.utils.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.util.UriComponentsBuilder;

@Getter
@Setter
@NoArgsConstructor
public class UserManagementFilters {
    public static final String SORT_NONE = "none";
    public static final String SORT_ASC = "asc";
    public static final String SORT_DESC = "desc";
    public static final String SORT_BY_USERNAME = "username";
    public static final String SORT_BY_ENABLED = "enabled";

    private static final List<Integer> ALLOWED_PAGE_SIZES = List.of(10, 25, 50);
    private static final Set<String> ALLOWED_SORT_BY = Set.of(SORT_NONE, SORT_BY_USERNAME, SORT_BY_ENABLED);
    private static final Set<String> ALLOWED_SORT_DIR = Set.of(SORT_ASC, SORT_DESC);

    private String username = "";
    private String enabled = "all";
    private List<String> roles = new ArrayList<>();
    private int page = 1;
    private int size = 10;
    private String sortBy = SORT_NONE;
    private String sortDir = SORT_ASC;

    public static UserManagementFilters copyOf(UserManagementFilters source) {
        UserManagementFilters copy = new UserManagementFilters();
        if (source == null) {
            return copy;
        }

        copy.setUsername(source.getUsername());
        copy.setEnabled(source.getEnabled());
        copy.setRoles(new ArrayList<>(source.getRoles()));
        copy.setPage(source.getPage());
        copy.setSize(source.getSize());
        copy.setSortBy(source.getSortBy());
        copy.setSortDir(source.getSortDir());
        return copy;
    }

    public String toUserManagementUrl() {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/users")
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParam("enabled", enabled)
                .queryParam("sortBy", sortBy)
                .queryParam("sortDir", sortDir);

        if (StringUtils.hasText(username)) {
            builder.queryParam("username", username.trim());
        }
        for (String role : roles) {
            builder.queryParam("roles", role);
        }
        return builder.build().toUriString();
    }

    public SortLink buildSortLink(String requestedSortBy) {
        boolean active = requestedSortBy.equals(sortBy);

        String nextSortBy;
        String nextSortDir;
        if (!active) {
            nextSortBy = requestedSortBy;
            nextSortDir = SORT_ASC;
        } else if (SORT_ASC.equals(sortDir)) {
            nextSortBy = requestedSortBy;
            nextSortDir = SORT_DESC;
        } else {
            nextSortBy = SORT_NONE;
            nextSortDir = SORT_ASC;
        }

        UserManagementFilters nextState = copyOf(this);
        nextState.setPage(1);
        nextState.setSortBy(nextSortBy);
        nextState.setSortDir(nextSortDir);

        String direction = active ? sortDir : SORT_NONE;
        return new SortLink(nextState.toUserManagementUrl(), active, direction);
    }

    public void sanitize() {
        if (page < 1) {
            page = 1;
        }
        if (!ALLOWED_PAGE_SIZES.contains(size)) {
            size = 10;
        }
        if (enabled == null || (!enabled.equals("all") && !enabled.equals("enabled") && !enabled.equals("disabled"))) {
            enabled = "all";
        }
        if (!ALLOWED_SORT_BY.contains(sortBy)) {
            sortBy = SORT_NONE;
        }
        if (!ALLOWED_SORT_DIR.contains(sortDir)) {
            sortDir = SORT_ASC;
        }
        if (roles == null) {
            roles = new ArrayList<>();
        }
    }

    public static List<Integer> allowedPageSizes() {
        return ALLOWED_PAGE_SIZES;
    }
}
