package app.partsvibe.testsupport;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;

public final class IntegrationTestDatabase {
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
    private static final String SHARED_SCHEMA = "it_shared";

    static {
        POSTGRES.start();
        ensureSchemaExists(SHARED_SCHEMA);
    }

    private IntegrationTestDatabase() {}

    public static void registerSharedProperties(DynamicPropertyRegistry registry) {
        String schemaJdbcUrl = jdbcUrlWithCurrentSchema(SHARED_SCHEMA);

        registry.add("spring.datasource.url", () -> schemaJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        registry.add("app.datasource.write.jdbc-url", () -> schemaJdbcUrl);
        registry.add("app.datasource.write.username", POSTGRES::getUsername);
        registry.add("app.datasource.write.password", POSTGRES::getPassword);

        registry.add("app.datasource.read.jdbc-url", () -> schemaJdbcUrl);
        registry.add("app.datasource.read.username", POSTGRES::getUsername);
        registry.add("app.datasource.read.password", POSTGRES::getPassword);

        registry.add("spring.jpa.properties.hibernate.default_schema", () -> SHARED_SCHEMA);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    private static String jdbcUrlWithCurrentSchema(String schema) {
        String baseJdbcUrl = POSTGRES.getJdbcUrl();
        String delimiter = baseJdbcUrl.contains("?") ? "&" : "?";
        return baseJdbcUrl + delimiter + "currentSchema=" + schema;
    }

    private static void ensureSchemaExists(String schema) {
        String sql = "CREATE SCHEMA IF NOT EXISTS \"" + schema + "\"";
        try (Connection connection = POSTGRES.createConnection("");
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException ex) {
            throw new IllegalStateException("Cannot create integration-test schema: " + schema, ex);
        }
    }
}
