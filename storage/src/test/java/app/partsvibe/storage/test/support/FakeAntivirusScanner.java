package app.partsvibe.storage.test.support;

import app.partsvibe.shared.antivirus.AntivirusScanner;
import app.partsvibe.shared.antivirus.ScanResult;
import java.io.InputStream;
import java.util.Optional;

public class FakeAntivirusScanner implements AntivirusScanner {
    private volatile ScanResult nextResult = new ScanResult(ScanResult.Status.OK, Optional.empty());

    @Override
    public ScanResult scan(InputStream bytes) {
        return nextResult;
    }

    public void setNextResult(ScanResult nextResult) {
        this.nextResult = nextResult;
    }

    public void reset() {
        this.nextResult = new ScanResult(ScanResult.Status.OK, Optional.empty());
    }
}
