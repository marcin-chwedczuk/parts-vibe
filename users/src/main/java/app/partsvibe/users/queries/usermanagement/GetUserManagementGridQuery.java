package app.partsvibe.users.queries.usermanagement;

import app.partsvibe.shared.cqrs.Query;
import java.util.List;

public record GetUserManagementGridQuery(
        String username, String enabled, List<String> roles, int page, int size, String sortBy, String sortDir)
        implements Query<GetUserManagementGridQueryResult> {
    public static final String ENABLED_ALL = "all";
    public static final String ENABLED_ENABLED = "enabled";
    public static final String ENABLED_DISABLED = "disabled";
    public static final String SORT_NONE = "none";
    public static final String SORT_ASC = "asc";
    public static final String SORT_DESC = "desc";
    public static final String SORT_BY_USERNAME = "username";
    public static final String SORT_BY_ENABLED = "enabled";
}
