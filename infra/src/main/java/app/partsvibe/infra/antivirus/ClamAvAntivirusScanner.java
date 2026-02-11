package app.partsvibe.infra.antivirus;

import app.partsvibe.shared.antivirus.AntivirusScanException;
import app.partsvibe.shared.antivirus.AntivirusScanner;
import app.partsvibe.shared.antivirus.ScanResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClamAvAntivirusScanner implements AntivirusScanner {
    private static final Logger log = LoggerFactory.getLogger(ClamAvAntivirusScanner.class);
    private static final byte[] INSTREAM_COMMAND = "zINSTREAM\0".getBytes(StandardCharsets.US_ASCII);
    private static final int RESPONSE_MAX_BYTES = 4096;
    private static final int CHUNK_SIZE = 8192;

    private final String host;
    private final int port;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final Counter scansCounter;
    private final Counter scanErrorsCounter;
    private final Counter malwareFoundCounter;

    public ClamAvAntivirusScanner(
            @Value("${app.antivirus.host}") String host,
            @Value("${app.antivirus.port}") int port,
            @Value("${app.antivirus.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${app.antivirus.read-timeout-ms}") int readTimeoutMs,
            MeterRegistry meterRegistry) {
        this.host = host;
        this.port = port;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.scansCounter = meterRegistry.counter("app.antivirus.scans");
        this.scanErrorsCounter = meterRegistry.counter("app.antivirus.scan.errors");
        this.malwareFoundCounter = meterRegistry.counter("app.antivirus.scan.malware.found");
    }

    @Override
    public ScanResult scan(InputStream bytes) {
        scansCounter.increment();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMs);
            socket.setSoTimeout(readTimeoutMs);

            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();
            sendInstreamRequest(bytes, outputStream);
            String response = readResponse(inputStream);

            if (response.endsWith("OK")) {
                return new ScanResult(ScanResult.Status.OK, Optional.empty());
            }
            if (response.endsWith("FOUND")) {
                String foundMessage = extractMessage(response, "FOUND");
                malwareFoundCounter.increment();
                log.warn("ClamAV reported malware FOUND: {}", foundMessage);
                return new ScanResult(ScanResult.Status.MALWARE_FOUND, optionalMessage(foundMessage));
            }
            if (response.endsWith("ERROR")) {
                String errorMessage = extractMessage(response, "ERROR");
                scanErrorsCounter.increment();
                log.error("ClamAV reported scan ERROR: {}", errorMessage);
                throw new AntivirusScanException("ClamAV scan error: " + errorMessage);
            }

            scanErrorsCounter.increment();
            log.error("ClamAV returned unexpected scan response: {}", response);
            throw new AntivirusScanException("Unexpected ClamAV scan response: " + response);
        } catch (IOException e) {
            scanErrorsCounter.increment();
            log.error("I/O error while communicating with ClamAV.", e);
            throw new AntivirusScanException("Failed to communicate with ClamAV.", e);
        }
    }

    private static void sendInstreamRequest(InputStream source, OutputStream target) throws IOException {
        target.write(INSTREAM_COMMAND);
        byte[] buffer = new byte[CHUNK_SIZE];
        int bytesRead;
        while ((bytesRead = source.read(buffer)) != -1) {
            writeLength(target, bytesRead);
            target.write(buffer, 0, bytesRead);
        }
        writeLength(target, 0);
        target.flush();
    }

    private static void writeLength(OutputStream outputStream, int value) throws IOException {
        outputStream.write((value >>> 24) & 0xFF);
        outputStream.write((value >>> 16) & 0xFF);
        outputStream.write((value >>> 8) & 0xFF);
        outputStream.write(value & 0xFF);
    }

    private static String readResponse(InputStream inputStream) throws IOException {
        byte[] responseBytes = new byte[RESPONSE_MAX_BYTES];
        int offset = 0;
        while (offset < RESPONSE_MAX_BYTES) {
            int b = inputStream.read();
            if (b == -1 || b == 0 || b == '\n') {
                break;
            }
            responseBytes[offset++] = (byte) b;
        }
        if (offset == 0) {
            throw new IOException("Empty ClamAV response.");
        }
        return new String(responseBytes, 0, offset, StandardCharsets.UTF_8).trim();
    }

    private static String extractMessage(String response, String suffix) {
        String trimmed = response.trim();
        if (trimmed.endsWith(suffix)) {
            trimmed = trimmed.substring(0, trimmed.length() - suffix.length()).trim();
        }
        int separatorIndex = trimmed.indexOf(':');
        if (separatorIndex >= 0 && separatorIndex + 1 < trimmed.length()) {
            return trimmed.substring(separatorIndex + 1).trim();
        }
        return trimmed;
    }

    private static Optional<String> optionalMessage(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value);
    }
}
