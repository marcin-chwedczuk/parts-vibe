package vibe.webapp.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import vibe.webapp.domain.ContactMessage;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
}
