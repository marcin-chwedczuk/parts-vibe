package app.partsvibe.catalog.commands;

import app.partsvibe.search.api.SearchServiceClient;
import app.partsvibe.shared.cqrs.BaseCommandHandler;
import org.springframework.stereotype.Component;

@Component
class IndexCatalogTextCommandHandler
        extends BaseCommandHandler<IndexCatalogTextCommand, IndexCatalogTextCommandResult> {
    private final SearchServiceClient searchServiceClient;

    IndexCatalogTextCommandHandler(SearchServiceClient searchServiceClient) {
        this.searchServiceClient = searchServiceClient;
    }

    @Override
    protected IndexCatalogTextCommandResult doHandle(IndexCatalogTextCommand command) {
        var documentId = searchServiceClient.indexText(command.text());
        return new IndexCatalogTextCommandResult(documentId);
    }
}
