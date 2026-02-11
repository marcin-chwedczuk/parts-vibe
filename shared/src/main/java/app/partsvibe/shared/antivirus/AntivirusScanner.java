package app.partsvibe.shared.antivirus;

import java.io.InputStream;

public interface AntivirusScanner {
    ScanResult scan(InputStream bytes);
}
