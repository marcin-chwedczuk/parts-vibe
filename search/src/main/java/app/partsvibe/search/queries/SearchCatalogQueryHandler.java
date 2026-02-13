package app.partsvibe.search.queries;

import app.partsvibe.search.api.CatalogSearchHit;
import app.partsvibe.search.api.CatalogSearchResult;
import app.partsvibe.search.solr.config.SolrProperties;
import app.partsvibe.shared.cqrs.BaseQueryHandler;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.springframework.stereotype.Component;

@Component
class SearchCatalogQueryHandler extends BaseQueryHandler<SearchCatalogQuery, CatalogSearchResult> {
    private static final int MAX_RESULTS = 10;

    private final SolrClient solrClient;
    private final SolrProperties solrProperties;

    SearchCatalogQueryHandler(SolrClient solrClient, SolrProperties solrProperties) {
        this.solrClient = solrClient;
        this.solrProperties = solrProperties;
    }

    @Override
    protected CatalogSearchResult doHandle(SearchCatalogQuery queryRequest) {
        if (queryRequest.queryText() == null || queryRequest.queryText().isBlank()) {
            return new CatalogSearchResult(List.of(), 0, queryRequest.page(), queryRequest.pageSize());
        }

        int maxPage = Math.max(0, (MAX_RESULTS - 1) / queryRequest.pageSize());
        int safePage = Math.max(0, Math.min(queryRequest.page(), maxPage));
        int start = safePage * queryRequest.pageSize();
        if (start >= MAX_RESULTS) {
            return new CatalogSearchResult(List.of(), 0, safePage, queryRequest.pageSize());
        }

        var query = new SolrQuery();
        query.setQuery("content_txt:"
                + ClientUtils.escapeQueryChars(queryRequest.queryText().trim()));
        query.setStart(start);
        query.setRows(queryRequest.pageSize());
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
            return new CatalogSearchResult(hits, cappedTotal, safePage, queryRequest.pageSize());
        } catch (SolrServerException | IOException e) {
            throw new IllegalStateException("Failed to query Solr.", e);
        }
    }
}
