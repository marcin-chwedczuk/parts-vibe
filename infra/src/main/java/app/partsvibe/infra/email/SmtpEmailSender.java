package app.partsvibe.infra.email;

import app.partsvibe.shared.email.EmailMessage;
import app.partsvibe.shared.email.EmailSender;
import app.partsvibe.shared.email.EmailSenderException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailSender implements EmailSender {
    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);
    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final Counter sendAttemptsCounter;
    private final Counter sendSuccessCounter;
    private final Counter sendErrorsCounter;

    public SmtpEmailSender(
            JavaMailSender mailSender, @Value("${app.mail.from}") String fromAddress, MeterRegistry meterRegistry) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.sendAttemptsCounter = meterRegistry.counter("app.email.send.attempts");
        this.sendSuccessCounter = meterRegistry.counter("app.email.send.success");
        this.sendErrorsCounter = meterRegistry.counter("app.email.send.errors");
    }

    @Override
    public void send(EmailMessage message) {
        sendAttemptsCounter.increment();
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
            sendSuccessCounter.increment();
            log.info("Email sent successfully. recipientsCount={}", recipientsCount(message));
        } catch (MessagingException ex) {
            sendErrorsCounter.increment();
            log.error("Email send failed. recipientsCount={}", recipientsCount(message), ex);
            throw new EmailSenderException("Failed to send email.", ex);
        }
    }

    private String[] toArray(List<String> recipients) {
        return recipients.toArray(String[]::new);
    }

    private static int recipientsCount(EmailMessage message) {
        return message.to().size() + message.cc().size() + message.bcc().size();
    }
}
