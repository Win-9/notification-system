package com.example.seunggu.notification.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.seunggu.global.resolver.NotificationSenderResolver;
import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.domain.NotificationChannel;
import com.example.seunggu.notification.domain.NotificationStatus;
import com.example.seunggu.notification.repository.NotificationRepository;
import com.example.seunggu.notification.sender.NotificationSender;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private NotificationRepository repository;
    @Mock
    private NotificationSenderResolver senderResolver;
    @Mock
    private NotificationSender sender;
    @InjectMocks
    private NotificationConsumer consumer;

    private Notification pending() {
        return new Notification("key-1", NotificationChannel.KAKAO, "010-1234-5678", "제목", "내용");
    }

    @Test
    @DisplayName("PENDING 알림을 소비하면 발송기를 호출하고 SENT 로 바꾼다")
    void consume_sendsAndMarksSent() {
        Notification n = pending();
        when(repository.findById(1L)).thenReturn(Optional.of(n));
        when(senderResolver.resolve(NotificationChannel.KAKAO)).thenReturn(sender);

        consumer.consume("1");

        verify(sender).send(n);
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    @DisplayName("이미 SENT 인 알림은 재발송 안 함")
    void consume_alreadySent_skips() {
        Notification n = pending();
        n.markSent();
        when(repository.findById(1L)).thenReturn(Optional.of(n));

        consumer.consume("1");

        verifyNoInteractions(senderResolver);
    }

    @Test
    @DisplayName("알림이 없으면 아무 발송도 하지 않는다")
    void consume_notFound_noop() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        consumer.consume("1");

        verifyNoInteractions(senderResolver);
    }

    @Test
    @DisplayName("발송이 실패하면 예외를 전파하고 상태는 SENT 로 바뀌지 않는다")
    void consume_sendFails_propagates() {
        Notification n = pending();
        when(repository.findById(1L)).thenReturn(Optional.of(n));
        when(senderResolver.resolve(NotificationChannel.KAKAO)).thenReturn(sender);
        doThrow(new RuntimeException("api down")).when(sender).send(n);

        assertThatThrownBy(() -> consumer.consume("1"))
                .isInstanceOf(RuntimeException.class);
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    @DisplayName("DLT 핸들러는 알림을 FAILED 로 확정한다")
    void handleDlt_marksFailed() {
        Notification n = pending();
        when(repository.findById(1L)).thenReturn(Optional.of(n));

        consumer.handleDlt("1");

        assertThat(n.getStatus()).isEqualTo(NotificationStatus.FAILED);
    }
}
