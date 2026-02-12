package app.partsvibe.users.service.impl;

import app.partsvibe.shared.events.admin.TriggerRetentionCleanupEvent;
import app.partsvibe.shared.events.publishing.EventPublisher;
import app.partsvibe.users.service.AdminMaintenanceService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminMaintenanceServiceImpl implements AdminMaintenanceService {
    private final EventPublisher eventPublisher;

    public AdminMaintenanceServiceImpl(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public UUID triggerRetentionCleanup(String requestId) {
        TriggerRetentionCleanupEvent event = TriggerRetentionCleanupEvent.create(requestId);
        eventPublisher.publish(event);
        return event.eventId();
    }
}
