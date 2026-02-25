package app.partsvibe.users.test.web;

import app.partsvibe.testsupport.web.WebTestBaseApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

@SpringBootConfiguration
@ComponentScan(basePackages = "app.partsvibe.users.web")
@Import(WebTestBaseApplication.class)
public class UsersWebTestApplication {
    @Bean
    MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasenames("classpath:users/messages", "classpath:messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
