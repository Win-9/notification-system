package com.example.seunggu.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.seunggu.notification.application.port.out.NotificationPersistencePort;
import com.example.seunggu.notification.application.port.out.SendPort;
import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.domain.NotificationChannel;
import com.example.seunggu.notification.domain.NotificationStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SendNotificationServiceTest {

    @Mock
    private NotificationPersistencePort persistencePort;
    @Mock
    private SendPort sendPort;
    @Mock
    private NotificationStatusRecorder recorder;
    @InjectMocks
    private SendNotificationService service;

    private Notification notification(UUID id, NotificationStatus status) {
        return Notification.restore(id, "key-1", NotificationChannel.KAKAO, "010-1234-5678",
                "제목", "내용", status, LocalDateTime.now(), null);
    }

    @Test
    @DisplayName("발송 대상이면 발송기를 호출하고 SENT 로 확정 기록한다")
    void send_sendsAndRecordsSent() {
        UUID id = UUID.randomUUID();
        Notification processing = notification(id, NotificationStatus.PROCESSING);
        when(recorder.startProcessing(id)).thenReturn(processing);

        service.send(id);

        verify(sendPort).send(processing);
        verify(recorder).recordSent(id);
    }

    @Test
    @DisplayName("startProcessing 이 null 이면(이미 처리됨/없음) 발송하지 않고 건너뛴다")
    void send_notEligible_skips() {
        UUID id = UUID.randomUUID();
        when(recorder.startProcessing(id)).thenReturn(null);

        service.send(id);

        verifyNoInteractions(sendPort);
        verify(recorder, never()).recordSent(any());
    }

    @Test
    @DisplayName("markFailed 는 알림을 FAILED 로 바꿔 저장한다")
    void markFailed_marksFailed() {
        UUID id = UUID.randomUUID();
        Notification n = notification(id, NotificationStatus.PROCESSING);
        when(persistencePort.findById(id)).thenReturn(Optional.of(n));

        service.markFailed(id);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(persistencePort).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    @DisplayName("markFailed 대상 알림이 없으면 저장하지 않는다")
    void markFailed_notFound_noSave() {
        UUID id = UUID.randomUUID();
        when(persistencePort.findById(id)).thenReturn(Optional.empty());

        service.markFailed(id);

        verify(persistencePort, never()).save(any());
    }

    @Test
    @DisplayName("markRetryWait 은 알림을 RETRY_WAIT 로 바꿔 저장한다")
    void markRetryWait_marksRetryWait() {
        UUID id = UUID.randomUUID();
        Notification n = notification(id, NotificationStatus.PROCESSING);
        when(persistencePort.findById(id)).thenReturn(Optional.of(n));

        service.markRetryWait(id);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(persistencePort).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.RETRY_WAIT);
    }

    @Test
    @DisplayName("markDead 는 알림을 DEAD 로 격리 저장한다")
    void markDead_marksDead() {
        UUID id = UUID.randomUUID();
        Notification n = notification(id, NotificationStatus.PROCESSING);
        when(persistencePort.findById(id)).thenReturn(Optional.of(n));

        service.markDead(id);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(persistencePort).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.DEAD);
    }
}
