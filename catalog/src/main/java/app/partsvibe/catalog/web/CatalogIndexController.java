package app.partsvibe.catalog.web;

import app.partsvibe.search.api.CatalogSearchService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/catalog")
public class CatalogIndexController {
    private final CatalogSearchService catalogSearchService;

    public CatalogIndexController(CatalogSearchService catalogSearchService) {
        this.catalogSearchService = catalogSearchService;
    }

    @GetMapping("/index")
    public String form(Model model) {
        if (!model.containsAttribute("catalogIndexForm")) {
            model.addAttribute("catalogIndexForm", new CatalogIndexForm());
        }
        return "catalog-index";
    }

    @PostMapping("/index")
    public String submit(
            @Valid @ModelAttribute("catalogIndexForm") CatalogIndexForm form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return "catalog-index";
        }

        String documentId = catalogSearchService.indexText(form.getText());
        model.addAttribute("documentId", documentId);
        return "catalog-index-success";
    }
}
