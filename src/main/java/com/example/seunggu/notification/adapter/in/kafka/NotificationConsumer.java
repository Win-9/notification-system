package com.example.seunggu.notification.adapter.in.kafka;

import com.example.seunggu.global.config.KafkaTopicConfig;
import com.example.seunggu.notification.application.port.in.SendNotificationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
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
            topics = KafkaTopicConfig.NOTIFICATION_TOPIC,
            groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String notificationId) {
        sendUseCase.send(Long.valueOf(notificationId));
    }

    @DltHandler
    public void handleDlt(String notificationId) {
        sendUseCase.markFailed(Long.valueOf(notificationId));
    }
}
