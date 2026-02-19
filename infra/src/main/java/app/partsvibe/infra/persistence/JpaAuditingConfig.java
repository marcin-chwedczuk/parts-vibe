package app.partsvibe.infra.persistence;

import app.partsvibe.shared.security.CurrentUserProvider;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    @Bean
    AuditorAware<String> auditorAware(CurrentUserProvider currentUserProvider) {
        return () -> currentUserProvider
                .currentUsername()
                .filter(name -> !name.isBlank())
                .or(() -> Optional.of("system"));
    }
}
