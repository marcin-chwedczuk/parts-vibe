package vibe.webapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vibe.webapp.service.SolrIndexService;
import vibe.webapp.web.CatalogSearchResult;

@Controller
@RequestMapping("/catalog")
public class CatalogSearchController {
  private static final int PAGE_SIZE = 5;
  private final SolrIndexService solrIndexService;

  public CatalogSearchController(SolrIndexService solrIndexService) {
    this.solrIndexService = solrIndexService;
  }

  @GetMapping("/search")
  public String search(@RequestParam(name = "q", required = false) String query,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       Model model) {
    CatalogSearchResult result = solrIndexService.search(query, page, PAGE_SIZE);
    model.addAttribute("query", query == null ? "" : query);
    model.addAttribute("result", result);
    return "catalog-search";
  }
}
