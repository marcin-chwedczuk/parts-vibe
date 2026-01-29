package partsvibe.webapp;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;
import partsvibe.webapp.web.ContactForm;

class ContactFormTest {
  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void validatesHappyPath() {
    ContactForm form = new ContactForm();
    form.setName("Jane Doe");
    form.setEmail("jane@example.com");
    form.setMessage("Hello there!");

    Set<ConstraintViolation<ContactForm>> violations = validator.validate(form);
    assertThat(violations).isEmpty();
  }

  @Test
  void rejectsLongName() {
    ContactForm form = new ContactForm();
    form.setName("a".repeat(65));
    form.setEmail("jane@example.com");
    form.setMessage("Hello there!");

    Set<ConstraintViolation<ContactForm>> violations = validator.validate(form);
    assertThat(violations).isNotEmpty();
  }

  @Test
  void rejectsBlankMessage() {
    ContactForm form = new ContactForm();
    form.setName("Jane Doe");
    form.setEmail("jane@example.com");
    form.setMessage("  ");

    Set<ConstraintViolation<ContactForm>> violations = validator.validate(form);
    assertThat(violations).isNotEmpty();
  }
}
