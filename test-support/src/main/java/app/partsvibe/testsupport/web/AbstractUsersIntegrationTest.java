package app.partsvibe.testsupport.web;

import app.partsvibe.testsupport.it.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = UsersModuleTestApplication.class)
@Transactional
public abstract class AbstractUsersIntegrationTest extends AbstractPostgresIntegrationTest {
    @Autowired
    protected InMemoryRequestIdProvider requestIdProvider;

    @Autowired
    protected InMemoryEventPublisher eventPublisher;

    @Autowired
    protected ManuallySetTimeProvider timeProvider;

    @Autowired
    protected InMemoryCurrentUserProvider currentUserProvider;

    @BeforeEach
    void prepareTestContext(TestInfo testInfo) {
        requestIdProvider.set(testInfo.getDisplayName());
        eventPublisher.clear();
        timeProvider.reset();
        currentUserProvider.setCurrentUser(testInfo.getDisplayName());
    }
}
