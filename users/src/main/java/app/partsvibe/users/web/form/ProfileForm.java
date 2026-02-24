package app.partsvibe.users.web.form;

import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProfileForm implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Size(max = 1000, message = "{profile.validation.bio.max}")
    private String bio = "";

    @Size(max = 255, message = "{profile.validation.website.max}")
    private String website = "";
}
