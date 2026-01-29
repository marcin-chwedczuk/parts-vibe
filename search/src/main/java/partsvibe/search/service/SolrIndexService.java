package partsvibe.search.service;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.stereotype.Service;
import java.io.IOException;
import org.apache.solr.client.solrj.util.ClientUtils;
import partsvibe.search.config.SolrProperties;
import partsvibe.search.web.CatalogSearchHit;
import partsvibe.search.web.CatalogSearchResult;

@Service
public class SolrIndexService {
  private static final int MAX_RESULTS = 10;
  private final SolrClient solrClient;
  private final SolrProperties solrProperties;

  public SolrIndexService(SolrClient solrClient, SolrProperties solrProperties) {
    this.solrClient = solrClient;
    this.solrProperties = solrProperties;
  }

  public String indexText(String text) {
    String id = UUID.randomUUID().toString();
    SolrInputDocument doc = new SolrInputDocument();
    doc.addField("id", id);
    doc.addField("content_txt", text);

    try {
      solrClient.add(solrProperties.core(), doc);
      solrClient.commit(solrProperties.core());
    } catch (SolrServerException | IOException e) {
      throw new IllegalStateException("Failed to index text into Solr.", e);
    }

    return id;
  }

  public CatalogSearchResult search(String queryText, int page, int pageSize) {
    if (queryText == null || queryText.isBlank()) {
      return new CatalogSearchResult(List.of(), 0, page, pageSize);
    }

    int maxPage = Math.max(0, (MAX_RESULTS - 1) / pageSize);
    int safePage = Math.max(0, Math.min(page, maxPage));
    int start = safePage * pageSize;
    if (start >= MAX_RESULTS) {
      return new CatalogSearchResult(List.of(), 0, safePage, pageSize);
    }

    SolrQuery query = new SolrQuery();
    query.setQuery("content_txt:" + ClientUtils.escapeQueryChars(queryText.trim()));
    query.setStart(start);
    query.setRows(pageSize);
    query.setFields("id", "content_txt");

    try {
      QueryResponse response = solrClient.query(solrProperties.core(), query);
      long totalFound = response.getResults().getNumFound();
      long cappedTotal = Math.min(totalFound, MAX_RESULTS);
      List<CatalogSearchHit> hits = new ArrayList<>();
      response.getResults().forEach(doc -> {
        String id = (String) doc.getFieldValue("id");
        Object contentField = doc.getFieldValue("content_txt");
        String content;
        if (contentField instanceof List<?> list) {
          content = list.stream()
              .map(Object::toString)
              .reduce((left, right) -> left + "\n" + right)
              .orElse("");
        } else {
          content = contentField == null ? "" : contentField.toString();
        }
        hits.add(new CatalogSearchHit(id, content));
      });
      return new CatalogSearchResult(hits, cappedTotal, safePage, pageSize);
    } catch (SolrServerException | IOException e) {
      throw new IllegalStateException("Failed to query Solr.", e);
    }
  }
}
