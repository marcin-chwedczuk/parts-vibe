package app.partsvibe.infra.events.it;

import app.partsvibe.infra.events.it.support.QueueTestEventHandler;
import app.partsvibe.infra.events.it.support.QueueTestEventProbe;
import app.partsvibe.shared.request.RequestIdProvider;
import app.partsvibe.shared.security.CurrentUserProvider;
import app.partsvibe.testsupport.fakes.InMemoryCurrentUserProvider;
import app.partsvibe.testsupport.fakes.InMemoryRequestIdProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;

@TestConfiguration(proxyBeanMethods = false)
@Import({QueueTestEventHandler.class, QueueTestEventProbe.class})
public class EventQueueItTestConfiguration {
    @Bean
    InMemoryRequestIdProvider inMemoryRequestIdProvider() {
        return new InMemoryRequestIdProvider();
    }

    @Bean
    RequestIdProvider requestIdProvider(InMemoryRequestIdProvider provider) {
        return provider;
    }

    @Bean
    InMemoryCurrentUserProvider inMemoryCurrentUserProvider() {
        return new InMemoryCurrentUserProvider();
    }

    @Bean
    CurrentUserProvider currentUserProvider(InMemoryCurrentUserProvider provider) {
        return provider;
    }

    @Bean
    MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    @Primary
    AuditorAware<String> currentUserAuditorAware(CurrentUserProvider currentUserProvider) {
        return () -> currentUserProvider
                .currentUsername()
                .filter(name -> !name.isBlank())
                .or(() -> Optional.of("system"));
    }
}
