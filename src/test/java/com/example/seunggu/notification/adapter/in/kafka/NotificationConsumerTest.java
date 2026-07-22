package com.example.seunggu.notification.adapter.in.kafka;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.seunggu.global.exception.NotificationSendException;
import com.example.seunggu.notification.application.port.in.SendNotificationUseCase;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private SendNotificationUseCase sendUseCase;
    @InjectMocks
    private NotificationConsumer consumer;

    @Test
    @DisplayName("메시지를 소비하면 발송 유스케이스를 호출한다")
    void consume_delegatesToUseCase() {
        UUID id = UUID.randomUUID();

        consumer.consume(id.toString());

        verify(sendUseCase).send(id);
    }

    @Test
    @DisplayName("발송이 실패하면 RETRY_WAIT 기록 후 NotificationSendException 으로 감싸 전파한다")
    void consume_sendFails_recordsRetryAndPropagates() {
        UUID id = UUID.randomUUID();
        doThrow(new RuntimeException("api down")).when(sendUseCase).send(id);

        assertThatThrownBy(() -> consumer.consume(id.toString()))
                .isInstanceOf(NotificationSendException.class);
        verify(sendUseCase).markRetryWait(id);
    }

    @Test
    @DisplayName("DLT 원인이 NotificationSendException 이면 FAILED 로 확정한다")
    void handleDlt_sendFailureCause_marksFailed() {
        UUID id = UUID.randomUUID();

        consumer.handleDlt(id.toString(),
                "com.example.seunggu.global.exception.NotificationSendException");

        verify(sendUseCase).markFailed(id);
    }

    @Test
    @DisplayName("DLT 원인이 예상 밖 오류면 DEAD 로 격리한다")
    void handleDlt_unexpectedCause_marksDead() {
        UUID id = UUID.randomUUID();

        consumer.handleDlt(id.toString(), "java.lang.NullPointerException");

        verify(sendUseCase).markDead(id);
    }

    @Test
    @DisplayName("DLT 메시지의 id 를 UUID 로 파싱할 수 없으면 아무 것도 마킹하지 않는다")
    void handleDlt_unparseableId_noop() {
        consumer.handleDlt("not-a-uuid", "com.example.seunggu.global.exception.NotificationSendException");

        verifyNoInteractions(sendUseCase);
    }
}
