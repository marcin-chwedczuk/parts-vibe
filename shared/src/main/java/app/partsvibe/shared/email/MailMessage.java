package app.partsvibe.shared.email;

import java.util.List;
import lombok.Builder;
import lombok.Singular;

@Builder
public record MailMessage(
        @Singular("to") List<String> to,
        @Singular("cc") List<String> cc,
        @Singular("bcc") List<String> bcc,
        String subject,
        String bodyText,
        String bodyHtml) {}
