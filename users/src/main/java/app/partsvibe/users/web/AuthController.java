package app.partsvibe.users.web;

import app.partsvibe.shared.cqrs.Mediator;
import app.partsvibe.shared.utils.StringUtils;
import app.partsvibe.users.commands.invite.FinalizeUserInviteCommand;
import app.partsvibe.users.commands.password.RequestPasswordResetCommand;
import app.partsvibe.users.commands.password.ResetPasswordWithTokenCommand;
import app.partsvibe.users.errors.InvalidOrExpiredCredentialTokenException;
import app.partsvibe.users.errors.PasswordsDoNotMatchException;
import app.partsvibe.users.errors.WeakPasswordException;
import app.partsvibe.users.queries.password.ResolveInviteTokenContextQuery;
import app.partsvibe.users.queries.password.ResolvePasswordResetTokenContextQuery;
import app.partsvibe.users.web.form.ForgotPasswordForm;
import app.partsvibe.users.web.form.PasswordResetForm;
import jakarta.validation.Valid;
import java.util.Optional;
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
        return renderPasswordSetupPage(token, model, PasswordSetupMode.PASSWORD_RESET);
    }

    @GetMapping("/invite")
    public String invitePasswordSetup(@RequestParam(name = "token", required = false) String token, Model model) {
        return renderPasswordSetupPage(token, model, PasswordSetupMode.INVITE);
    }

    @PostMapping("/password-reset")
    public String completePasswordReset(
            @Valid @ModelAttribute("form") PasswordResetForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        return completePasswordSetup(form, bindingResult, model, redirectAttributes, PasswordSetupMode.PASSWORD_RESET);
    }

    @PostMapping("/invite")
    public String completeInvitePasswordSetup(
            @Valid @ModelAttribute("form") PasswordResetForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        return completePasswordSetup(form, bindingResult, model, redirectAttributes, PasswordSetupMode.INVITE);
    }

    private String renderPasswordSetupPage(String token, Model model, PasswordSetupMode requestedMode) {
        if (!StringUtils.hasText(token)) {
            model.addAttribute("tokenInvalid", true);
            applyPasswordSetupPageModel(model, requestedMode, null);
            return "password-reset";
        }

        String setupUsername = null;
        if (requestedMode == PasswordSetupMode.INVITE) {
            Optional<ResolveInviteTokenContextQuery.TokenContext> inviteContext =
                    mediator.executeQuery(new ResolveInviteTokenContextQuery(token));
            if (inviteContext.isEmpty()) {
                model.addAttribute("tokenInvalid", true);
                applyPasswordSetupPageModel(model, requestedMode, null);
                return "password-reset";
            }
            var context = inviteContext.get();
            setupUsername = context.username();
            if (context.mode() == ResolveInviteTokenContextQuery.InviteTokenMode.ALREADY_REGISTERED) {
                model.addAttribute("tokenAlreadyRegistered", true);
                applyPasswordSetupPageModel(model, requestedMode, setupUsername);
                return "password-reset";
            }
        } else {
            Optional<ResolvePasswordResetTokenContextQuery.TokenContext> resetContext =
                    mediator.executeQuery(new ResolvePasswordResetTokenContextQuery(token));
            if (resetContext.isEmpty()) {
                model.addAttribute("tokenInvalid", true);
                applyPasswordSetupPageModel(model, requestedMode, null);
                return "password-reset";
            }
            setupUsername = resetContext.get().username();
        }

        if (!model.containsAttribute("form")) {
            PasswordResetForm form = new PasswordResetForm();
            form.setToken(token);
            model.addAttribute("form", form);
        }
        applyPasswordSetupPageModel(model, requestedMode, setupUsername);
        return "password-reset";
    }

    private String completePasswordSetup(
            PasswordResetForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            PasswordSetupMode requestedMode) {
        if (bindingResult.hasErrors()) {
            return renderSetupPageOrInvalid(form, model, requestedMode);
        }

        try {
            if (requestedMode == PasswordSetupMode.INVITE) {
                mediator.executeCommand(FinalizeUserInviteCommand.builder()
                        .token(form.getToken())
                        .password(form.getPassword())
                        .repeatedPassword(form.getRepeatedPassword())
                        .build());
            } else {
                mediator.executeCommand(ResetPasswordWithTokenCommand.builder()
                        .token(form.getToken())
                        .password(form.getPassword())
                        .repeatedPassword(form.getRepeatedPassword())
                        .build());
            }
            redirectAttributes.addFlashAttribute("passwordResetDone", true);
            return "redirect:/login";
        } catch (PasswordsDoNotMatchException ex) {
            bindingResult.rejectValue("repeatedPassword", "auth.reset.validation.repeat.mismatch");
            return renderSetupPageOrInvalid(form, model, requestedMode);
        } catch (WeakPasswordException ex) {
            bindingResult.rejectValue("password", "auth.reset.validation.password.weak");
            return renderSetupPageOrInvalid(form, model, requestedMode);
        } catch (InvalidOrExpiredCredentialTokenException ex) {
            model.addAttribute("tokenInvalid", true);
            applyPasswordSetupPageModel(model, requestedMode, null);
            return "password-reset";
        }
    }

    private String renderSetupPageOrInvalid(PasswordResetForm form, Model model, PasswordSetupMode requestedMode) {
        if (!StringUtils.hasText(form.getToken())) {
            model.addAttribute("tokenInvalid", true);
            applyPasswordSetupPageModel(model, requestedMode, null);
            return "password-reset";
        }

        String setupUsername = null;
        if (requestedMode == PasswordSetupMode.INVITE) {
            Optional<ResolveInviteTokenContextQuery.TokenContext> inviteContext =
                    mediator.executeQuery(new ResolveInviteTokenContextQuery(form.getToken()));
            if (inviteContext.isEmpty()) {
                model.addAttribute("tokenInvalid", true);
                applyPasswordSetupPageModel(model, requestedMode, null);
                return "password-reset";
            }
            var context = inviteContext.get();
            setupUsername = context.username();
            if (context.mode() == ResolveInviteTokenContextQuery.InviteTokenMode.ALREADY_REGISTERED) {
                model.addAttribute("tokenAlreadyRegistered", true);
                applyPasswordSetupPageModel(model, requestedMode, setupUsername);
                return "password-reset";
            }
        } else {
            Optional<ResolvePasswordResetTokenContextQuery.TokenContext> resetContext =
                    mediator.executeQuery(new ResolvePasswordResetTokenContextQuery(form.getToken()));
            if (resetContext.isEmpty()) {
                model.addAttribute("tokenInvalid", true);
                applyPasswordSetupPageModel(model, requestedMode, null);
                return "password-reset";
            }
            setupUsername = resetContext.get().username();
        }

        applyPasswordSetupPageModel(model, requestedMode, setupUsername);
        model.addAttribute("form", form);
        return "password-reset";
    }

    private void applyPasswordSetupPageModel(Model model, PasswordSetupMode mode, String setupUsername) {
        boolean inviteFlow = mode == PasswordSetupMode.INVITE;
        model.addAttribute("inviteFlow", inviteFlow);
        model.addAttribute("setupPageTitle", inviteFlow ? "auth.invite.setup.title" : "auth.reset.title");
        model.addAttribute("setupPageHeading", inviteFlow ? "auth.invite.setup.heading" : "auth.reset.heading");
        model.addAttribute("setupUsername", setupUsername);
        model.addAttribute("setupActionPath", inviteFlow ? "/invite" : "/password-reset");
        model.addAttribute(
                "setupInvalidMessageKey", inviteFlow ? "auth.invite.tokenInvalid" : "auth.reset.tokenInvalid");
    }

    private enum PasswordSetupMode {
        PASSWORD_RESET,
        INVITE
    }
}
