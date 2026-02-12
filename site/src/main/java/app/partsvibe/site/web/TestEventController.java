package app.partsvibe.site.web;

import app.partsvibe.shared.cqrs.Mediator;
import app.partsvibe.shared.request.RequestIdProvider;
import app.partsvibe.site.commands.testevent.PublishTestEventCommand;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/test-event")
public class TestEventController {
    private static final Logger log = LoggerFactory.getLogger(TestEventController.class);

    private final Mediator mediator;
    private final RequestIdProvider requestIdProvider;

    public TestEventController(Mediator mediator, RequestIdProvider requestIdProvider) {
        this.mediator = mediator;
        this.requestIdProvider = requestIdProvider;
    }

    @GetMapping
    public String form(Model model) {
        if (!model.containsAttribute("testEventForm")) {
            model.addAttribute("testEventForm", new TestEventForm());
        }
        return "test-event";
    }

    @PostMapping
    public String submit(
            @Valid @ModelAttribute("testEventForm") TestEventForm testEventForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            log.warn("Test event form validation failed: errors={}", bindingResult.getErrorCount());
            return "test-event";
        }

        String requestId = requestIdProvider.currentOrElse("unknown");
        UUID eventId = mediator.executeCommand(new PublishTestEventCommand(requestId, testEventForm.getMessage()))
                .eventId();
        log.info("Test event published. eventId={}, requestId={}", eventId, requestId);

        redirectAttributes.addAttribute("submitted", "1");
        redirectAttributes.addAttribute("eventId", eventId);
        return "redirect:/test-event";
    }
}
