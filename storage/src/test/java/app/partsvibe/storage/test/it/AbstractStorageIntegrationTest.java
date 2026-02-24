package app.partsvibe.storage.test.it;

import app.partsvibe.storage.test.support.FakeAntivirusScanner;
import app.partsvibe.testsupport.it.AbstractModuleIntegrationTest;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(classes = StorageItTestApplication.class)
public abstract class AbstractStorageIntegrationTest extends AbstractModuleIntegrationTest {
    private static final String STORAGE_ROOT = Path.of(
                    System.getProperty("java.io.tmpdir"),
                    "parts-vibe-storage-it",
                    UUID.randomUUID().toString())
            .toString();

    @Autowired
    protected FakeAntivirusScanner fakeAntivirusScanner;

    @DynamicPropertySource
    static void configureStorageProperties(DynamicPropertyRegistry registry) {
        registry.add("app.storage.root-dir", () -> STORAGE_ROOT);
    }

    @BeforeEach
    void resetStorageTestContext() {
        fakeAntivirusScanner.reset();
    }
}
