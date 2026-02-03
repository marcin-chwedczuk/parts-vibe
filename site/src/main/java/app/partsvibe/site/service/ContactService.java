package app.partsvibe.site.service;

public interface ContactService {
    Long submitMessage(String name, String email, String message);
}
