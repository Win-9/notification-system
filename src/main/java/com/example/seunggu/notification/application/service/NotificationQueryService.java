package com.example.seunggu.notification.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.example.seunggu.notification.application.port.in.FindNotificationQuery;
import com.example.seunggu.notification.application.port.in.NotificationResult;
import com.example.seunggu.notification.application.port.out.NotificationPersistencePort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 상태 조회 유스케이스.
 */
@Service
@RequiredArgsConstructor
public class NotificationQueryService implements FindNotificationQuery {

    private final NotificationPersistencePort persistencePort;

    @Override
    @Transactional(readOnly = true)
    public Optional<NotificationResult> findById(UUID id) {
        return persistencePort.findById(id).map(NotificationResult::from);
    }

    private static final int MAX_PAGE_SIZE = 100;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResult> findRecentByRecipient(String recipient, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page 는 0 이상이어야 합니다.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size 는 1~" + MAX_PAGE_SIZE + " 사이여야 합니다.");
        }
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        return persistencePort
                .findByRecipient(recipient, since, page, size)
                .stream()
                .map(NotificationResult::from)
                .toList();
    }
}
