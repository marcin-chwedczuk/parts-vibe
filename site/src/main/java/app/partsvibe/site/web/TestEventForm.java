package app.partsvibe.site.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestEventForm {
    @NotBlank
    @Size(max = 500)
    private String message;
}
