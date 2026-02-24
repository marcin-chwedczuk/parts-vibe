package app.partsvibe.users.web;

import app.partsvibe.shared.cqrs.Mediator;
import app.partsvibe.shared.utils.StringUtils;
import app.partsvibe.users.commands.password.RequestPasswordResetCommand;
import app.partsvibe.users.commands.password.ResetPasswordWithTokenCommand;
import app.partsvibe.users.errors.InvalidOrExpiredCredentialTokenException;
import app.partsvibe.users.errors.WeakPasswordException;
import app.partsvibe.users.queries.password.IsPasswordResetTokenActiveQuery;
import app.partsvibe.users.web.form.ForgotPasswordForm;
import app.partsvibe.users.web.form.PasswordResetForm;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final Mediator mediator;

    public AuthController(Mediator mediator) {
        this.mediator = mediator;
    }

    @GetMapping("/login")
    public String login() {
        log.info("Login page requested");
        return "login";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ForgotPasswordForm());
        }
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String requestPasswordReset(
            @Valid @ModelAttribute("form") ForgotPasswordForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.form", bindingResult);
            redirectAttributes.addFlashAttribute("form", form);
            return "redirect:/forgot-password";
        }

        mediator.executeCommand(new RequestPasswordResetCommand(form.getEmail()));
        redirectAttributes.addFlashAttribute("passwordResetRequested", true);
        return "redirect:/forgot-password";
    }

    @GetMapping("/password-reset")
    public String passwordReset(@RequestParam(name = "token", required = false) String token, Model model) {
        if (!StringUtils.hasText(token)) {
            model.addAttribute("tokenInvalid", true);
            return "password-reset";
        }

        if (!mediator.executeQuery(new IsPasswordResetTokenActiveQuery(token))) {
            model.addAttribute("tokenInvalid", true);
            return "password-reset";
        }

        if (!model.containsAttribute("form")) {
            PasswordResetForm form = new PasswordResetForm();
            form.setToken(token);
            model.addAttribute("form", form);
        }
        return "password-reset";
    }

    @PostMapping("/password-reset")
    public String completePasswordReset(
            @Valid @ModelAttribute("form") PasswordResetForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return renderResetPageOrInvalid(form, model);
        }

        try {
            mediator.executeCommand(
                    new ResetPasswordWithTokenCommand(form.getToken(), form.getPassword(), form.getRepeatedPassword()));
            redirectAttributes.addFlashAttribute("passwordResetDone", true);
            return "redirect:/login";
        } catch (WeakPasswordException ex) {
            bindingResult.rejectValue("password", "auth.reset.validation.password.weak");
            return renderResetPageOrInvalid(form, model);
        } catch (InvalidOrExpiredCredentialTokenException ex) {
            model.addAttribute("tokenInvalid", true);
            return "password-reset";
        }
    }

    private String renderResetPageOrInvalid(PasswordResetForm form, Model model) {
        if (!StringUtils.hasText(form.getToken())
                || !mediator.executeQuery(new IsPasswordResetTokenActiveQuery(form.getToken()))) {
            model.addAttribute("tokenInvalid", true);
            return "password-reset";
        }
        model.addAttribute("form", form);
        return "password-reset";
    }
}
