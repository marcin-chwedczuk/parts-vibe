package app.partsvibe.site.service.impl;

import app.partsvibe.shared.email.EmailMessage;
import app.partsvibe.shared.email.EmailSender;
import app.partsvibe.site.domain.ContactMessage;
import app.partsvibe.site.repo.ContactMessageRepository;
import app.partsvibe.site.service.ContactService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactServiceImpl implements ContactService {
    private final ContactMessageRepository contactMessageRepository;
    private final EmailSender emailSender;

    public ContactServiceImpl(ContactMessageRepository contactMessageRepository, EmailSender emailSender) {
        this.contactMessageRepository = contactMessageRepository;
        this.emailSender = emailSender;
    }

    @Override
    @Transactional
    public Long submitMessage(String name, String email, String message) {
        ContactMessage saved = contactMessageRepository.save(new ContactMessage(name, email, message));
        sendConfirmationEmail(name, email, message);
        return saved.getId();
    }

    private void sendConfirmationEmail(String name, String email, String message) {
        String subject = "We received your message";
        String body = "Hello " + name + ",\n\n"
                + "Thanks for reaching out. We received your message:\n\n"
                + message + "\n\n"
                + "Regards,\n"
                + "parts-vibe";
        emailSender.send(
                EmailMessage.builder().to(email).subject(subject).bodyText(body).build());
    }
}
