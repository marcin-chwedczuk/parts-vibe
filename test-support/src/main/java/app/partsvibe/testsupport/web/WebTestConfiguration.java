package app.partsvibe.testsupport.web;

import app.partsvibe.testsupport.fakes.TestFakesConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.RedirectView;

@TestConfiguration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
@Import(TestFakesConfiguration.class)
public class WebTestConfiguration {
    @Bean
    org.springframework.security.web.SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
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
    ViewResolver viewResolver() {
        return (viewName, locale) -> {
            // Keep tests decoupled from Thymeleaf templates that live outside this module.
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
