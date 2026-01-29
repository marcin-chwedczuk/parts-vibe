package vibe.webapp.config;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SolrConfig {
  @Bean
  public SolrClient solrClient(SolrProperties solrProperties) {
    return new HttpSolrClient.Builder(solrProperties.baseUrl()).build();
  }
}
