package app.partsvibe.users.test.databuilders;

import app.partsvibe.users.domain.Role;
import java.util.UUID;

public final class RoleTestDataBuilder {
    private String name =
            "ROLE_TEST_" + UUID.randomUUID().toString().replace("-", "").toUpperCase();

    private RoleTestDataBuilder() {}

    public static RoleTestDataBuilder aRole() {
        return new RoleTestDataBuilder();
    }

    public RoleTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public Role build() {
        return new Role(name);
    }
}
