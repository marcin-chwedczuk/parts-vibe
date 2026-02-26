package app.partsvibe.users.events.handling;

import static app.partsvibe.users.test.databuilders.UserAvatarChangeRequestTestDataBuilder.aUserAvatarChangeRequest;
import static app.partsvibe.users.test.databuilders.UserTestDataBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;

import app.partsvibe.storage.api.StorageObjectType;
import app.partsvibe.storage.api.events.FileReadyEvent;
import app.partsvibe.users.domain.avatar.UserAvatarChangeRequestStatus;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.repo.avatar.UserAvatarChangeRequestRepository;
import app.partsvibe.users.test.it.AbstractUsersIntegrationTest;
import app.partsvibe.users.test.it.UsersItTestApplication.InMemoryStorageClient;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserAvatarReadyEventHandlerIT extends AbstractUsersIntegrationTest {
    @Autowired
    private UserAvatarReadyEventHandler handler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAvatarChangeRequestRepository requestRepository;

    @Autowired
    private InMemoryStorageClient storageClient;

    @Autowired
    private EntityManager entityManager;

    @Test
    void appliesPendingAvatarChangeAndDeletesPreviousAvatar() {
        // given
        storageClient.clear();
        Instant now = Instant.parse("2026-02-25T12:00:00Z");
        timeProvider.setNow(now);

        UUID previousAvatarId = UUID.randomUUID();
        UUID newAvatarId = UUID.randomUUID();

        var user = aUser().withUsername("avatar-user@example.com").build();
        user.setAvatarId(previousAvatarId);
        user = userRepository.save(user);

        requestRepository.save(aUserAvatarChangeRequest()
                .withUser(user)
                .withNewAvatarFileId(newAvatarId)
                .withPreviousAvatarFileId(previousAvatarId)
                .withRequestedAt(now.minusSeconds(5))
                .build());

        // when
        handler.handle(FileReadyEvent.builder()
                .fileId(newAvatarId)
                .objectType(StorageObjectType.USER_AVATAR_IMAGE)
                .build());

        // then
        entityManager.flush();
        entityManager.clear();

        var updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getAvatarId()).isEqualTo(newAvatarId);

        var request = requestRepository.findByNewAvatarFileId(newAvatarId).orElseThrow();
        assertThat(request.getStatus()).isEqualTo(UserAvatarChangeRequestStatus.APPLIED);
        assertThat(request.getResolvedAt()).isEqualTo(now);
        assertThat(storageClient.deletedIds()).contains(previousAvatarId);
    }

    @Test
    void ignoresNonAvatarFileReadyEvents() {
        // given
        UUID fileId = UUID.randomUUID();

        // when
        handler.handle(FileReadyEvent.builder()
                .fileId(fileId)
                .objectType(StorageObjectType.PART_ATTACHMENT)
                .build());

        // then
        assertThat(requestRepository.findByNewAvatarFileId(fileId)).isEmpty();
    }

    @Test
    void ignoresAlreadyResolvedRequest() {
        // given
        Instant now = Instant.parse("2026-02-25T12:00:00Z");
        timeProvider.setNow(now);

        UUID newAvatarId = UUID.randomUUID();
        var user = userRepository.save(
                aUser().withUsername("avatar-user-2@example.com").build());
        var request = requestRepository.save(aUserAvatarChangeRequest()
                .withUser(user)
                .withNewAvatarFileId(newAvatarId)
                .withRequestedAt(now.minusSeconds(10))
                .build());
        request.setStatus(UserAvatarChangeRequestStatus.APPLIED);
        request.setResolvedAt(now.minusSeconds(1));
        requestRepository.save(request);

        // when
        handler.handle(FileReadyEvent.builder()
                .fileId(newAvatarId)
                .objectType(StorageObjectType.USER_AVATAR_IMAGE)
                .build());

        // then
        var saved = requestRepository.findByNewAvatarFileId(newAvatarId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(UserAvatarChangeRequestStatus.APPLIED);
        assertThat(saved.getResolvedAt()).isEqualTo(now.minusSeconds(1));
    }
}
