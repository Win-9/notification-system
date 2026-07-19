package com.example.seunggu.notification.application.service;

import com.example.seunggu.notification.application.port.in.SendNotificationUseCase;
import com.example.seunggu.notification.application.port.out.NotificationPersistencePort;
import com.example.seunggu.notification.application.port.out.SendPort;
import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.domain.NotificationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 발송 유스케이스. Kafka Consumer(인바운드 어댑터)가 호출한다.
 * 발송 실패 시 예외를 전파해 재시도(WAIT 토픽)를 유도하고,
 * 재시도 소진 시 DLT 핸들러가 markFailed 로 최종 실패를 확정한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SendNotificationService implements SendNotificationUseCase {

    private final NotificationPersistencePort persistencePort;
    private final SendPort sendPort;

    @Override
    @Transactional
    public void send(Long notificationId) {
        Notification notification = persistencePort.findById(notificationId).orElse(null);
        if (notification == null) {
            log.warn("발송 대상 알림 없음 id={}", notificationId);
            return;
        }

        // 멱등 가드: Kafka at-least-once 재전달 시 중복 발송 방지.
        if (notification.getStatus() == NotificationStatus.SENT) {
            log.info("이미 발송된 알림 — 건너뜀 id={}", notificationId);
            return;
        }

        sendPort.send(notification);
        notification.markSent();
        // 도메인-엔티티 분리로 dirty checking 이 없으므로 명시적으로 저장한다.
        persistencePort.save(notification);
        log.info("알림 발송 완료 id={}, channel={}", notificationId, notification.getChannel());
    }

    @Override
    @Transactional
    public void markFailed(Long notificationId) {
        persistencePort.findById(notificationId).ifPresentOrElse(
                notification -> {
                    notification.markFailed();
                    persistencePort.save(notification);
                    log.error("[DLT] 최종 발송 실패 — 격리됨 id={}", notificationId);
                },
                () -> log.error("[DLT] 알림 없음 id={}", notificationId)
        );
    }
}
