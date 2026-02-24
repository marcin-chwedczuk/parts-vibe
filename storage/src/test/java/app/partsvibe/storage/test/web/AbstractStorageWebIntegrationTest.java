package app.partsvibe.storage.test.web;

import app.partsvibe.testsupport.fakes.InMemoryMediator;
import app.partsvibe.testsupport.web.AbstractWebMvcIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = StorageWebTestApplication.class)
public abstract class AbstractStorageWebIntegrationTest extends AbstractWebMvcIntegrationTest {
    @Autowired
    protected InMemoryMediator mediator;

    @BeforeEach
    void setupWebTestContext() {
        mediator.clear();
    }
}
