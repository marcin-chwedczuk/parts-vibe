package app.partsvibe.site.web;

import app.partsvibe.shared.cqrs.Mediator;
import app.partsvibe.site.commands.contact.SubmitContactMessageCommand;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/contact")
public class ContactController {
    private static final Logger log = LoggerFactory.getLogger(ContactController.class);

    private final Mediator mediator;

    public ContactController(Mediator mediator) {
        this.mediator = mediator;
    }

    @GetMapping
    public String form(Model model) {
        log.info("Contact form requested");
        if (!model.containsAttribute("contactForm")) {
            model.addAttribute("contactForm", new ContactForm());
        }
        return "contact";
    }

    @PostMapping
    public String submit(@Valid @ModelAttribute("contactForm") ContactForm contactForm, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            log.warn("Contact form validation failed: errors={}", bindingResult.getErrorCount());
            return "contact";
        }

        var messageId = mediator.executeCommand(new SubmitContactMessageCommand(
                        contactForm.getName(), contactForm.getEmail(), contactForm.getMessage()))
                .messageId();
        log.info("Contact message saved: id={}", messageId);
        return "contact-success";
    }
}
