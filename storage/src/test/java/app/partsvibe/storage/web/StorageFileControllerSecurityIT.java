package app.partsvibe.storage.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.partsvibe.storage.queries.ResolveFileQuery;
import app.partsvibe.storage.test.web.AbstractStorageWebIntegrationTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

class StorageFileControllerSecurityIT extends AbstractStorageWebIntegrationTest {
    @BeforeEach
    void setUpMediatorHandlers() throws Exception {
        Path file = Files.createTempFile("storage-security-it-", ".bin");
        byte[] payload = "x".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(file, payload);
        mediator.onQuery(
                ResolveFileQuery.class,
                query -> new ResolveFileQuery.FileResource(file, "application/octet-stream", payload.length));
    }

    @Test
    void anonymousCannotAccessStorageFiles() throws Exception {
        mockMvc.perform(get("/storage/files/{fileId}", UUID.randomUUID())).andExpect(status().isForbidden());
        mockMvc.perform(get("/storage/files/{fileId}/thumbnail/{size}", UUID.randomUUID(), 128))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void authenticatedUserCanAccessStorageFiles() throws Exception {
        mockMvc.perform(get("/storage/files/{fileId}", UUID.randomUUID())).andExpect(status().isOk());
        mockMvc.perform(get("/storage/files/{fileId}/thumbnail/{size}", UUID.randomUUID(), 128))
                .andExpect(status().isOk());
    }
}
