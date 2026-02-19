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

    static boolean isSqlDebugEnabled() {
        return Boolean.parseBoolean(System.getProperty("it.sql.debug", "false"));
    }

    static void applySqlDebugLoggingSystemProperties() {
        System.setProperty("logging.level.org.hibernate.SQL", "DEBUG");
        System.setProperty("logging.level.org.hibernate.orm.sql", "DEBUG");
        System.setProperty("logging.level.org.hibernate.orm.jdbc", "TRACE");
        System.setProperty("logging.level.org.hibernate.orm.jdbc.bind", "TRACE");
        System.setProperty("logging.level.org.hibernate.orm.jdbc.extract", "TRACE");
    }

    public static void registerSharedProperties(DynamicPropertyRegistry registry) {
        String schemaJdbcUrl = jdbcUrlWithCurrentSchema(SHARED_SCHEMA);
        boolean sqlDebug = isSqlDebugEnabled();

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

        if (sqlDebug) {
            registry.add("spring.jpa.show-sql", () -> "true");
            registry.add("spring.jpa.properties.hibernate.show_sql", () -> "true");
            registry.add("spring.jpa.properties.hibernate.format_sql", () -> "true");
            registry.add("spring.jpa.properties.hibernate.use_sql_comments", () -> "true");
        }
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
