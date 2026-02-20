package app.partsvibe.uicomponents.thymeleaf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UiComponentsDialectConfiguration {
    @Bean
    UiComponentsDialect uiComponentsDialect() {
        return new UiComponentsDialect();
    }
}
