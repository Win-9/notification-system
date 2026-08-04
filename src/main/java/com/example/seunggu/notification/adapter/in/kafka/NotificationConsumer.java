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
            attempts = "${notification.retry.attempts:5}",
            backoff = @Backoff(
                    delayExpression = "${notification.retry.delay:2000}",
                    multiplierExpression = "${notification.retry.multiplier:3.0}",
                    maxDelayExpression = "${notification.retry.max-delay:60000}")
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
            // 실패 기록(원인 코드 포함)은 독립 트랜잭션으로 남기고, 예외는 반드시 재전파해 재시도를 유도한다.
            String errorCode = extractErrorCode(e);
            sendUseCase.markRetryWait(id, errorCode, e.getMessage());
            throw new NotificationSendException(errorCode, "알림 발송 실패 — 재시도 예정 id=" + id, e);
        }
    }

    @DltHandler
    public void handleDlt(String notificationId,
                          @Header(name = KafkaHeaders.EXCEPTION_CAUSE_FQCN, required = false) String causeType,
                          @Header(name = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String errorMessage) {
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
        // 최종 실패 시점의 원인 코드는 DLT 헤더에서만 알 수 있다. 상세 코드는 직전 재시도 기록에 남아 있으므로
        // 여기서는 "재시도 소진" 또는 "복구 불가"라는 종결 사유를 남긴다.
        if (sendFailure) {
            sendUseCase.markFailed(id, "RETRY_EXHAUSTED", errorMessage);
        } else {
            sendUseCase.markDead(id, "UNRECOVERABLE", causeType + " : " + errorMessage);
        }
    }

    /** 발송 어댑터가 분류한 코드를 우선 사용하고, 없으면 예외 타입명으로 대체한다. */
    private String extractErrorCode(RuntimeException e) {
        for (Throwable cause = e; cause != null && cause.getCause() != cause; cause = cause.getCause()) {
            if (cause instanceof NotificationSendException sendException) {
                return sendException.getErrorCode();
            }
        }
        return e.getClass().getSimpleName();
    }
}
