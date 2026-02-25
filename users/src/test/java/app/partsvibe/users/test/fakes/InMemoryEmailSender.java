package app.partsvibe.users.test.fakes;

import app.partsvibe.shared.email.EmailMessage;
import app.partsvibe.shared.email.EmailSender;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryEmailSender implements EmailSender {
    private final List<EmailMessage> sentEmails = new CopyOnWriteArrayList<>();

    @Override
    public void send(EmailMessage message) {
        sentEmails.add(message);
    }

    public List<EmailMessage> sentEmails() {
        return List.copyOf(sentEmails);
    }

    public void clear() {
        sentEmails.clear();
    }
}
