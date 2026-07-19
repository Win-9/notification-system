package com.example.seunggu.notification.adapter.out.kafka;

import com.example.seunggu.notification.application.port.out.RegisteredEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 아웃바운드 어댑터(이벤트). 트랜잭션 안에서 스프링 이벤트를 발행하고,
 * 실제 Kafka 발행은 커밋 이후 NotificationEventProducer(AFTER_COMMIT)가 수행한다.
 */
@Component
@RequiredArgsConstructor
public class RegisteredEventAdapter implements RegisteredEventPort {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publishRegistered(Long notificationId) {
        eventPublisher.publishEvent(new NotificationRegisteredEvent(notificationId));
    }
}
