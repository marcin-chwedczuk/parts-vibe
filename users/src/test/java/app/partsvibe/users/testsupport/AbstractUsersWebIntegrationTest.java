package app.partsvibe.users.testsupport;

import app.partsvibe.testsupport.AbstractWebMvcIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = UsersWebModuleTestApplication.class)
public abstract class AbstractUsersWebIntegrationTest extends AbstractWebMvcIntegrationTest {
    @Autowired
    protected InMemoryMediator mediator;

    @BeforeEach
    void setupWebTestContext() {
        setupMockMvc();
        mediator.clear();
    }
}
