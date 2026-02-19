package app.partsvibe.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class AbstractPostgresIntegrationTest {
    static {
        if (IntegrationTestDatabase.isSqlDebugEnabled()) {
            IntegrationTestDatabase.applySqlDebugLoggingSystemProperties();
        }
    }

    @DynamicPropertySource
    static void configureSharedProperties(DynamicPropertyRegistry registry) {
        IntegrationTestDatabase.registerSharedProperties(registry);
    }
}
