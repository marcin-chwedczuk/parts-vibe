package app.partsvibe.testsupport.examples;

import static org.junit.jupiter.api.Assertions.assertTrue;

import app.partsvibe.testsupport.web.AbstractWebMvcIntegrationTest;
import app.partsvibe.testsupport.web.WebTestBaseApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.bind.annotation.GetMapping;

@ContextConfiguration(classes = ExampleWebTest.ExampleWebApplication.class)
class ExampleWebTest extends AbstractWebMvcIntegrationTest {
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
