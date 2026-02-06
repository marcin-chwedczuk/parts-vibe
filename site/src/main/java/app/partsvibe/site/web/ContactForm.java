package app.partsvibe.site.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactForm {
    @NotBlank
    @Size(max = 64)
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String message;
}
