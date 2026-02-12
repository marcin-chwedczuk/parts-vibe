package app.partsvibe.site.commands.contact;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.shared.email.EmailMessage;
import app.partsvibe.shared.email.EmailSender;
import app.partsvibe.site.domain.ContactMessage;
import app.partsvibe.site.repo.ContactMessageRepository;
import org.springframework.stereotype.Component;

@Component
class SubmitContactMessageCommandHandler
        extends BaseCommandHandler<SubmitContactMessageCommand, SubmitContactMessageCommandResult> {
    private final ContactMessageRepository contactMessageRepository;
    private final EmailSender emailSender;

    SubmitContactMessageCommandHandler(ContactMessageRepository contactMessageRepository, EmailSender emailSender) {
        this.contactMessageRepository = contactMessageRepository;
        this.emailSender = emailSender;
    }

    @Override
    protected SubmitContactMessageCommandResult doHandle(SubmitContactMessageCommand command) {
        var saved = contactMessageRepository.save(new ContactMessage(command.name(), command.email(), command.message()));
        sendConfirmationEmail(command.name(), command.email(), command.message());
        return new SubmitContactMessageCommandResult(saved.getId());
    }

    private void sendConfirmationEmail(String name, String email, String message) {
        var subject = "We received your message";
        var body = "Hello " + name + ",\n\n"
                + "Thanks for reaching out. We received your message:\n\n"
                + message + "\n\n"
                + "Regards,\n"
                + "parts-vibe";
        emailSender.send(
                EmailMessage.builder().to(email).subject(subject).bodyText(body).build());
    }
}
