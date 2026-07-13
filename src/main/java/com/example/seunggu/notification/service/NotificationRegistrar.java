package com.example.seunggu.notification.service;

import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.dto.NotificationRequest;
import com.example.seunggu.notification.dto.NotificationResponse;
import com.example.seunggu.notification.event.NotificationRegisteredEvent;
import com.example.seunggu.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 DB 저장 + 발송 이벤트 발행을 담당하는 트랜잭션 경계.
 */
@Component
@RequiredArgsConstructor
public class NotificationRegistrar {

    private final NotificationRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public NotificationResponse persist(String key, NotificationRequest request) {
        Notification saved = repository.save(new Notification(
                key,
                request.getChannel(),
                request.getRecipient(),
                request.getTitle(),
                request.getMessage()
        ));

        // 커밋 이후 발송이 트리거되도록 이벤트 발행 (AFTER_COMMIT 리스너).
        eventPublisher.publishEvent(new NotificationRegisteredEvent(saved.getId()));
        return NotificationResponse.from(saved);
    }
}
