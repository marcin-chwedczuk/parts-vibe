package app.partsvibe.users.queries.usermanagement;

import app.partsvibe.shared.cqrs.Query;
import java.util.List;

public record GetUserManagementGridQuery(
        String username, String enabled, List<String> roles, int page, int size, String sortBy, String sortDir)
        implements Query<GetUserManagementGridQueryResult> {}
