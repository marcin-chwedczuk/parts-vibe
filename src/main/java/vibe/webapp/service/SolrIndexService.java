package vibe.webapp.service;

import java.util.UUID;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.stereotype.Service;
import java.io.IOException;
import vibe.webapp.config.SolrProperties;

@Service
public class SolrIndexService {
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
}
