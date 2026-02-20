package app.partsvibe.testsupport.web;

import app.partsvibe.testsupport.fakes.InMemoryMediator;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = UsersWebModuleTestApplication.class)
public abstract class AbstractUsersWebIntegrationTest extends AbstractWebMvcIntegrationTest {
    @Autowired
    protected InMemoryMediator mediator;

    @BeforeEach
    void setupWebTestContext() {
        mediator.clear();
    }
}
