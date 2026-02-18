package app.partsvibe.users.web.form;

import app.partsvibe.shared.cqrs.PaginationPolicy;
import app.partsvibe.shared.utils.StringUtils;
import app.partsvibe.users.queries.usermanagement.GetUserManagementGridQuery;
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
    private static final Set<String> ALLOWED_SORT_BY = Set.of(
            GetUserManagementGridQuery.SORT_NONE,
            GetUserManagementGridQuery.SORT_BY_USERNAME,
            GetUserManagementGridQuery.SORT_BY_ENABLED);
    private static final Set<String> ALLOWED_SORT_DIR =
            Set.of(GetUserManagementGridQuery.SORT_ASC, GetUserManagementGridQuery.SORT_DESC);

    private String username = "";
    private String enabled = GetUserManagementGridQuery.ENABLED_ALL;
    private List<String> roles = new ArrayList<>();
    private int page = 1;
    private int size = PaginationPolicy.DEFAULT_PAGE_SIZE;
    private String sortBy = GetUserManagementGridQuery.SORT_NONE;
    private String sortDir = GetUserManagementGridQuery.SORT_ASC;

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
            nextSortDir = GetUserManagementGridQuery.SORT_ASC;
        } else if (GetUserManagementGridQuery.SORT_ASC.equals(sortDir)) {
            nextSortBy = requestedSortBy;
            nextSortDir = GetUserManagementGridQuery.SORT_DESC;
        } else {
            nextSortBy = GetUserManagementGridQuery.SORT_NONE;
            nextSortDir = GetUserManagementGridQuery.SORT_ASC;
        }

        UserManagementFilters nextState = copyOf(this);
        nextState.setPage(1);
        nextState.setSortBy(nextSortBy);
        nextState.setSortDir(nextSortDir);

        String direction = active ? sortDir : GetUserManagementGridQuery.SORT_NONE;
        return new SortLink(nextState.toUserManagementUrl(), active, direction);
    }

    public void sanitize() {
        if (page < 1) {
            page = 1;
        }
        if (!PaginationPolicy.ALLOWED_PAGE_SIZES.contains(size)) {
            size = PaginationPolicy.DEFAULT_PAGE_SIZE;
        }
        if (enabled == null
                || (!enabled.equals(GetUserManagementGridQuery.ENABLED_ALL)
                        && !enabled.equals(GetUserManagementGridQuery.ENABLED_ENABLED)
                        && !enabled.equals(GetUserManagementGridQuery.ENABLED_DISABLED))) {
            enabled = GetUserManagementGridQuery.ENABLED_ALL;
        }
        if (!ALLOWED_SORT_BY.contains(sortBy)) {
            sortBy = GetUserManagementGridQuery.SORT_NONE;
        }
        if (!ALLOWED_SORT_DIR.contains(sortDir)) {
            sortDir = GetUserManagementGridQuery.SORT_ASC;
        }
        if (roles == null) {
            roles = new ArrayList<>();
        }
    }

    public static List<Integer> allowedPageSizes() {
        return PaginationPolicy.ALLOWED_PAGE_SIZES;
    }
}
