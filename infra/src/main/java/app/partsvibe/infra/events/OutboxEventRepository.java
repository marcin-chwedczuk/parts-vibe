package app.partsvibe.infra.events;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {
    Optional<OutboxEventEntity> findByEventId(UUID eventId);
}
