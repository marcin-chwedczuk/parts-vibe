package app.partsvibe.catalog.web;

import app.partsvibe.catalog.queries.CatalogSearchQuery;
import app.partsvibe.search.api.CatalogSearchResult;
import app.partsvibe.shared.cqrs.Mediator;
import app.partsvibe.uicomponents.breadcrumbs.BreadcrumbItemData;
import app.partsvibe.uicomponents.breadcrumbs.BreadcrumbsData;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/catalog")
public class CatalogSearchController {
    private static final int PAGE_SIZE = 5;
    private static final Logger log = LoggerFactory.getLogger(CatalogSearchController.class);
    private final Mediator mediator;
    private final MessageSource messageSource;

    public CatalogSearchController(Mediator mediator, MessageSource messageSource) {
        this.mediator = mediator;
        this.messageSource = messageSource;
    }

    @GetMapping("/search")
    public String search(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model,
            Locale locale) {
        String safeQuery = query == null ? "" : query;
        log.info("Catalog search requested: queryLength={}, page={}", safeQuery.length(), page);
        CatalogSearchResult result = mediator.executeQuery(new CatalogSearchQuery(query, page, PAGE_SIZE));
        log.info(
                "Catalog search completed: total={}, page={}, pageSize={}",
                result.total(),
                result.page(),
                result.pageSize());
        model.addAttribute("query", safeQuery);
        model.addAttribute("result", result);
        model.addAttribute(
                "breadcrumbs",
                new BreadcrumbsData(List.of(
                        new BreadcrumbItemData(
                                messageSource.getMessage("nav.catalog.browse", null, locale), "/catalog", false),
                        new BreadcrumbItemData(
                                messageSource.getMessage("nav.catalog.search", null, locale), null, true))));
        return "catalog-search";
    }
}
