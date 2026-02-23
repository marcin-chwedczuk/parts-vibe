package app.partsvibe.infra.events.it;

import app.partsvibe.infra.events.handling.EventHandlerRegistry;
import app.partsvibe.infra.events.handling.EventQueueConsumer;
import app.partsvibe.infra.events.handling.SpringEventHandlerRegistry;
import app.partsvibe.infra.events.it.support.QueueTestEventHandler;
import app.partsvibe.infra.events.it.support.QueueTestEventProbe;
import app.partsvibe.infra.events.serialization.EventJsonSerializer;
import app.partsvibe.infra.events.serialization.JacksonEventJsonSerializer;
import app.partsvibe.shared.request.RequestIdProvider;
import app.partsvibe.testsupport.fakes.InMemoryRequestIdProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@TestConfiguration(proxyBeanMethods = false)
@Import({
    EventQueueConsumer.class,
    SpringEventHandlerRegistry.class,
    JacksonEventJsonSerializer.class,
    QueueTestEventHandler.class,
    QueueTestEventProbe.class
})
public class EventQueueConsumerItTestConfiguration {
    @Bean
    InMemoryRequestIdProvider inMemoryRequestIdProvider() {
        return new InMemoryRequestIdProvider();
    }

    @Bean
    RequestIdProvider requestIdProvider(InMemoryRequestIdProvider provider) {
        return provider;
    }

    @Bean
    EventHandlerRegistry eventHandlerRegistry(SpringEventHandlerRegistry registry) {
        return registry;
    }

    @Bean
    EventJsonSerializer eventJsonSerializer(JacksonEventJsonSerializer serializer) {
        return serializer;
    }
}
