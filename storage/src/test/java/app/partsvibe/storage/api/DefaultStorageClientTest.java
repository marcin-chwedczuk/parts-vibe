package app.partsvibe.storage.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.partsvibe.shared.cqrs.Mediator;
import app.partsvibe.shared.error.ApplicationException;
import org.junit.jupiter.api.Test;

class DefaultStorageClientTest {
    private final Mediator mediator = mock(Mediator.class);
    private final DefaultStorageClient client = new DefaultStorageClient(mediator);

    @Test
    void uploadWrapsApplicationExceptionIntoStorageException() {
        when(mediator.executeCommand(any())).thenThrow(new ApplicationException("boom"));

        assertThatThrownBy(() -> client.upload(
                        new StorageUploadRequest(StorageObjectType.USER_AVATAR_IMAGE, "a.png", new byte[] {1})))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Storage upload failed");
    }

    @Test
    void uploadRethrowsStorageException() {
        when(mediator.executeCommand(any())).thenThrow(new StorageException("storage failed"));

        assertThatThrownBy(() -> client.upload(
                        new StorageUploadRequest(StorageObjectType.USER_AVATAR_IMAGE, "a.png", new byte[] {1})))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("storage failed");
    }

    @Test
    void deleteReturnsFailedWhenMediatorThrowsApplicationException() {
        when(mediator.executeCommand(any())).thenThrow(new ApplicationException("boom"));

        DeleteFileResult result = client.delete(java.util.UUID.randomUUID());

        assertThat(result.status()).isEqualTo(DeleteFileResult.Status.FAILED);
    }

    @Test
    void deleteReturnsMediatorResultWhenCallSucceeds() {
        DeleteFileResult expected = DeleteFileResult.deleted();
        when(mediator.executeCommand(any())).thenReturn(expected);

        DeleteFileResult result = client.delete(java.util.UUID.randomUUID());

        assertThat(result).isEqualTo(expected);
    }
}
