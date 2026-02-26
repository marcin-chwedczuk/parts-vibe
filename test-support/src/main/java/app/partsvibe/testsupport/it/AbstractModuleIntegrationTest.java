package app.partsvibe.testsupport.it;

import app.partsvibe.testsupport.fakes.InMemoryCurrentUserProvider;
import app.partsvibe.testsupport.fakes.InMemoryEventPublisher;
import app.partsvibe.testsupport.fakes.InMemoryRequestIdProvider;
import app.partsvibe.testsupport.fakes.ManuallySetTimeProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public abstract class AbstractModuleIntegrationTest {
    static {
        if (IntegrationTestDatabase.isSqlDebugEnabled()) {
            IntegrationTestDatabase.applySqlDebugLoggingSystemProperties();
        }
    }

    @DynamicPropertySource
    static void configureSharedProperties(DynamicPropertyRegistry registry) {
        IntegrationTestDatabase.registerSharedProperties(registry);
    }

    @Autowired(required = false)
    protected InMemoryRequestIdProvider requestIdProvider;

    @Autowired(required = false)
    protected InMemoryEventPublisher eventPublisher;

    @Autowired(required = false)
    protected ManuallySetTimeProvider timeProvider;

    @Autowired(required = false)
    protected InMemoryCurrentUserProvider currentUserProvider;

    @BeforeEach
    void prepareSharedTestContext(TestInfo testInfo) {
        if (requestIdProvider != null) {
            requestIdProvider.set(testInfo.getDisplayName());
        }
        if (eventPublisher != null) {
            eventPublisher.clear();
        }
        if (timeProvider != null) {
            timeProvider.reset();
        }
        if (currentUserProvider != null) {
            currentUserProvider.setCurrentUser(testInfo.getDisplayName());
        }
        beforeEachTest(testInfo);
    }

    @AfterEach
    void cleanupSharedTestContext(TestInfo testInfo) {
        afterEachTest(testInfo);
    }

    protected void beforeEachTest(TestInfo testInfo) {
        // extension hook for module-specific test setup
    }

    protected void afterEachTest(TestInfo testInfo) {
        // extension hook for module-specific test cleanup
    }
}
