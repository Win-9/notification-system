package com.example.seunggu.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.seunggu.notification.application.port.in.NotificationResult;
import com.example.seunggu.notification.application.port.in.RegisterNotificationCommand;
import com.example.seunggu.notification.application.port.out.NotificationPersistencePort;
import com.example.seunggu.notification.application.port.out.RegisteredEventPort;
import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.domain.NotificationChannel;
import com.example.seunggu.notification.domain.NotificationStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationRegistrarTest {

    @Mock
    private NotificationPersistencePort persistencePort;
    @Mock
    private RegisteredEventPort eventPort;
    @InjectMocks
    private NotificationRegistrar registrar;

    private RegisterNotificationCommand command() {
        return new RegisterNotificationCommand(NotificationChannel.KAKAO, "010-1234-5678", "제목", "내용");
    }

    private Notification saved(UUID id) {
        return Notification.restore(id, "key-1", NotificationChannel.KAKAO, "010-1234-5678",
                "제목", "내용", NotificationStatus.PENDING, LocalDateTime.now(), null);
    }

    @Test
    @DisplayName("저장한 알림을 결과로 매핑하고, 저장된 id 로 등록 이벤트를 발행한다")
    void persist_savesAndPublishesEvent() {
        UUID id = UUID.randomUUID();
        when(persistencePort.save(any(Notification.class))).thenReturn(saved(id));

        NotificationResult result = registrar.persist("key-1", command());

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getStatus()).isEqualTo(NotificationStatus.PENDING);
        verify(eventPort).publishRegistered(id);
    }

    @Test
    @DisplayName("요청 값 + 멱등키를 담아 PENDING 상태로 저장한다")
    void persist_savesWithCommandFields() {
        UUID id = UUID.randomUUID();
        when(persistencePort.save(any(Notification.class))).thenReturn(saved(id));

        registrar.persist("key-1", command());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(persistencePort).save(captor.capture());
        Notification toSave = captor.getValue();
        assertThat(toSave.getIdempotencyKey()).isEqualTo("key-1");
        assertThat(toSave.getChannel()).isEqualTo(NotificationChannel.KAKAO);
        assertThat(toSave.getRecipient()).isEqualTo("010-1234-5678");
        assertThat(toSave.getStatus()).isEqualTo(NotificationStatus.PENDING);
    }
}
