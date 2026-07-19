package com.example.seunggu.notification.application.service;

import com.example.seunggu.notification.application.port.in.NotificationResult;
import com.example.seunggu.notification.application.port.in.RegisterNotificationCommand;
import com.example.seunggu.notification.application.port.out.NotificationPersistencePort;
import com.example.seunggu.notification.application.port.out.RegisteredEventPort;
import com.example.seunggu.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 저장 + 발송 이벤트 발행을 담당하는 트랜잭션 경계.
 * 멱등성 선점(Redis)은 트랜잭션 밖(RegisterNotificationService)에 두고,
 * 커밋 실패가 호출부로 전파되면 그쪽에서 마커를 회수한다.
 */
@Component
@RequiredArgsConstructor
public class NotificationRegistrar {

    private final NotificationPersistencePort persistencePort;
    private final RegisteredEventPort eventPort;

    @Transactional
    public NotificationResult persist(String idempotencyKey, RegisterNotificationCommand command) {
        Notification saved = persistencePort.save(Notification.create(
                idempotencyKey,
                command.getChannel(),
                command.getRecipient(),
                command.getTitle(),
                command.getMessage()
        ));

        // 커밋 이후 발송이 트리거되도록 이벤트 발행 (AFTER_COMMIT 리스너).
        eventPort.publishRegistered(saved.getId());
        return NotificationResult.from(saved);
    }
}
