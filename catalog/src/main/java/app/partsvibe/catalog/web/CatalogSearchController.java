package app.partsvibe.catalog.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import app.partsvibe.search.api.CatalogSearchResult;
import app.partsvibe.search.api.CatalogSearchService;

@Controller
@RequestMapping("/catalog")
public class CatalogSearchController {
  private static final int PAGE_SIZE = 5;
  private final CatalogSearchService catalogSearchService;

  public CatalogSearchController(CatalogSearchService catalogSearchService) {
    this.catalogSearchService = catalogSearchService;
  }

  @GetMapping("/search")
  public String search(@RequestParam(name = "q", required = false) String query,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       Model model) {
    CatalogSearchResult result = catalogSearchService.search(query, page, PAGE_SIZE);
    model.addAttribute("query", query == null ? "" : query);
    model.addAttribute("result", result);
    return "catalog-search";
  }
}
