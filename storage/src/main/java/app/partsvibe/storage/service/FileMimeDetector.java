package app.partsvibe.storage.service;

public interface FileMimeDetector {
    String detect(byte[] bytes, String originalFilename);
}
