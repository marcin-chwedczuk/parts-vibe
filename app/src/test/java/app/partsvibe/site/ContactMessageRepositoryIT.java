package app.partsvibe.site;

import static org.assertj.core.api.Assertions.assertThat;

import app.partsvibe.site.domain.ContactMessage;
import app.partsvibe.site.repo.ContactMessageRepository;
import app.partsvibe.testsupport.it.AbstractModuleIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ContactMessageRepositoryIT extends AbstractModuleIntegrationTest {
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
