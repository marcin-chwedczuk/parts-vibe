package app.partsvibe.infra.events.it;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@Import(EventQueueConsumerItTestConfiguration.class)
public class EventQueueConsumerItTestApplication {}
