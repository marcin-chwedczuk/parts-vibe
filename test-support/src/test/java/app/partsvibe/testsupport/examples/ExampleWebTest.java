package app.partsvibe.testsupport.examples;

import static org.junit.jupiter.api.Assertions.assertTrue;

import app.partsvibe.testsupport.web.AbstractWebMvcIntegrationTest;
import app.partsvibe.testsupport.web.WebTestBaseApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.GetMapping;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = ExampleWebTest.ExampleWebApplication.class)
class ExampleWebTest extends AbstractWebMvcIntegrationTest {
    @BeforeEach
    void setUpWeb() {
        setupMockMvc();
    }

    @Test
    void exampleWebBaseUsage() {
        assertTrue(true);
    }

    @SpringBootConfiguration
    @ComponentScan(
            basePackageClasses = ExampleController.class,
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Controller.class))
    @Import(WebTestBaseApplication.class)
    static class ExampleWebApplication {}

    @Controller
    static class ExampleController {
        @GetMapping("/example")
        String example() {
            return "example";
        }
    }
}
