package com.example.seunggu.notification.adapter.out.outbox;

import java.util.UUID;

import com.example.seunggu.notification.application.port.out.RegisteredEventPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 아웃바운드 어댑터(Outbox). Kafka 직접 발행 대신 outbox 테이블에 INSERT 한다.
 * 호출부(NotificationRegistrar.persist)가 @Transactional 이므로
 * 알림 저장과 outbox 기록이 하나의 트랜잭션으로 원자적이다 —
 * 커밋됐는데 발행 기록이 없는 상태가 구조적으로 불가능하다.
 */
@Component
public class OutboxEventAdapter implements RegisteredEventPort {

    private final OutboxJpaRepository outboxRepository;
    private final String topic;

    public OutboxEventAdapter(OutboxJpaRepository outboxRepository,
                              @Value("${notification.topic}") String topic) {
        this.outboxRepository = outboxRepository;
        this.topic = topic;
    }

    @Override
    public void publishRegistered(UUID notificationId) {
        String id = notificationId.toString();
        outboxRepository.save(OutboxMessageJpaEntity.create(topic, id, id));
    }
}
