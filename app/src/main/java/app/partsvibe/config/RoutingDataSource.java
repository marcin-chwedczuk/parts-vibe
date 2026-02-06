package app.partsvibe.config;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

final class RoutingDataSource extends AbstractRoutingDataSource {
    private final AtomicInteger readOnlyCounter = new AtomicInteger();
    private final String readKey;
    private final String writeKey;

    RoutingDataSource(String readKey, String writeKey) {
        this.readKey = readKey;
        this.writeKey = writeKey;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            int pick = Math.floorMod(readOnlyCounter.getAndIncrement(), 2);
            return pick == 0 ? readKey : writeKey;
        }
        return writeKey;
    }
}
