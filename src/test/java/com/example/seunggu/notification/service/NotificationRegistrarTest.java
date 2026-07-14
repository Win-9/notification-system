package com.example.seunggu.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.domain.NotificationChannel;
import com.example.seunggu.notification.domain.NotificationStatus;
import com.example.seunggu.notification.dto.NotificationRequest;
import com.example.seunggu.notification.dto.NotificationResponse;
import com.example.seunggu.notification.event.NotificationRegisteredEvent;
import com.example.seunggu.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationRegistrarTest {

    @Mock
    private NotificationRepository repository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @InjectMocks
    private NotificationRegistrar registrar;

    private NotificationRequest request() {
        return new NotificationRequest(NotificationChannel.KAKAO, "010-1234-5678", "제목", "내용");
    }

    @Test
    @DisplayName("저장한 알림을 응답으로 매핑하고, 저장된 id 로 이벤트를 발행한다")
    void persist_savesAndPublishesEvent() {
        Notification saved = new Notification("key-1", NotificationChannel.KAKAO, "010-1234-5678", "제목", "내용");
        ReflectionTestUtils.setField(saved, "id", 1L);
        when(repository.save(any(Notification.class))).thenReturn(saved);

        NotificationResponse result = registrar.persist("key-1", request());

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(NotificationStatus.PENDING);

        ArgumentCaptor<NotificationRegisteredEvent> eventCaptor =
                ArgumentCaptor.forClass(NotificationRegisteredEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getNotificationId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("요청 값 + 멱등키를 담아 PENDING 상태로 저장한다")
    void persist_savesWithRequestFields() {
        Notification saved = new Notification("key-1", NotificationChannel.KAKAO, "010-1234-5678", "제목", "내용");
        ReflectionTestUtils.setField(saved, "id", 1L);
        when(repository.save(any(Notification.class))).thenReturn(saved);

        registrar.persist("key-1", request());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        Notification toSave = captor.getValue();
        assertThat(toSave.getIdempotencyKey()).isEqualTo("key-1");
        assertThat(toSave.getChannel()).isEqualTo(NotificationChannel.KAKAO);
        assertThat(toSave.getRecipient()).isEqualTo("010-1234-5678");
        assertThat(toSave.getStatus()).isEqualTo(NotificationStatus.PENDING);
    }
}
