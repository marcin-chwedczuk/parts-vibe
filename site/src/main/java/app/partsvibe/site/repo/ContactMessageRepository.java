package app.partsvibe.site.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import app.partsvibe.site.domain.ContactMessage;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
}
