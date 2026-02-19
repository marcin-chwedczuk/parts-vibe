package app.partsvibe.users.testsupport;

import app.partsvibe.testsupport.CommonJpaTestConfiguration;
import app.partsvibe.users.domain.Role;
import app.partsvibe.users.domain.User;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.repo.UserRepository;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableJpaRepositories(basePackageClasses = {UserRepository.class, RoleRepository.class})
@EntityScan(basePackageClasses = {User.class, Role.class})
@ComponentScan(
        basePackages = {"app.partsvibe.users.queries", "app.partsvibe.users.commands"},
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*Handler"))
@Import({CommonJpaTestConfiguration.class, UsersHandlersTestConfiguration.class})
public class UsersModuleTestApplication {}
