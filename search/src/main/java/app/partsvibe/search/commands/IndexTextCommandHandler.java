package app.partsvibe.search.commands;

import app.partsvibe.search.solr.config.SolrProperties;
import app.partsvibe.shared.cqrs.BaseCommandHandler;
import java.io.IOException;
import java.util.UUID;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.stereotype.Component;

@Component
class IndexTextCommandHandler extends BaseCommandHandler<IndexTextCommand, IndexTextCommandResult> {
    private final SolrClient solrClient;
    private final SolrProperties solrProperties;

    IndexTextCommandHandler(SolrClient solrClient, SolrProperties solrProperties) {
        this.solrClient = solrClient;
        this.solrProperties = solrProperties;
    }

    @Override
    protected IndexTextCommandResult doHandle(IndexTextCommand command) {
        var id = UUID.randomUUID().toString();
        var doc = new SolrInputDocument();
        doc.addField("id", id);
        doc.addField("content_txt", command.text());

        try {
            solrClient.add(solrProperties.core(), doc);
            solrClient.commit(solrProperties.core());
            return new IndexTextCommandResult(id);
        } catch (SolrServerException | IOException e) {
            throw new IllegalStateException("Failed to index text into Solr.", e);
        }
    }
}
