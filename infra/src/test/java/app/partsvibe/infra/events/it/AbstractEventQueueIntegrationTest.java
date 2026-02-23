package app.partsvibe.infra.events.it;

import app.partsvibe.testsupport.it.IntegrationTestDatabase;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(classes = EventQueueItTestApplication.class)
@Execution(ExecutionMode.SAME_THREAD)
public abstract class AbstractEventQueueIntegrationTest {
    @DynamicPropertySource
    static void configureSharedProperties(DynamicPropertyRegistry registry) {
        IntegrationTestDatabase.registerSharedProperties(registry);

        registry.add("app.events.dispatcher.enabled", () -> "true");
        registry.add("app.events.dispatcher.poll-interval-ms", () -> "100");
        registry.add("app.events.dispatcher.max-attempts", () -> "3");
        registry.add("app.events.dispatcher.thread-pool-size", () -> "4");
        registry.add("app.events.dispatcher.thread-pool-queue-capacity", () -> "32");
        registry.add("app.events.dispatcher.handler-timeout-ms", () -> "300");
        registry.add("app.events.dispatcher.processing-timeout-ms", () -> "2000");
        registry.add("app.events.dispatcher.backoff-initial-ms", () -> "25");
        registry.add("app.events.dispatcher.backoff-multiplier", () -> "1.0");
        registry.add("app.events.dispatcher.backoff-max-ms", () -> "25");
    }
}
