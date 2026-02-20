package app.partsvibe.uicomponents.thymeleaf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.partsvibe.uicomponents.pagination.PaginationLinkModel;
import app.partsvibe.uicomponents.pagination.PaginationModel;
import java.util.List;
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

class PaginationElementTagProcessorTest {
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
    void shouldRenderPaginationFromComponentTemplate() {
        Context context = new Context();
        context.setVariable(
                "pageInfo",
                new TestPaginationData(
                        "/admin/users?page=1",
                        "/admin/users?page=10",
                        List.of(new TestPageLink(1, "/admin/users?page=1"), new TestPageLink(2, "/admin/users?page=2")),
                        10,
                        2,
                        11,
                        20,
                        100));

        String html = templateEngine.process("test/pagination-host", context);
        Document document = Jsoup.parse(html);

        assertThat(document.select("ul.pagination.mb-0")).hasSize(1);
        assertThat(document.select("ul.pagination li.page-item a.page-link")).isNotEmpty();
        assertThat(document.select("a.page-link[href=/admin/users?page=2]")).hasSize(1);
        assertThat(document.select("app\\:pagination")).isEmpty();
    }

    @Test
    void shouldFailWhenDataAttributeMissing() {
        Context context = new Context();
        assertThatThrownBy(() -> templateEngine.process("test/pagination-host-missing-data", context))
                .isInstanceOf(TemplateInputException.class)
                .hasRootCauseInstanceOf(TemplateProcessingException.class)
                .rootCause()
                .hasMessageContaining("Missing required attribute 'app:data'");
    }

    @Test
    void shouldFailWhenDataAttributeResolvesToNull() {
        Context context = new Context();
        context.setVariable("pageInfo", null);

        assertThatThrownBy(() -> templateEngine.process("test/pagination-host", context))
                .isInstanceOf(TemplateInputException.class)
                .hasRootCauseInstanceOf(TemplateProcessingException.class)
                .rootCause()
                .hasMessageContaining("resolved to null");
    }

    @Test
    void shouldFailWhenDataHasWrongType() {
        Context context = new Context();
        context.setVariable("pageInfo", "bad-data");

        assertThatThrownBy(() -> templateEngine.process("test/pagination-host", context))
                .isInstanceOf(TemplateInputException.class)
                .hasRootCauseInstanceOf(TemplateProcessingException.class)
                .rootCause()
                .hasMessageContaining("must resolve to PaginationModel");
    }

    private record TestPaginationData(
            String firstUrl,
            String lastUrl,
            List<? extends PaginationLinkModel> pageLinks,
            int totalPages,
            int currentPage,
            int startRow,
            int endRow,
            long totalRows)
            implements PaginationModel {}

    private record TestPageLink(int pageNumber, String url) implements PaginationLinkModel {}
}
