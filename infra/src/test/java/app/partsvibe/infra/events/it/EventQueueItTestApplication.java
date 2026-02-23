package app.partsvibe.infra.events.it;

import app.partsvibe.testsupport.it.CommonJpaTestConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@EnableAutoConfiguration
@ConfigurationPropertiesScan(basePackages = "app.partsvibe.infra.events")
@EntityScan(basePackages = "app.partsvibe.infra.events.jpa")
@ComponentScan(
        basePackages = {"app.partsvibe.infra.events", "app.partsvibe.infra.time"},
        excludeFilters =
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "app\\.partsvibe\\.infra\\.events\\.it\\..*"))
@Import({CommonJpaTestConfiguration.class, EventQueueItTestConfiguration.class})
public class EventQueueItTestApplication {}
