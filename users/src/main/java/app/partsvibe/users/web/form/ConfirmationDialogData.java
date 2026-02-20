package app.partsvibe.users.web.form;

import app.partsvibe.uicomponents.confirmation.ConfirmationDialogModel;

public record ConfirmationDialogData(
        String dialogId,
        String actionUrl,
        String actionMethod,
        String titleText,
        String bodyText,
        String confirmText,
        String cancelText)
        implements ConfirmationDialogModel {}
