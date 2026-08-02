package com.example.seunggu.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.seunggu.global.exception.DuplicateRequestException;
import com.example.seunggu.notification.application.port.in.NotificationResult;
import com.example.seunggu.notification.application.port.in.RegisterNotificationCommand;
import com.example.seunggu.notification.application.port.out.IdempotencyPort;
import com.example.seunggu.notification.application.port.out.NotificationPersistencePort;
import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.domain.NotificationChannel;
import com.example.seunggu.notification.domain.NotificationStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterNotificationServiceTest {

    @Mock
    private IdempotencyPort idempotencyPort;
    @Mock
    private NotificationPersistencePort persistencePort;
    @Mock
    private NotificationRegistrar registrar;
    @InjectMocks
    private RegisterNotificationService service;

    private RegisterNotificationCommand validCommand() {
        return new RegisterNotificationCommand(NotificationChannel.KAKAO, "010-1234-5678", "제목", "내용");
    }

    private Notification persisted(UUID id) {
        return Notification.restore(id, "key-1", NotificationChannel.KAKAO, "010-1234-5678",
                "제목", "내용", NotificationStatus.PENDING, LocalDateTime.now(), null);
    }

    @Test
    @DisplayName("최초 요청이면 키를 선점하고 registrar.persist 결과를 반환한다")
    void firstRequest_persists() {
        UUID id = UUID.randomUUID();
        NotificationResult expected = NotificationResult.from(persisted(id));
        when(idempotencyPort.tryClaim("key-1")).thenReturn(true);
        when(registrar.persist(eq("key-1"), any(RegisterNotificationCommand.class))).thenReturn(expected);

        NotificationResult result = service.register("key-1", validCommand());

        assertThat(result).isEqualTo(expected);
        verify(registrar).persist(eq("key-1"), any(RegisterNotificationCommand.class));
    }

    @Test
    @DisplayName("선점 실패 + 이미 저장된 알림이 있으면 기존 결과를 재생(replay)하고 persist 는 호출하지 않는다")
    void claimFails_withExisting_replays() {
        UUID id = UUID.randomUUID();
        when(idempotencyPort.tryClaim("key-1")).thenReturn(false);
        when(persistencePort.findByIdempotencyKey("key-1")).thenReturn(Optional.of(persisted(id)));

        NotificationResult result = service.register("key-1", validCommand());

        assertThat(result.getId()).isEqualTo(id);
        verify(registrar, never()).persist(anyString(), any(RegisterNotificationCommand.class));
    }

    @Test
    @DisplayName("선점 실패 + 아직 저장된 알림이 없으면(처리 중) DuplicateRequestException 을 던진다")
    void claimFails_withoutExisting_throwsConflict() {
        when(idempotencyPort.tryClaim("key-1")).thenReturn(false);
        when(persistencePort.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register("key-1", validCommand()))
                .isInstanceOf(DuplicateRequestException.class);
        verify(registrar, never()).persist(anyString(), any(RegisterNotificationCommand.class));
    }

    @Test
    @DisplayName("persist 실패 후 실제로 저장된 행이 있으면(경합/유니크 제약) 마커를 유지하고 기존 결과를 재생한다")
    void persistFails_butRowExists_replaysAndKeepsMarker() {
        UUID id = UUID.randomUUID();
        when(idempotencyPort.tryClaim("key-1")).thenReturn(true);
        when(registrar.persist(anyString(), any(RegisterNotificationCommand.class)))
                .thenThrow(new RuntimeException("unique violation"));
        when(persistencePort.findByIdempotencyKey("key-1")).thenReturn(Optional.of(persisted(id)));

        NotificationResult result = service.register("key-1", validCommand());

        assertThat(result.getId()).isEqualTo(id);
        verify(idempotencyPort, never()).release(anyString());
    }

    @Test
    @DisplayName("persist 실패 후 저장된 행이 없으면 마커를 회수하고 원래 예외를 전파한다")
    void persistFails_noRow_releasesMarkerAndPropagates() {
        when(idempotencyPort.tryClaim("key-1")).thenReturn(true);
        when(registrar.persist(anyString(), any(RegisterNotificationCommand.class)))
                .thenThrow(new RuntimeException("boom"));
        when(persistencePort.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register("key-1", validCommand()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("boom");
        verify(idempotencyPort).release("key-1");
    }

    @Test
    @DisplayName("channel 이 null 이면 검증 예외를 던지고 어떤 포트/레지스트라도 건드리지 않는다")
    void invalidChannel_failsBeforePorts() {
        RegisterNotificationCommand bad =
                new RegisterNotificationCommand(null, "010-1234-5678", "제목", "내용");

        assertThatThrownBy(() -> service.register("key-1", bad))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(idempotencyPort, persistencePort, registrar);
    }

    @Test
    @DisplayName("recipient 가 비어 있으면 검증 예외를 던진다")
    void blankRecipient_fails() {
        RegisterNotificationCommand bad =
                new RegisterNotificationCommand(NotificationChannel.SMS, "  ", "제목", "내용");

        assertThatThrownBy(() -> service.register("key-1", bad))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(idempotencyPort, persistencePort, registrar);
    }

    @Test
    @DisplayName("message 가 비어 있으면 검증 예외를 던진다")
    void blankMessage_fails() {
        RegisterNotificationCommand bad =
                new RegisterNotificationCommand(NotificationChannel.SMS, "010-1234-5678", "제목", "  ");

        assertThatThrownBy(() -> service.register("key-1", bad))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
