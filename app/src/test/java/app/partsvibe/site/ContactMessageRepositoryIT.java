package app.partsvibe.site;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import app.partsvibe.site.domain.ContactMessage;
import app.partsvibe.site.repo.ContactMessageRepository;

@Testcontainers
@SpringBootTest
class ContactMessageRepositoryIT {
  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
  }

  @Autowired
  private ContactMessageRepository contactMessageRepository;

  @Test
  void savesAndLoads() {
    ContactMessage message = new ContactMessage("Jane", "jane@example.com", "Hello!");
    ContactMessage saved = contactMessageRepository.save(message);

    assertThat(saved.getId()).isNotNull();
    assertThat(contactMessageRepository.findById(saved.getId())).isPresent();
  }
}
