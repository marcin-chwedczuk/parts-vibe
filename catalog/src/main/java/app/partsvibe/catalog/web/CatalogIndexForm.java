package app.partsvibe.catalog.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CatalogIndexForm {
    @NotBlank
    @Size(max = 50000)
    private String text;
}
