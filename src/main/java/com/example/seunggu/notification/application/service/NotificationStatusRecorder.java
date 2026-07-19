package com.example.seunggu.notification.application.service;

import com.example.seunggu.notification.application.port.out.NotificationPersistencePort;
import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.domain.NotificationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationStatusRecorder {
    private final NotificationPersistencePort persistencePort;

    @Transactional
    public Notification startProcessing(Long notificationId) {
        Notification notification = persistencePort.findById(notificationId).orElse(null);
        if (notification == null) {
            return null;
        }
        // 멱등 가드: 이미 끝났거나 확정된 알림은 재전달돼도 건드리지 않는다.
        NotificationStatus status = notification.getStatus();
        if (status == NotificationStatus.SENT
                || status == NotificationStatus.FAILED
                || status == NotificationStatus.DEAD) {
            return null;
        }
        notification.markProcessing();
        return persistencePort.save(notification);
    }

    /** tx2: 발송 성공 확정 (PROCESSING → SENT). */
    @Transactional
    public void recordSent(Long notificationId) {
        persistencePort.findById(notificationId).ifPresent(n -> {
            n.markSent();
            persistencePort.save(n);
        });
    }
}
