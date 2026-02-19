package app.partsvibe.users.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.partsvibe.shared.cqrs.Mediator;
import app.partsvibe.shared.cqrs.PageResult;
import app.partsvibe.users.commands.usermanagement.CreateUserCommand;
import app.partsvibe.users.commands.usermanagement.DeleteUserCommand;
import app.partsvibe.users.commands.usermanagement.DeleteUserCommandResult;
import app.partsvibe.users.commands.usermanagement.UpdateUserCommand;
import app.partsvibe.users.models.UserDetailsModel;
import app.partsvibe.users.queries.usermanagement.SearchUsersQuery;
import app.partsvibe.users.queries.usermanagement.UserByIdQuery;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.RedirectView;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = UsersControllerSecurityIT.TestApplication.class)
class UsersControllerSecurityIT {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private Mediator mediator;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMocks() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    void roleUserCannotAccessAdminUsersEndpoints() throws Exception {
        mockMvc.perform(get("/admin/users")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/users/create")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/users/1")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/users/1/edit")).andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/users/create").with(csrf()).param("username", "john@example.com"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/users/1/edit").with(csrf()).param("username", "john@example.com"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/users/1/do-delete").with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/users/1/do-lock").with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/users/1/do-unlock").with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void roleAdminCanAccessAdminUsersEndpoints() throws Exception {
        mockMvc.perform(get("/admin/users")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/users/create")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/users/1")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/users/1/edit")).andExpect(status().isOk());

        mockMvc.perform(post("/admin/users/create").with(csrf()).param("username", "john@example.com"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/admin/users/1/edit").with(csrf()).param("username", "john@example.com"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/admin/users/1/do-delete").with(csrf())).andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/admin/users/1/do-lock").with(csrf())).andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/admin/users/1/do-unlock").with(csrf())).andExpect(status().is3xxRedirection());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    @ComponentScan(basePackageClasses = {UsersController.class, UserWebMapper.class})
    static class TestApplication {
        @Bean
        Mediator mediator() {
            return new FakeMediator();
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http.authorizeHttpRequests(auth -> auth.requestMatchers("/admin/**")
                            .hasRole("ADMIN")
                            .anyRequest()
                            .authenticated())
                    .formLogin(form -> form.disable())
                    .httpBasic(basic -> basic.disable())
                    .build();
        }

        @Bean
        UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("admin").password("admin").roles("ADMIN").build(),
                    User.withUsername("user").password("user").roles("USER").build());
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return NoOpPasswordEncoder.getInstance();
        }

        @Bean
        LocalValidatorFactoryBean validator() {
            return new LocalValidatorFactoryBean();
        }

        @Bean
        ViewResolver viewResolver() {
            return (viewName, locale) -> {
                if (viewName.startsWith("redirect:")) {
                    return new RedirectView(viewName.substring("redirect:".length()));
                }
                View view = (model, request, response) -> {
                    response.setStatus(200);
                };
                return view;
            };
        }
    }

    static final class FakeMediator implements Mediator {
        @Override
        @SuppressWarnings("unchecked")
        public <R, C extends app.partsvibe.shared.cqrs.Command<R>> R executeCommand(C command) {
            if (command instanceof CreateUserCommand) {
                return (R) new UserDetailsModel(2L, "new@example.com", true, List.of("ROLE_USER"));
            }
            if (command instanceof UpdateUserCommand) {
                return (R) new UserDetailsModel(1L, "bob@example.com", true, List.of("ROLE_USER"));
            }
            if (command instanceof DeleteUserCommand) {
                return (R) new DeleteUserCommandResult("bob@example.com");
            }
            throw new UnsupportedOperationException(
                    "Unexpected command type: " + command.getClass().getName());
        }

        @Override
        @SuppressWarnings("unchecked")
        public <R, Q extends app.partsvibe.shared.cqrs.Query<R>> R executeQuery(Q query) {
            if (query instanceof SearchUsersQuery) {
                return (R) new PageResult<>(List.of(), 0, 0, 1, 10);
            }
            if (query instanceof UserByIdQuery) {
                return (R) new UserDetailsModel(1L, "bob@example.com", true, List.of("ROLE_USER"));
            }
            throw new UnsupportedOperationException(
                    "Unexpected query type: " + query.getClass().getName());
        }
    }
}
