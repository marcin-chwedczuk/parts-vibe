package app.partsvibe.users.test.web;

import app.partsvibe.testsupport.fakes.InMemoryMediator;
import app.partsvibe.testsupport.web.AbstractWebMvcIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = UsersWebTestApplication.class)
public abstract class AbstractUsersWebIntegrationTest extends AbstractWebMvcIntegrationTest {
    @Autowired
    protected InMemoryMediator mediator;

    @BeforeEach
    void setupWebTestContext() {
        mediator.clear();
    }
}
