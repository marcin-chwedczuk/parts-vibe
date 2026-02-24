package app.partsvibe.users.web;

import app.partsvibe.shared.cqrs.Mediator;
import app.partsvibe.shared.security.CurrentUserProvider;
import app.partsvibe.storage.api.StorageException;
import app.partsvibe.storage.api.StorageFileSizeLimitExceededException;
import app.partsvibe.uicomponents.breadcrumbs.BreadcrumbItemData;
import app.partsvibe.uicomponents.breadcrumbs.BreadcrumbsData;
import app.partsvibe.users.commands.profile.ChangeCurrentUserAvatarCommand;
import app.partsvibe.users.commands.profile.UpdateCurrentUserProfileCommand;
import app.partsvibe.users.commands.profile.password.ChangeCurrentUserPasswordCommand;
import app.partsvibe.users.errors.InvalidCurrentPasswordException;
import app.partsvibe.users.errors.WeakPasswordException;
import app.partsvibe.users.models.UserProfileModel;
import app.partsvibe.users.queries.profile.GetCurrentUserProfileQuery;
import app.partsvibe.users.web.form.ProfileForm;
import app.partsvibe.users.web.form.ProfilePasswordForm;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@PreAuthorize("isAuthenticated()")
public class ProfileController {
    private static final String PLACEHOLDER_IMAGE_URL = "/resources/images/placeholder.png";
    private static final String PROFILE_MESSAGE_TARGET_AVATAR = "avatar";
    private static final String PROFILE_MESSAGE_TARGET_INFO = "info";
    private static final String PROFILE_MESSAGE_TARGET_PASSWORD = "password";
    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private final Mediator mediator;
    private final CurrentUserProvider currentUserProvider;
    private final MessageSource messageSource;

    public ProfileController(Mediator mediator, CurrentUserProvider currentUserProvider, MessageSource messageSource) {
        this.mediator = mediator;
        this.currentUserProvider = currentUserProvider;
        this.messageSource = messageSource;
    }

    @GetMapping
    public String viewProfile(Model model, Locale locale) {
        populateProfileViewModel(model, locale);

        return "profile/view";
    }

    @PostMapping("/info")
    public String updateInfo(
            @Valid @ModelAttribute("form") ProfileForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Locale locale,
            Model model) {
        if (bindingResult.hasErrors()) {
            populateProfileViewModel(model, locale);
            return "profile/view";
        }

        mediator.executeCommand(new UpdateCurrentUserProfileCommand(currentUserId(), form.getBio(), form.getWebsite()));
        redirectAttributes.addFlashAttribute("profileMessageCode", "profile.info.updated");
        redirectAttributes.addFlashAttribute("profileMessageLevel", "alert-success");
        redirectAttributes.addFlashAttribute("profileMessageTarget", PROFILE_MESSAGE_TARGET_INFO);
        return "redirect:/profile";
    }

    @PostMapping("/avatar")
    public String updateAvatar(MultipartFile avatarFile, RedirectAttributes redirectAttributes) {
        Long userId = currentUserId();
        if (avatarFile == null || avatarFile.isEmpty()) {
            log.warn("Profile avatar upload rejected because file is empty. userId={}", userId);
            redirectAttributes.addFlashAttribute("profileMessageCode", "profile.avatar.empty");
            redirectAttributes.addFlashAttribute("profileMessageLevel", "alert-danger");
            redirectAttributes.addFlashAttribute("profileMessageTarget", PROFILE_MESSAGE_TARGET_AVATAR);
            return "redirect:/profile";
        }

        String originalFilename =
                avatarFile.getOriginalFilename() == null ? "avatar" : avatarFile.getOriginalFilename();

        try {
            log.info(
                    "Profile avatar upload started. userId={}, originalFilename={}, contentType={}, sizeBytes={}",
                    userId,
                    originalFilename,
                    avatarFile.getContentType(),
                    avatarFile.getSize());

            mediator.executeCommand(
                    new ChangeCurrentUserAvatarCommand(userId, originalFilename, readFileBytes(avatarFile)));
            log.info("Profile avatar upload completed. userId={}", userId);
            redirectAttributes.addFlashAttribute("profileMessageCode", "profile.avatar.updated");
            redirectAttributes.addFlashAttribute("profileMessageLevel", "alert-success");
            redirectAttributes.addFlashAttribute("profileMessageTarget", PROFILE_MESSAGE_TARGET_AVATAR);
            return "redirect:/profile";
        } catch (StorageFileSizeLimitExceededException ex) {
            log.warn(
                    "Profile avatar upload failed due to size limit. userId={}, originalFilename={}, contentType={}, sizeBytes={}, limitBytes={}",
                    userId,
                    originalFilename,
                    avatarFile.getContentType(),
                    avatarFile.getSize(),
                    ex.maxAllowedBytes());
            redirectAttributes.addFlashAttribute("profileMessageCode", "profile.avatar.tooLarge");
            redirectAttributes.addFlashAttribute("profileMessageArgs", new Object[] {ex.maxAllowedBytes() / 1024});
            redirectAttributes.addFlashAttribute("profileMessageLevel", "alert-danger");
            redirectAttributes.addFlashAttribute("profileMessageTarget", PROFILE_MESSAGE_TARGET_AVATAR);
            return "redirect:/profile";
        } catch (IOException ex) {
            log.error(
                    "Profile avatar upload failed while reading multipart bytes. userId={}, originalFilename={}, contentType={}, sizeBytes={}",
                    userId,
                    originalFilename,
                    avatarFile.getContentType(),
                    avatarFile.getSize(),
                    ex);
            redirectAttributes.addFlashAttribute("profileMessageCode", "profile.avatar.failed");
            redirectAttributes.addFlashAttribute("profileMessageLevel", "alert-danger");
            redirectAttributes.addFlashAttribute("profileMessageTarget", PROFILE_MESSAGE_TARGET_AVATAR);
            return "redirect:/profile";
        } catch (StorageException ex) {
            log.error(
                    "Profile avatar upload failed. userId={}, originalFilename={}, contentType={}, sizeBytes={}",
                    userId,
                    originalFilename,
                    avatarFile.getContentType(),
                    avatarFile.getSize(),
                    ex);
            redirectAttributes.addFlashAttribute("profileMessageCode", "profile.avatar.failed");
            redirectAttributes.addFlashAttribute("profileMessageLevel", "alert-danger");
            redirectAttributes.addFlashAttribute("profileMessageTarget", PROFILE_MESSAGE_TARGET_AVATAR);
            return "redirect:/profile";
        }
    }

    @GetMapping("/password")
    public String viewChangePassword() {
        return "redirect:/profile";
    }

    @PostMapping("/password")
    public String changePassword(
            @Valid @ModelAttribute("passwordForm") ProfilePasswordForm passwordForm,
            BindingResult bindingResult,
            Model model,
            Locale locale,
            RedirectAttributes redirectAttributes) {
        if (!passwordForm.getNewPassword().equals(passwordForm.getRepeatedNewPassword())) {
            bindingResult.rejectValue("repeatedNewPassword", "profile.password.validation.repeat.mismatch");
        }

        if (bindingResult.hasErrors()) {
            populateProfileViewModel(model, locale);
            return "profile/view";
        }

        try {
            mediator.executeCommand(new ChangeCurrentUserPasswordCommand(
                    currentUserId(),
                    passwordForm.getCurrentPassword(),
                    passwordForm.getNewPassword(),
                    passwordForm.getRepeatedNewPassword()));
            redirectAttributes.addFlashAttribute("profileMessageCode", "profile.password.updated");
            redirectAttributes.addFlashAttribute("profileMessageLevel", "alert-success");
            redirectAttributes.addFlashAttribute("profileMessageTarget", PROFILE_MESSAGE_TARGET_PASSWORD);
            return "redirect:/profile";
        } catch (InvalidCurrentPasswordException ex) {
            bindingResult.rejectValue("currentPassword", "profile.password.validation.current.invalid");
        } catch (WeakPasswordException ex) {
            bindingResult.rejectValue("newPassword", "profile.password.validation.new.weak");
        }

        populateProfileViewModel(model, locale);
        return "profile/view";
    }

    private Long currentUserId() {
        return currentUserProvider
                .currentUserId()
                .orElseThrow(() -> new IllegalStateException("Authenticated userId is required."));
    }

    private static String defaultString(String value) {
        return value == null ? "" : value;
    }

    private static String avatarUrl(UUID avatarId) {
        return avatarId == null ? PLACEHOLDER_IMAGE_URL : "/storage/files/" + avatarId + "/thumbnail/128";
    }

    private byte[] readFileBytes(MultipartFile avatarFile) throws IOException {
        return avatarFile.getBytes();
    }

    private BreadcrumbsData breadcrumbs(Locale locale) {
        return new BreadcrumbsData(
                List.of(new BreadcrumbItemData(messageSource.getMessage("nav.profile", null, locale), null, true)));
    }

    private void populateProfileViewModel(Model model, Locale locale) {
        Long userId = currentUserId();
        UserProfileModel userProfile = mediator.executeQuery(new GetCurrentUserProfileQuery(userId));

        if (!model.containsAttribute("form")) {
            ProfileForm form = new ProfileForm();
            form.setBio(defaultString(userProfile.bio()));
            form.setWebsite(defaultString(userProfile.website()));
            model.addAttribute("form", form);
        }
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new ProfilePasswordForm());
        }

        model.addAttribute("profile", userProfile);
        model.addAttribute("showProfileEmail", true);
        model.addAttribute("avatarUrl", avatarUrl(userProfile.avatarId()));
        model.addAttribute("breadcrumbs", breadcrumbs(locale));
    }
}
