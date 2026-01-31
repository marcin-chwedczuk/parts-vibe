package app.partsvibe.catalog.web;

import app.partsvibe.search.api.CatalogSearchService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(CatalogIndexController.class);
    private final CatalogSearchService catalogSearchService;

    public CatalogIndexController(CatalogSearchService catalogSearchService) {
        this.catalogSearchService = catalogSearchService;
    }

    @GetMapping("/index")
    public String form(Model model) {
        log.info("Catalog index form requested");
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
            log.warn("Catalog index validation failed: errors={}", bindingResult.getErrorCount());
            return "catalog-index";
        }

        String documentId = catalogSearchService.indexText(form.getText());
        log.info(
                "Catalog text indexed: length={}, documentId={}", form.getText().length(), documentId);
        model.addAttribute("documentId", documentId);
        return "catalog-index-success";
    }
}
