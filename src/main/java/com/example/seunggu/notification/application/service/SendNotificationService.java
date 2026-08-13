package com.example.seunggu.notification.application.service;

import java.util.UUID;

import com.example.seunggu.notification.application.port.in.SendNotificationUseCase;
import com.example.seunggu.notification.application.port.out.NotificationPersistencePort;
import com.example.seunggu.notification.application.port.out.SendPort;
import com.example.seunggu.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 발송 유스케이스. Kafka Consumer(인바운드 어댑터)가 호출한다.
 * 발송 실패 시 예외를 전파해 재시도(WAIT 토픽)를 유도하고,
 * 재시도 소진 시 DLT 핸들러가 markFailed / markDead 로 최종 상태를 확정한다.
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SendNotificationService implements SendNotificationUseCase {

    private final NotificationPersistencePort persistencePort;
    private final SendPort sendPort;
    private final NotificationStatusRecorder recorder;

    @Override
    public void send(UUID notificationId) {
        Notification notification = recorder.startProcessing(notificationId);
        if (notification == null) {
            log.info("발송 대상 아님 — 건너뜀 id={}", notificationId);
            return;
        }

        sendPort.send(notification);

        recorder.recordSent(notificationId);
        log.info("알림 발송 완료 id={}, channel={}, attempt={}",
                notificationId, notification.getChannel(), notification.getAttemptCount());
    }

    @Override
    @Transactional
    public void markRetryWait(UUID notificationId, String errorCode, String errorMessage) {
        persistencePort.findById(notificationId).ifPresent(n -> {
            n.markRetryWait(errorCode, errorMessage);
            persistencePort.save(n);
            log.warn("발송 일시 실패 — 재시도 대기 id={}, attempt={}, code={}",
                    notificationId, n.getAttemptCount(), errorCode);
        });
    }

    @Override
    @Transactional
    public void markFailed(UUID notificationId, String errorCode, String errorMessage) {
        persistencePort.findById(notificationId).ifPresentOrElse(
                notification -> {
                    notification.markFailed(errorCode, errorMessage);
                    persistencePort.save(notification);
                    log.error("[DLT] 최종 발송 실패 id={}, attempt={}, code={}",
                            notificationId, notification.getAttemptCount(), errorCode);
                },
                () -> log.error("[DLT] 알림 없음 id={}", notificationId)
        );
    }

    @Override
    @Transactional
    public void markDead(UUID notificationId, String errorCode, String errorMessage) {
        persistencePort.findById(notificationId).ifPresent(n -> {
            n.markDead(errorCode, errorMessage);
            persistencePort.save(n);
            log.error("[DEAD] 복구 불가 — 격리 id={}, attempt={}, code={}",
                    notificationId, n.getAttemptCount(), errorCode);
        });
    }
}
