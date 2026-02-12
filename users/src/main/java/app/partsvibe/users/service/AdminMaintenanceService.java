package app.partsvibe.users.service;

import java.util.UUID;

public interface AdminMaintenanceService {
    UUID triggerRetentionCleanup(String requestId);
}
