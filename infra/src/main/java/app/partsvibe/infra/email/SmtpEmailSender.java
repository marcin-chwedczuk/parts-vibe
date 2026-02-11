package app.partsvibe.infra.email;

import app.partsvibe.shared.email.EmailMessage;
import app.partsvibe.shared.email.EmailSender;
import app.partsvibe.shared.email.EmailSenderException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailSender implements EmailSender {
    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpEmailSender(JavaMailSender mailSender, @Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void send(EmailMessage message) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        boolean multipart = message.bodyHtml() != null && !message.bodyHtml().isBlank();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, multipart, "UTF-8");
            helper.setTo(toArray(message.to()));
            if (!message.cc().isEmpty()) {
                helper.setCc(toArray(message.cc()));
            }
            if (!message.bcc().isEmpty()) {
                helper.setBcc(toArray(message.bcc()));
            }
            if (fromAddress != null && !fromAddress.isBlank()) {
                helper.setFrom(fromAddress);
            }
            if (message.subject() != null) {
                helper.setSubject(message.subject());
            }
            if (message.bodyHtml() != null && !message.bodyHtml().isBlank()) {
                helper.setText(message.bodyText(), message.bodyHtml());
            } else {
                helper.setText(message.bodyText(), false);
            }
            mailSender.send(mimeMessage);
        } catch (MessagingException ex) {
            throw new EmailSenderException("Failed to send email.", ex);
        }
    }

    private String[] toArray(List<String> recipients) {
        return recipients.toArray(String[]::new);
    }
}
