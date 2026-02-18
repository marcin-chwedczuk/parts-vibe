package app.partsvibe.users.queries.usermanagement;

import app.partsvibe.shared.cqrs.PageResult;
import app.partsvibe.shared.cqrs.PaginatedQuery;
import java.util.List;
import lombok.Builder;

@Builder
public record SearchUsersQuery(
        String usernameContains,
        Boolean enabledIs,
        List<String> rolesContainAll,
        int currentPage,
        int pageSize,
        String sortBy,
        String sortDir)
        implements PaginatedQuery<PageResult<SearchUsersQuery.UserRow>> {

    public static final String SORT_NONE = "none";
    public static final String SORT_ASC = "asc";
    public static final String SORT_DESC = "desc";
    public static final String SORT_BY_USERNAME = "username";
    public static final String SORT_BY_ENABLED = "enabled";

    public record UserRow(Long id, String username, boolean enabled, List<String> roles) {}
}
