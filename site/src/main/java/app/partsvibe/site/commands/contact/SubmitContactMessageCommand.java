package app.partsvibe.site.commands.contact;

import app.partsvibe.shared.cqrs.Command;
import app.partsvibe.site.commands.contact.validation.NoForbiddenWord;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitContactMessageCommand(
        @NotBlank @Size(max = 64) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank
                @Size(max = 2000)
                @NoForbiddenWord(value = "foo", message = "Message cannot contain forbidden word: foo")
                String message)
        implements Command<SubmitContactMessageCommandResult> {}
