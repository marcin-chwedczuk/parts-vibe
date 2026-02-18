package app.partsvibe.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class AbstractPostgresIntegrationTest {
    @DynamicPropertySource
    static void configureSharedProperties(DynamicPropertyRegistry registry) {
        IntegrationTestDatabase.registerSharedProperties(registry);
    }
}
