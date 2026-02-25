package app.partsvibe.users.test.it;

import app.partsvibe.storage.api.DeleteFileResult;
import app.partsvibe.storage.api.StorageClient;
import app.partsvibe.storage.api.StorageUploadRequest;
import app.partsvibe.storage.api.StorageUploadResult;
import app.partsvibe.testsupport.fakes.TestFakesConfiguration;
import app.partsvibe.testsupport.it.CommonJpaTestConfiguration;
import app.partsvibe.users.email.ThymeleafEmailTemplateRenderer;
import app.partsvibe.users.email.config.EmailTextTemplateEngineConfig;
import app.partsvibe.users.email.templates.InviteEmailTemplate;
import app.partsvibe.users.email.templates.PasswordResetEmailTemplate;
import app.partsvibe.users.security.tokens.CredentialTokenCodec;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = "app.partsvibe.users.repo")
@EntityScan(basePackages = "app.partsvibe.users.domain")
@ComponentScan(
        basePackages = {"app.partsvibe.users.queries", "app.partsvibe.users.commands"},
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*Handler"))
@Import({
    CommonJpaTestConfiguration.class,
    TestFakesConfiguration.class,
    EmailTextTemplateEngineConfig.class,
    ThymeleafEmailTemplateRenderer.class,
    InviteEmailTemplate.class,
    PasswordResetEmailTemplate.class,
    CredentialTokenCodec.class
})
public class UsersItTestApplication {
    @Bean
    @Qualifier("templateEngine")
    TemplateEngine htmlTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);

        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    @Bean
    StorageClient storageClient() {
        return new InMemoryStorageClient();
    }

    private static final class InMemoryStorageClient implements StorageClient {
        private final Set<UUID> storedIds = new HashSet<>();

        @Override
        public StorageUploadResult upload(StorageUploadRequest request) {
            UUID fileId = UUID.randomUUID();
            storedIds.add(fileId);
            return new StorageUploadResult(fileId);
        }

        @Override
        public DeleteFileResult delete(UUID fileId) {
            return storedIds.remove(fileId) ? DeleteFileResult.deleted() : DeleteFileResult.notFound();
        }
    }
}
