package app.partsvibe.users.web;

import app.partsvibe.shared.cqrs.Mediator;
import app.partsvibe.shared.error.ApplicationException;
import app.partsvibe.shared.security.CurrentUserProvider;
import app.partsvibe.storage.api.StorageClient;
import app.partsvibe.storage.api.StorageObjectType;
import app.partsvibe.storage.api.StorageUploadRequest;
import app.partsvibe.uicomponents.breadcrumbs.BreadcrumbItemData;
import app.partsvibe.uicomponents.breadcrumbs.BreadcrumbsData;
import app.partsvibe.users.commands.profile.UpdateCurrentUserAvatarCommand;
import app.partsvibe.users.commands.profile.UpdateCurrentUserProfileCommand;
import app.partsvibe.users.models.UserProfileModel;
import app.partsvibe.users.queries.profile.GetCurrentUserProfileQuery;
import app.partsvibe.users.web.form.ProfileForm;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private final Mediator mediator;
    private final CurrentUserProvider currentUserProvider;
    private final StorageClient storageClient;
    private final MessageSource messageSource;
    private final long avatarMaxBytes;

    public ProfileController(
            Mediator mediator,
            CurrentUserProvider currentUserProvider,
            StorageClient storageClient,
            MessageSource messageSource,
            @Value("${app.storage.limits.avatar-bytes:102400}") long avatarMaxBytes) {
        this.mediator = mediator;
        this.currentUserProvider = currentUserProvider;
        this.storageClient = storageClient;
        this.messageSource = messageSource;
        this.avatarMaxBytes = avatarMaxBytes;
    }

    @GetMapping
    public String viewProfile(Model model, Locale locale) {
        String username = currentUsername();
        UserProfileModel userProfile = mediator.executeQuery(new GetCurrentUserProfileQuery(username));

        if (!model.containsAttribute("form")) {
            ProfileForm form = new ProfileForm();
            form.setBio(defaultString(userProfile.bio()));
            form.setWebsite(defaultString(userProfile.website()));
            model.addAttribute("form", form);
        }

        model.addAttribute("profile", userProfile);
        model.addAttribute("avatarUrl", avatarUrl(userProfile.avatarId()));
        model.addAttribute("breadcrumbs", breadcrumbs(locale));

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
            UserProfileModel profile = mediator.executeQuery(new GetCurrentUserProfileQuery(currentUsername()));
            model.addAttribute("profile", profile);
            model.addAttribute("avatarUrl", avatarUrl(profile.avatarId()));
            model.addAttribute("breadcrumbs", breadcrumbs(locale));
            return "profile/view";
        }

        mediator.executeCommand(
                new UpdateCurrentUserProfileCommand(currentUsername(), form.getBio(), form.getWebsite()));
        redirectAttributes.addFlashAttribute("profileMessageCode", "profile.info.updated");
        redirectAttributes.addFlashAttribute("profileMessageLevel", "alert-success");
        return "redirect:/profile";
    }

    @PostMapping("/avatar")
    public String updateAvatar(MultipartFile avatarFile, RedirectAttributes redirectAttributes) {
        String username = currentUsername();
        if (avatarFile == null || avatarFile.isEmpty()) {
            log.warn("Profile avatar upload rejected because file is empty. username={}", username);
            redirectAttributes.addFlashAttribute("profileMessageCode", "profile.avatar.empty");
            redirectAttributes.addFlashAttribute("profileMessageLevel", "alert-danger");
            return "redirect:/profile";
        }
        if (avatarFile.getSize() > avatarMaxBytes) {
            log.warn(
                    "Profile avatar upload rejected because size limit exceeded. username={}, sizeBytes={}, limitBytes={}",
                    username,
                    avatarFile.getSize(),
                    avatarMaxBytes);
            redirectAttributes.addFlashAttribute("profileMessageCode", "profile.avatar.tooLarge");
            redirectAttributes.addFlashAttribute("profileMessageArgs", new Object[] {avatarMaxBytes / 1024});
            redirectAttributes.addFlashAttribute("profileMessageLevel", "alert-danger");
            return "redirect:/profile";
        }

        String originalFilename =
                avatarFile.getOriginalFilename() == null ? "avatar" : avatarFile.getOriginalFilename();

        try {
            log.info(
                    "Profile avatar upload started. username={}, originalFilename={}, contentType={}, sizeBytes={}",
                    username,
                    originalFilename,
                    avatarFile.getContentType(),
                    avatarFile.getSize());

            var uploadResult = storageClient.upload(new StorageUploadRequest(
                    StorageObjectType.USER_AVATAR_IMAGE, originalFilename, avatarFile.getBytes()));

            var updateResult =
                    mediator.executeCommand(new UpdateCurrentUserAvatarCommand(username, uploadResult.fileId()));
            if (updateResult.previousAvatarId() != null
                    && !updateResult.previousAvatarId().equals(uploadResult.fileId())) {
                try {
                    storageClient.delete(updateResult.previousAvatarId());
                    log.info(
                            "Profile previous avatar deleted. username={}, previousAvatarId={}",
                            username,
                            updateResult.previousAvatarId());
                } catch (ApplicationException ignored) {
                    // The new avatar is already persisted; old avatar cleanup is best-effort.
                    log.warn(
                            "Failed to delete previous avatar (best-effort). username={}, previousAvatarId={}",
                            username,
                            updateResult.previousAvatarId());
                }
            }

            log.info("Profile avatar upload completed. username={}, avatarId={}", username, uploadResult.fileId());
            redirectAttributes.addFlashAttribute("profileMessageCode", "profile.avatar.updated");
            redirectAttributes.addFlashAttribute("profileMessageLevel", "alert-success");
            return "redirect:/profile";
        } catch (IOException | ApplicationException ex) {
            log.error(
                    "Profile avatar upload failed. username={}, originalFilename={}, contentType={}, sizeBytes={}",
                    username,
                    originalFilename,
                    avatarFile.getContentType(),
                    avatarFile.getSize(),
                    ex);
            redirectAttributes.addFlashAttribute("profileMessageCode", "profile.avatar.failed");
            redirectAttributes.addFlashAttribute("profileMessageLevel", "alert-danger");
            return "redirect:/profile";
        }
    }

    private String currentUsername() {
        return currentUserProvider
                .currentUsername()
                .orElseThrow(() -> new IllegalStateException("Authenticated username is required."));
    }

    private static String defaultString(String value) {
        return value == null ? "" : value;
    }

    private static String avatarUrl(UUID avatarId) {
        return avatarId == null ? PLACEHOLDER_IMAGE_URL : "/storage/files/" + avatarId + "/thumbnail/128";
    }

    private BreadcrumbsData breadcrumbs(Locale locale) {
        return new BreadcrumbsData(
                List.of(new BreadcrumbItemData(messageSource.getMessage("nav.profile", null, locale), null, true)));
    }
}
