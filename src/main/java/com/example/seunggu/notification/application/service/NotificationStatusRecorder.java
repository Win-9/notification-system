package com.example.seunggu.notification.application.service;

import java.time.Duration;
import java.util.UUID;

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
    private static final Duration PROCESSING_LEASE = Duration.ofSeconds(30);

    @Transactional
    public Notification startProcessing(UUID notificationId) {
        if (!persistencePort.claimForProcessing(notificationId, PROCESSING_LEASE)) {
            return null;
        }

        return persistencePort.findById(notificationId).orElse(null);
    }

    /** tx2: 발송 성공 확정 (PROCESSING → SENT). */
    @Transactional
    public void recordSent(UUID notificationId) {
        persistencePort.findById(notificationId).ifPresent(n -> {
            n.markSent();
            persistencePort.save(n);
        });
    }
}
