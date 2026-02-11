package app.partsvibe.site.service;

import java.util.UUID;

public interface TestEventService {
    UUID publishTestEvent(String requestId, String message);
}
