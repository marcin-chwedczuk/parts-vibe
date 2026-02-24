package app.partsvibe.users.web.form;

import app.partsvibe.shared.cqrs.PaginationPolicy;
import app.partsvibe.shared.utils.StringUtils;
import app.partsvibe.users.queries.usermanagement.SearchUsersQuery;
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
public class UserFilters {
    private static final Set<String> ALLOWED_SORT_BY =
            Set.of(SearchUsersQuery.SORT_NONE, SearchUsersQuery.SORT_BY_USERNAME, SearchUsersQuery.SORT_BY_ENABLED);
    private static final Set<String> ALLOWED_SORT_DIR = Set.of(SearchUsersQuery.SORT_ASC, SearchUsersQuery.SORT_DESC);

    private String usernameContains = "";
    private Boolean enabledIs = null;
    private List<String> rolesContainAll = new ArrayList<>();
    private int page = 1;
    private int size = PaginationPolicy.DEFAULT_PAGE_SIZE;
    private String sortBy = SearchUsersQuery.SORT_NONE;
    private String sortDir = SearchUsersQuery.SORT_ASC;

    public static UserFilters copyOf(UserFilters source) {
        UserFilters copy = new UserFilters();
        if (source == null) {
            return copy;
        }

        copy.setUsernameContains(source.getUsernameContains());
        copy.setEnabledIs(source.getEnabledIs());
        copy.setRolesContainAll(new ArrayList<>(source.getRolesContainAll()));
        copy.setPage(source.getPage());
        copy.setSize(source.getSize());
        copy.setSortBy(source.getSortBy());
        copy.setSortDir(source.getSortDir());
        return copy;
    }

    public String toUserManagementUrl() {
        return managementUrlBuilder().build().toUriString();
    }

    public String toUserViewUrl(Long userId) {
        return managementUrlBuilder()
                .replacePath("/admin/users/{id}")
                .buildAndExpand(userId)
                .toUriString();
    }

    public String toUserEditUrl(Long userId) {
        return managementUrlBuilder()
                .replacePath("/admin/users/{id}/edit")
                .buildAndExpand(userId)
                .toUriString();
    }

    public String toUserCreateUrl() {
        return managementUrlBuilder().replacePath("/admin/users/create").build().toUriString();
    }

    public String toUserInviteUrl() {
        return managementUrlBuilder().replacePath("/admin/users/invite").build().toUriString();
    }

    private UriComponentsBuilder managementUrlBuilder() {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/users")
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParam("sortBy", sortBy)
                .queryParam("sortDir", sortDir);

        if (StringUtils.hasText(usernameContains)) {
            builder.queryParam("usernameContains", usernameContains.trim());
        }
        if (enabledIs != null) {
            builder.queryParam("enabledIs", enabledIs);
        }
        for (String role : rolesContainAll) {
            builder.queryParam("rolesContainAll", role);
        }
        return builder;
    }

    public SortLink buildSortLink(String requestedSortBy) {
        boolean active = requestedSortBy.equals(sortBy);

        String nextSortBy;
        String nextSortDir;
        if (!active) {
            nextSortBy = requestedSortBy;
            nextSortDir = SearchUsersQuery.SORT_ASC;
        } else if (SearchUsersQuery.SORT_ASC.equals(sortDir)) {
            nextSortBy = requestedSortBy;
            nextSortDir = SearchUsersQuery.SORT_DESC;
        } else {
            nextSortBy = SearchUsersQuery.SORT_NONE;
            nextSortDir = SearchUsersQuery.SORT_ASC;
        }

        UserFilters nextState = copyOf(this);
        nextState.setPage(1);
        nextState.setSortBy(nextSortBy);
        nextState.setSortDir(nextSortDir);

        String direction = active ? sortDir : SearchUsersQuery.SORT_NONE;
        return new SortLink(nextState.toUserManagementUrl(), active, direction);
    }

    public void sanitize() {
        if (usernameContains == null) {
            usernameContains = "";
        }
        if (page < 1) {
            page = 1;
        }
        if (!PaginationPolicy.ALLOWED_PAGE_SIZES.contains(size)) {
            size = PaginationPolicy.DEFAULT_PAGE_SIZE;
        }
        if (!ALLOWED_SORT_BY.contains(sortBy)) {
            sortBy = SearchUsersQuery.SORT_NONE;
        }
        if (!ALLOWED_SORT_DIR.contains(sortDir)) {
            sortDir = SearchUsersQuery.SORT_ASC;
        }
        if (rolesContainAll == null) {
            rolesContainAll = new ArrayList<>();
        }
    }

    public static List<Integer> allowedPageSizes() {
        return PaginationPolicy.ALLOWED_PAGE_SIZES;
    }
}
