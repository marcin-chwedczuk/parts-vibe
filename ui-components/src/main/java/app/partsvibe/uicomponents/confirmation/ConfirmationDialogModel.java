package app.partsvibe.uicomponents.confirmation;

public interface ConfirmationDialogModel {
    String dialogId();

    String actionUrl();

    String actionMethod();

    String titleText();

    String bodyText();

    String confirmText();

    String cancelText();
}
