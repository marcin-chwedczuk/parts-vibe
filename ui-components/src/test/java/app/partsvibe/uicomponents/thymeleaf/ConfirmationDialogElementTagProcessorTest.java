package app.partsvibe.uicomponents.thymeleaf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.partsvibe.uicomponents.confirmation.ConfirmationDialogModel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateInputException;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class ConfirmationDialogElementTagProcessorTest {
    private TemplateEngine templateEngine;

    @BeforeEach
    void setup() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        templateEngine.addDialect(new UiComponentsDialect());
    }

    @Test
    void shouldRenderDialogWithSlotContent() {
        Context context = new Context();
        context.setVariable(
                "dialog",
                new TestDialogData("delete-user-7", "/admin/users/7/do-delete", "POST", null, null, null, null));
        context.setVariable("username", "bob@example.com");

        String html = templateEngine.process("test/confirmation-host", context);
        Document document = Jsoup.parse(html);

        assertThat(document.select("div.modal.fade#delete-user-7")).hasSize(1);
        assertThat(document.select("div.modal-content form[action=/admin/users/7/do-delete][method=post]"))
                .hasSize(1);
        assertThat(document.select("div.modal-body strong").text()).isEqualTo("bob@example.com");
        assertThat(document.select("form input[name=_csrf][value=token]")).hasSize(1);
        assertThat(document.select("form button[type=submit].btn-danger").text())
                .isEqualTo("Delete");
    }

    @Test
    void shouldUseFallbackBodyTextWhenNoBodySlotProvided() {
        Context context = new Context();
        context.setVariable(
                "dialog",
                new TestDialogData(
                        "delete-user-8",
                        "/admin/users/8/do-delete",
                        "POST",
                        "Confirm delete",
                        "Fallback body text",
                        "Delete",
                        "Cancel"));

        String html = templateEngine.process("test/confirmation-host-default-body", context);
        Document document = Jsoup.parse(html);

        assertThat(document.select("div.modal-body").text()).contains("Fallback body text");
    }

    @Test
    void shouldFailWhenActionMethodIsNotPost() {
        Context context = new Context();
        context.setVariable(
                "dialog",
                new TestDialogData("delete-user-9", "/admin/users/9/do-delete", "GET", null, null, null, null));

        assertThatThrownBy(() -> templateEngine.process("test/confirmation-host-default-body", context))
                .isInstanceOf(TemplateInputException.class)
                .hasRootCauseInstanceOf(TemplateProcessingException.class)
                .rootCause()
                .hasMessageContaining("must be POST");
    }

    @Test
    void shouldFailWhenDataAttributeMissing() {
        Context context = new Context();

        assertThatThrownBy(() -> templateEngine.process("test/confirmation-host-missing-data", context))
                .isInstanceOf(TemplateInputException.class)
                .hasRootCauseInstanceOf(TemplateProcessingException.class)
                .rootCause()
                .hasMessageContaining("Missing required attribute 'app:data'");
    }

    private record TestDialogData(
            String dialogId,
            String actionUrl,
            String actionMethod,
            String titleText,
            String bodyText,
            String confirmText,
            String cancelText)
            implements ConfirmationDialogModel {}
}
