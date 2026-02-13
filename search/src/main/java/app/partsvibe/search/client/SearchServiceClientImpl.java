package app.partsvibe.search.client;

import app.partsvibe.search.api.CatalogSearchResult;
import app.partsvibe.search.api.SearchServiceClient;
import app.partsvibe.search.commands.IndexTextCommand;
import app.partsvibe.search.queries.SearchCatalogQuery;
import app.partsvibe.shared.cqrs.Mediator;
import org.springframework.stereotype.Service;

@Service
public class SearchServiceClientImpl implements SearchServiceClient {
    private final Mediator mediator;

    public SearchServiceClientImpl(Mediator mediator) {
        this.mediator = mediator;
    }

    @Override
    public String indexText(String text) {
        return mediator.executeCommand(new IndexTextCommand(text)).documentId();
    }

    @Override
    public CatalogSearchResult search(String queryText, int page, int pageSize) {
        return mediator.executeQuery(new SearchCatalogQuery(queryText, page, pageSize));
    }
}
