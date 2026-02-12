package app.partsvibe.users.web;

import app.partsvibe.shared.request.RequestIdProvider;
import app.partsvibe.users.service.AdminMaintenanceService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    private final AdminMaintenanceService adminMaintenanceService;
    private final RequestIdProvider requestIdProvider;

    public AdminController(AdminMaintenanceService adminMaintenanceService, RequestIdProvider requestIdProvider) {
        this.adminMaintenanceService = adminMaintenanceService;
        this.requestIdProvider = requestIdProvider;
    }

    @GetMapping
    public String adminHome() {
        log.info("Admin dashboard requested");
        return "admin";
    }

    @PostMapping("/event-queue/retention-cleanup")
    public String triggerRetentionCleanup(RedirectAttributes redirectAttributes) {
        String requestId = requestIdProvider.currentOrElse("unknown");
        UUID eventId = adminMaintenanceService.triggerRetentionCleanup(requestId);
        log.info(
                "Retention cleanup trigger event published from admin page. requestId={}, eventId={}",
                requestId,
                eventId);
        redirectAttributes.addFlashAttribute("retentionCleanupTriggered", true);
        redirectAttributes.addFlashAttribute("retentionCleanupEventId", eventId);
        return "redirect:/admin";
    }
}
