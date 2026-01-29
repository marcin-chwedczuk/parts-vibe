package partsvibe.dataaccess.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import partsvibe.dataaccess.domain.ContactMessage;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
}
