package app.partsvibe.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class RoutingDataSourceTest {
    private static final String READ_KEY = "read";
    private static final String WRITE_KEY = "write";

    private RoutingDataSource routingDataSource;

    @BeforeEach
    void setUp() {
        routingDataSource = new RoutingDataSource(READ_KEY, WRITE_KEY);
    }

    @AfterEach
    void resetTransactionState() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    void usesWriteKeyWhenNoTransaction() {
        assertThat(routingDataSource.determineCurrentLookupKey()).isEqualTo("write");
    }

    @Test
    void alternatesBetweenReadAndWriteForReadOnlyTransactions() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);

        assertThat(routingDataSource.determineCurrentLookupKey()).isEqualTo(READ_KEY);
        assertThat(routingDataSource.determineCurrentLookupKey()).isEqualTo(WRITE_KEY);
        assertThat(routingDataSource.determineCurrentLookupKey()).isEqualTo(READ_KEY);
    }
}
