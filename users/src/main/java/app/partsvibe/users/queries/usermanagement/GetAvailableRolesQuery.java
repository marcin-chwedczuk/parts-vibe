package app.partsvibe.users.queries.usermanagement;

import app.partsvibe.shared.cqrs.Query;
import java.util.List;

public record GetAvailableRolesQuery() implements Query<List<String>> {}
