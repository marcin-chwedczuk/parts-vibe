package app.partsvibe.storage.test.it;

import app.partsvibe.shared.antivirus.AntivirusScanner;
import app.partsvibe.storage.test.support.FakeAntivirusScanner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class StorageItTestConfiguration {
    @Bean
    FakeAntivirusScanner fakeAntivirusScanner() {
        return new FakeAntivirusScanner();
    }

    @Bean
    AntivirusScanner antivirusScanner(FakeAntivirusScanner scanner) {
        return scanner;
    }
}
