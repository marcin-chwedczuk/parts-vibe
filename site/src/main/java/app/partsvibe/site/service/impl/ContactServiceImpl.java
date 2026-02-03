package app.partsvibe.site.service.impl;

import app.partsvibe.site.domain.ContactMessage;
import app.partsvibe.site.repo.ContactMessageRepository;
import app.partsvibe.site.service.ContactService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactServiceImpl implements ContactService {
    private final ContactMessageRepository contactMessageRepository;

    public ContactServiceImpl(ContactMessageRepository contactMessageRepository) {
        this.contactMessageRepository = contactMessageRepository;
    }

    @Override
    @Transactional
    public Long submitMessage(String name, String email, String message) {
        ContactMessage saved = contactMessageRepository.save(new ContactMessage(name, email, message));
        return saved.getId();
    }
}
