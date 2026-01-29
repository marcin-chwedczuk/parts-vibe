package vibe.webapp.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import vibe.webapp.service.SolrIndexService;
import vibe.webapp.web.CatalogIndexForm;

@Controller
@RequestMapping("/catalog")
public class CatalogIndexController {
  private final SolrIndexService solrIndexService;

  public CatalogIndexController(SolrIndexService solrIndexService) {
    this.solrIndexService = solrIndexService;
  }

  @GetMapping("/index")
  public String form(Model model) {
    if (!model.containsAttribute("catalogIndexForm")) {
      model.addAttribute("catalogIndexForm", new CatalogIndexForm());
    }
    return "catalog-index";
  }

  @PostMapping("/index")
  public String submit(@Valid @ModelAttribute("catalogIndexForm") CatalogIndexForm form,
                       BindingResult bindingResult,
                       Model model) {
    if (bindingResult.hasErrors()) {
      return "catalog-index";
    }

    String documentId = solrIndexService.indexText(form.getText());
    model.addAttribute("documentId", documentId);
    return "catalog-index-success";
  }
}
