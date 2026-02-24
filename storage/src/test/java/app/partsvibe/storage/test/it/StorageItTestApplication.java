package app.partsvibe.storage.test.it;

import app.partsvibe.storage.config.StorageModuleConfig;
import app.partsvibe.testsupport.fakes.TestFakesConfiguration;
import app.partsvibe.testsupport.it.CommonJpaTestConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = "app.partsvibe.storage.repo")
@EntityScan(basePackages = "app.partsvibe.storage.domain")
@ComponentScan(
        basePackages = {
            "app.partsvibe.storage.commands",
            "app.partsvibe.storage.queries",
            "app.partsvibe.storage.events.handling",
            "app.partsvibe.storage.service"
        },
        useDefaultFilters = false,
        includeFilters =
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = ".*(Handler|Service|Storage|Detector|Rules|Resolver)$"))
@Import({
    StorageModuleConfig.class,
    CommonJpaTestConfiguration.class,
    TestFakesConfiguration.class,
    StorageItTestConfiguration.class
})
public class StorageItTestApplication {}
