package com.example.seunggu.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
class NotificationStatusRecorderTest {

    @Mock
    private NotificationPersistencePort persistencePort;
    @InjectMocks
    private NotificationStatusRecorder recorder;

    private Notification notification(UUID id, NotificationStatus status) {
        return Notification.restore(id, "key-1", NotificationChannel.KAKAO, "010-1234-5678",
                "제목", "내용", status, LocalDateTime.now(), null);
    }

    @Test
    @DisplayName("PENDING 알림은 PROCESSING 으로 전이해 저장하고 반환한다")
    void startProcessing_pending_marksProcessing() {
        UUID id = UUID.randomUUID();
        when(persistencePort.findById(id)).thenReturn(Optional.of(notification(id, NotificationStatus.PENDING)));
        when(persistencePort.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        Notification result = recorder.startProcessing(id);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(NotificationStatus.PROCESSING);
        verify(persistencePort).save(any(Notification.class));
    }

    @Test
    @DisplayName("알림이 없으면 null 을 반환하고 저장하지 않는다")
    void startProcessing_notFound_returnsNull() {
        UUID id = UUID.randomUUID();
        when(persistencePort.findById(id)).thenReturn(Optional.empty());

        assertThat(recorder.startProcessing(id)).isNull();
        verify(persistencePort, never()).save(any());
    }

    @Test
    @DisplayName("이미 SENT(종결)인 알림은 멱등 가드로 null 을 반환하고 건드리지 않는다")
    void startProcessing_alreadySent_returnsNull() {
        UUID id = UUID.randomUUID();
        when(persistencePort.findById(id)).thenReturn(Optional.of(notification(id, NotificationStatus.SENT)));

        assertThat(recorder.startProcessing(id)).isNull();
        verify(persistencePort, never()).save(any());
    }

    @Test
    @DisplayName("recordSent 는 PROCESSING 알림을 SENT 로 확정 저장한다")
    void recordSent_marksSent() {
        UUID id = UUID.randomUUID();
        Notification n = notification(id, NotificationStatus.PROCESSING);
        when(persistencePort.findById(id)).thenReturn(Optional.of(n));

        recorder.recordSent(id);

        assertThat(n.getStatus()).isEqualTo(NotificationStatus.SENT);
        verify(persistencePort).save(n);
    }
}
