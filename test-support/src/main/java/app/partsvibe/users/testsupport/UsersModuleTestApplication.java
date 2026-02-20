package app.partsvibe.users.testsupport;

import app.partsvibe.testsupport.CommonJpaTestConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = "app.partsvibe.users.repo")
@EntityScan(basePackages = "app.partsvibe.users.domain")
@ComponentScan(
        basePackages = {"app.partsvibe.users.queries", "app.partsvibe.users.commands"},
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*Handler"))
@Import({CommonJpaTestConfiguration.class, UsersHandlersTestConfiguration.class})
public class UsersModuleTestApplication {}
