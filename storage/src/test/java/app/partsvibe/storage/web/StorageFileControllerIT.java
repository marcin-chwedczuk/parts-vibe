package app.partsvibe.storage.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.partsvibe.storage.api.StorageFileVariant;
import app.partsvibe.storage.errors.StoredFileNotFoundException;
import app.partsvibe.storage.queries.ResolveFileQuery;
import app.partsvibe.storage.test.web.AbstractStorageWebIntegrationTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

class StorageFileControllerIT extends AbstractStorageWebIntegrationTest {
    @Test
    @WithMockUser
    void readOriginalReturnsFileBodyAndHeaders() throws Exception {
        UUID fileId = UUID.randomUUID();
        Path file = Files.createTempFile("storage-controller-it-", ".bin");
        byte[] payload = "abc123".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(file, payload);

        mediator.onQuery(ResolveFileQuery.class, query -> {
            if (!query.fileId().equals(fileId) || query.variant() != StorageFileVariant.ORIGINAL) {
                throw new IllegalStateException("Unexpected query arguments.");
            }
            return new ResolveFileQuery.FileResource(file, "image/png", payload.length);
        });

        mockMvc.perform(get("/storage/files/{fileId}", fileId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Content-Length", String.valueOf(payload.length)))
                .andExpect(content().bytes(payload));
    }

    @Test
    @WithMockUser
    void readThumbnailReturnsNotFoundForUnsupportedThumbnailSize() throws Exception {
        mockMvc.perform(get("/storage/files/{fileId}/thumbnail/{size}", UUID.randomUUID(), 256))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void readReturnsNotFoundWhenFileMissing() throws Exception {
        UUID fileId = UUID.randomUUID();
        mediator.onQuery(ResolveFileQuery.class, query -> {
            throw new StoredFileNotFoundException(fileId);
        });

        mockMvc.perform(get("/storage/files/{fileId}", fileId)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void readFallsBackToOctetStreamForInvalidMimeType() throws Exception {
        UUID fileId = UUID.randomUUID();
        Path file = Files.createTempFile("storage-controller-it-", ".bin");
        byte[] payload = "abc123".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(file, payload);

        mediator.onQuery(
                ResolveFileQuery.class,
                query -> new ResolveFileQuery.FileResource(file, "invalid-mime", payload.length));

        mockMvc.perform(get("/storage/files/{fileId}", fileId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/octet-stream"));
    }
}
