package app.partsvibe.users.domain;

import java.util.Set;

public final class RoleNames {
    public static final String ADMIN = "ROLE_ADMIN";
    public static final String USER = "ROLE_USER";
    public static final Set<String> ALL = Set.of(ADMIN, USER);

    private RoleNames() {}
}
