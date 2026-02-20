package app.partsvibe.testsupport.examples;

import static org.junit.jupiter.api.Assertions.assertTrue;

import app.partsvibe.testsupport.fakes.TestFakesConfiguration;
import app.partsvibe.testsupport.it.AbstractModuleIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = ExampleModuleIntegrationIT.ExampleItApplication.class)
class ExampleModuleIntegrationIT extends AbstractModuleIntegrationTest {
    @Test
    void exampleItBaseUsage() {
        assertTrue(true);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(TestFakesConfiguration.class)
    static class ExampleItApplication {}
}
