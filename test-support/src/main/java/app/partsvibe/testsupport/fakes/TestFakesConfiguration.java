package app.partsvibe.testsupport.fakes;

import app.partsvibe.shared.cqrs.Mediator;
import app.partsvibe.shared.events.publishing.EventPublisher;
import app.partsvibe.shared.request.RequestIdProvider;
import app.partsvibe.shared.security.CurrentUserProvider;
import app.partsvibe.shared.time.TimeProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration(proxyBeanMethods = false)
public class TestFakesConfiguration {
    @Bean
    InMemoryMediator inMemoryMediator() {
        return new InMemoryMediator();
    }

    @Bean
    Mediator mediator(InMemoryMediator inMemoryMediator) {
        return inMemoryMediator;
    }

    @Bean
    InMemoryEventPublisher inMemoryEventPublisher() {
        return new InMemoryEventPublisher();
    }

    @Bean
    EventPublisher eventPublisher(InMemoryEventPublisher publisher) {
        return publisher;
    }

    @Bean
    InMemoryRequestIdProvider inMemoryRequestIdProvider() {
        return new InMemoryRequestIdProvider();
    }

    @Bean
    RequestIdProvider requestIdProvider(InMemoryRequestIdProvider provider) {
        return provider;
    }

    @Bean
    ManuallySetTimeProvider manuallySetTimeProvider() {
        return new ManuallySetTimeProvider();
    }

    @Bean
    TimeProvider timeProvider(ManuallySetTimeProvider provider) {
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
    PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}
