package com.example.seunggu.notification.adapter.out.kafka;

import com.example.seunggu.global.config.KafkaTopicConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Kafka Producer.
 * 알림 등록 트랜잭션이 <b>커밋된 이후(AFTER_COMMIT)</b> 발송 요청 메시지를 토픽에 발행한다.
 * 커밋 전에 발행하면 Consumer 가 아직 저장되지 않은 알림을 조회할 수 있으므로 반드시 커밋 이후에 발행한다.
 * 메시지 key/value 는 알림 ID(문자열)이며, Consumer 가 이 ID 로 DB 에서 알림을 조회한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(NotificationRegisteredEvent event) {
        String id = String.valueOf(event.getNotificationId());
        kafkaTemplate.send(KafkaTopicConfig.NOTIFICATION_TOPIC, id, id);
        log.info("알림 발송 이벤트 발행 topic={}, id={}", KafkaTopicConfig.NOTIFICATION_TOPIC, id);
    }
}
