package com.example.seunggu.notification.service;

import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.event.NotificationRegisteredEvent;
import com.example.seunggu.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 알림 서버 발송
 * 등록 트랜잭션 커밋 이후, thread pool 에서 비동기로 발송을 수행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final NotificationRepository repository;

    /**
     * 커밋 이후(AFTER_COMMIT) 별도 thread pool 에서 실행된다.
     * REQUIRES_NEW 로 새 트랜잭션을 열어 상태 변경을 저장한다.
     */
    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(NotificationRegisteredEvent event) {
        Notification notification = repository.findById(event.getNotificationId()).orElse(null);
        if (notification == null) {
            log.warn("발송 대상 알림 없음 id={}", event.getNotificationId());
            return;
        }

        try {
            // TODO: 실제 발송 로직 (sender 제거됨 — 추후 재구성 예정)
            log.info("[발송 예정] id={}, channel={}, to={}",
                    notification.getId(), notification.getChannel(), notification.getRecipient());
            notification.markSent();
        } catch (Exception e) {
            notification.markFailed();
            log.error("알림 발송 실패 id={}", notification.getId(), e);
        }
    }
}
