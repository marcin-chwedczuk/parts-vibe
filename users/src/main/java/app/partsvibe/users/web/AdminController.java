package app.partsvibe.users.web;

import app.partsvibe.shared.cqrs.Mediator;
import app.partsvibe.users.commands.admin.TriggerRetentionCleanupCommand;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final Mediator mediator;

    public AdminController(Mediator mediator) {
        this.mediator = mediator;
    }

    @GetMapping
    public String adminHome() {
        log.info("Admin dashboard requested");
        return "redirect:/admin/users";
    }

    @GetMapping("/maintenance")
    public String maintenanceHome() {
        log.info("Admin maintenance page requested");
        return "admin";
    }

    @PostMapping("/event-queue/retention-cleanup")
    public String triggerRetentionCleanup(RedirectAttributes redirectAttributes) {
        UUID eventId =
                mediator.executeCommand(new TriggerRetentionCleanupCommand()).eventId();

        log.info("Retention cleanup trigger event published from admin page. eventId={}", eventId);
        redirectAttributes.addFlashAttribute("retentionCleanupTriggered", true);
        redirectAttributes.addFlashAttribute("retentionCleanupEventId", eventId);

        return "redirect:/admin/maintenance";
    }
}
