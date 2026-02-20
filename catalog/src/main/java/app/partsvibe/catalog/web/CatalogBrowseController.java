package app.partsvibe.catalog.web;

import app.partsvibe.catalog.errors.CategoryNotFoundException;
import app.partsvibe.catalog.queries.ListCategoriesQuery;
import app.partsvibe.catalog.queries.ListCategoryPartsQuery;
import app.partsvibe.shared.cqrs.Mediator;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

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

    public CatalogBrowseController(Mediator mediator) {
        this.mediator = mediator;
    }

    @GetMapping
    public String categories(Model model) {
        List<ListCategoriesQuery.CategoryCard> categories = mediator.executeQuery(new ListCategoriesQuery());
        model.addAttribute("categories", categories);
        model.addAttribute("tagBadgeClasses", TAG_BADGE_CLASSES);
        return "catalog-categories";
    }

    @GetMapping("/categories/{categoryId}")
    public String categoryView(
            @PathVariable("categoryId") Long categoryId,
            @RequestParam(name = "q", required = false) String query,
            Model model) {
        String safeQuery = query == null ? "" : query.trim();
        try {
            ListCategoryPartsQuery.CategoryParts result =
                    mediator.executeQuery(new ListCategoryPartsQuery(categoryId, safeQuery));

            model.addAttribute("result", result);
            model.addAttribute("query", safeQuery);
            model.addAttribute("tagBadgeClasses", TAG_BADGE_CLASSES);
        } catch (CategoryNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }

        return "catalog-category-view";
    }

    @GetMapping("/categories/{categoryId}/parts/create")
    public String createPartForm(@PathVariable("categoryId") Long categoryId, Model model) {
        model.addAttribute("categoryId", categoryId);
        return "catalog-part-create";
    }
}
