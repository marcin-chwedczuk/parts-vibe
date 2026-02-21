package app.partsvibe.catalog.web;

import app.partsvibe.catalog.errors.CategoryNotFoundException;
import app.partsvibe.catalog.errors.PartNotFoundException;
import app.partsvibe.catalog.queries.ListCategoriesQuery;
import app.partsvibe.catalog.queries.ListCategoryPartsQuery;
import app.partsvibe.catalog.queries.PartByIdQuery;
import app.partsvibe.shared.cqrs.Mediator;
import app.partsvibe.uicomponents.breadcrumbs.BreadcrumbItemData;
import app.partsvibe.uicomponents.breadcrumbs.BreadcrumbsData;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequestMapping("/catalog")
public class CatalogBrowseController {
    private static final Map<String, String> TAG_BADGE_CLASSES = Map.ofEntries(
            Map.entry("SLATE", "text-bg-secondary"),
            Map.entry("GRAY", "text-bg-secondary"),
            Map.entry("ZINC", "text-bg-secondary"),
            Map.entry("RED", "text-bg-danger"),
            Map.entry("ORANGE", "text-bg-warning"),
            Map.entry("AMBER", "text-bg-warning"),
            Map.entry("YELLOW", "text-bg-warning"),
            Map.entry("LIME", "text-bg-success"),
            Map.entry("GREEN", "text-bg-success"),
            Map.entry("EMERALD", "text-bg-success"),
            Map.entry("TEAL", "text-bg-info"),
            Map.entry("CYAN", "text-bg-info"),
            Map.entry("SKY", "text-bg-info"),
            Map.entry("BLUE", "text-bg-primary"),
            Map.entry("INDIGO", "text-bg-primary"),
            Map.entry("PINK", "text-bg-primary"));

    private final Mediator mediator;
    private final MessageSource messageSource;

    public CatalogBrowseController(Mediator mediator, MessageSource messageSource) {
        this.mediator = mediator;
        this.messageSource = messageSource;
    }

    @GetMapping
    public String categories(Model model, Locale locale) {
        List<ListCategoriesQuery.CategoryCard> categories = mediator.executeQuery(new ListCategoriesQuery());
        model.addAttribute("categories", categories);
        model.addAttribute("tagBadgeClasses", TAG_BADGE_CLASSES);
        model.addAttribute(
                "breadcrumbs",
                new BreadcrumbsData(List.of(new BreadcrumbItemData(
                        messageSource.getMessage("nav.catalog.browse", null, locale), null, true))));
        return "catalog-categories";
    }

    @GetMapping("/categories/{categoryId}")
    public String categoryView(
            @PathVariable("categoryId") Long categoryId,
            @RequestParam(name = "q", required = false) String query,
            Model model,
            Locale locale) {
        String safeQuery = query == null ? "" : query.trim();
        try {
            ListCategoryPartsQuery.CategoryParts result =
                    mediator.executeQuery(new ListCategoryPartsQuery(categoryId, safeQuery));

            model.addAttribute("result", result);
            model.addAttribute("query", safeQuery);
            model.addAttribute("tagBadgeClasses", TAG_BADGE_CLASSES);
            model.addAttribute(
                    "breadcrumbs",
                    new BreadcrumbsData(List.of(
                            new BreadcrumbItemData(
                                    messageSource.getMessage("nav.catalog.browse", null, locale), "/catalog", false),
                            new BreadcrumbItemData(result.categoryName(), null, true))));
        } catch (CategoryNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }

        return "catalog-category-view";
    }

    @GetMapping("/categories/{categoryId}/parts/create")
    public String createPartForm(
            @PathVariable("categoryId") Long categoryId,
            @RequestParam(name = "q", required = false) String categoryQuery,
            Model model,
            Locale locale) {
        String safeCategoryQuery = categoryQuery == null ? "" : categoryQuery.trim();
        String categoryUrl = buildCategoryUrl(categoryId, safeCategoryQuery);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("categoryUrl", categoryUrl);
        model.addAttribute(
                "breadcrumbs",
                new BreadcrumbsData(List.of(
                        new BreadcrumbItemData(
                                messageSource.getMessage("nav.catalog.browse", null, locale), "/catalog", false),
                        new BreadcrumbItemData(
                                messageSource.getMessage("catalog.category.title", null, locale), categoryUrl, false),
                        new BreadcrumbItemData(
                                messageSource.getMessage("catalog.part.create.heading", null, locale), null, true))));
        return "catalog-part-create";
    }

    @GetMapping("/parts/{partId}")
    public String partView(
            @PathVariable("partId") Long partId,
            @RequestParam(name = "categoryQ", required = false) String categoryQuery,
            Model model,
            Locale locale) {
        String safeCategoryQuery = categoryQuery == null ? "" : categoryQuery.trim();
        try {
            PartByIdQuery.PartDetails result = mediator.executeQuery(new PartByIdQuery(partId));
            String categoryUrl = buildCategoryUrl(result.categoryId(), safeCategoryQuery);

            model.addAttribute("result", result);
            model.addAttribute("tagBadgeClasses", TAG_BADGE_CLASSES);
            model.addAttribute("categoryUrl", categoryUrl);
            model.addAttribute(
                    "breadcrumbs",
                    new BreadcrumbsData(List.of(
                            new BreadcrumbItemData(
                                    messageSource.getMessage("nav.catalog.browse", null, locale), "/catalog", false),
                            new BreadcrumbItemData(result.categoryName(), categoryUrl, false),
                            new BreadcrumbItemData(result.name(), null, true))));
        } catch (PartNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }

        return "catalog-part-view";
    }

    private String buildCategoryUrl(Long categoryId, String categoryQuery) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/catalog/categories/{id}");
        if (categoryQuery != null && !categoryQuery.isBlank()) {
            builder.queryParam("q", categoryQuery);
        }
        return builder.buildAndExpand(categoryId).toUriString();
    }
}
