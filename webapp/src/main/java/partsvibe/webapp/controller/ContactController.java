package partsvibe.webapp.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import partsvibe.dataaccess.domain.ContactMessage;
import partsvibe.dataaccess.repo.ContactMessageRepository;
import partsvibe.webapp.web.ContactForm;

@Controller
@RequestMapping("/contact")
public class ContactController {
  private final ContactMessageRepository contactMessageRepository;

  public ContactController(ContactMessageRepository contactMessageRepository) {
    this.contactMessageRepository = contactMessageRepository;
  }

  @GetMapping
  public String form(Model model) {
    if (!model.containsAttribute("contactForm")) {
      model.addAttribute("contactForm", new ContactForm());
    }
    return "contact";
  }

  @PostMapping
  public String submit(@Valid @ModelAttribute("contactForm") ContactForm contactForm,
                       BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return "contact";
    }

    ContactMessage message = new ContactMessage(
        contactForm.getName(),
        contactForm.getEmail(),
        contactForm.getMessage()
    );
    contactMessageRepository.save(message);
    return "contact-success";
  }
}
