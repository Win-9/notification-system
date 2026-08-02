package com.example.seunggu.notification.adapter.in.kafka;

import java.util.UUID;

import com.example.seunggu.global.exception.NotificationSendException;
import com.example.seunggu.notification.application.port.in.SendNotificationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * 인바운드 어댑터(Kafka). 메시지를 유스케이스 호출로 번역한다.
 * 발송 실패 시 예외 전파 → 재시도 토픽(backoff 2s→4s→8s→16s) → 소진 시 DLT.
 * 트랜잭션 경계는 유스케이스(SendNotificationService) 쪽에 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final SendNotificationUseCase sendUseCase;

    @RetryableTopic(
            attempts = "5",
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    @KafkaListener(
            topics = "${notification.topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "3")
    public void consume(String notificationId) {
        UUID id = UUID.fromString(notificationId);
        try {
            sendUseCase.send(id);
        } catch (RuntimeException e) {
            // 실패 기록은 markRetryWait으로 남기고 예외처리
            sendUseCase.markRetryWait(id);
            throw new NotificationSendException("알림 발송 실패 — 재시도 예정 id=" + id, e);
        }
    }

    @DltHandler
    public void handleDlt(String notificationId,
                          @Header(name = KafkaHeaders.EXCEPTION_CAUSE_FQCN, required = false) String causeType) {
        log.error("[DLT] 도착 id={}, 원인={}", notificationId, causeType);

        UUID id;
        try {
            id = UUID.fromString(notificationId);
        } catch (IllegalArgumentException e) {
            log.error("[DLT] id 파싱 불가 — 마킹 불가, 수동 확인 필요 payload={}", notificationId);
            return;
        }

        // 우리가 인지하고 감싼 발송 실패(NotificationSendException)만 FAILED(운영자 복구 여지),
        // 그 외 예상 밖 오류는 재발송해도 소용없으므로 DEAD 로 격리한다.
        boolean sendFailure = String.valueOf(causeType).contains("NotificationSendException");
        if (sendFailure) {
            sendUseCase.markFailed(id);
        } else {
            sendUseCase.markDead(id);
        }
    }
}
