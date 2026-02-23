package app.partsvibe.infra.events.it;

import app.partsvibe.infra.events.handling.EventQueueConsumer;
import app.partsvibe.infra.events.handling.EventQueueDispatcher;
import app.partsvibe.infra.events.it.support.QueueTestEvent;
import app.partsvibe.infra.events.it.support.QueueTestEventProbe;
import app.partsvibe.infra.events.jpa.EventQueueEntry;
import app.partsvibe.infra.events.jpa.EventQueueEntryStatus;
import app.partsvibe.infra.events.jpa.EventQueueRepository;
import app.partsvibe.shared.events.publishing.EventPublisher;
import app.partsvibe.testsupport.fakes.InMemoryCurrentUserProvider;
import app.partsvibe.testsupport.fakes.InMemoryRequestIdProvider;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public abstract class AbstractEventQueueDatabaseIntegrationTest extends AbstractEventQueueIntegrationTest {
    @Autowired
    protected EventPublisher eventPublisher;

    @Autowired
    protected EventQueueRepository eventQueueRepository;

    @Autowired
    protected EventQueueConsumer eventQueueConsumer;

    @Autowired
    protected EventQueueDispatcher eventQueueDispatcher;

    @Autowired
    protected QueueTestEventProbe probe;

    @Autowired
    protected EntityManager entityManager;

    @Autowired
    protected InMemoryRequestIdProvider requestIdProvider;

    @Autowired
    protected InMemoryCurrentUserProvider currentUserProvider;

    @Autowired
    private PlatformTransactionManager transactionManager;

    protected TransactionTemplate tx;

    @BeforeEach
    void setUpBase() {
        tx = new TransactionTemplate(transactionManager);
        inTx(() -> {
            entityManager.createQuery("DELETE FROM EventQueueEntry").executeUpdate();
            return null;
        });
        probe.reset();
        requestIdProvider.clear();
        currentUserProvider.clear();
    }

    protected void publish(QueueTestEvent event) {
        inTx(() -> {
            eventPublisher.publish(event);
            return null;
        });
    }

    protected EventQueueEntry entryByEventId(UUID eventId) {
        return inTx(() -> {
            entityManager.clear();
            return entityManager
                    .createQuery("SELECT e FROM EventQueueEntry e WHERE e.eventId = :eventId", EventQueueEntry.class)
                    .setParameter("eventId", eventId)
                    .getSingleResult();
        });
    }

    protected long doneCount() {
        return inTx(() -> entityManager
                .createQuery("SELECT COUNT(e.id) FROM EventQueueEntry e WHERE e.status = :status", Long.class)
                .setParameter("status", EventQueueEntryStatus.DONE)
                .getSingleResult());
    }

    protected <T> T inTx(java.util.function.Supplier<T> supplier) {
        return tx.execute(status -> supplier.get());
    }
}
