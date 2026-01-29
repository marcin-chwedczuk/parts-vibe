package partsvibe.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.solr")
public record SolrProperties(String baseUrl, String core) {
}
