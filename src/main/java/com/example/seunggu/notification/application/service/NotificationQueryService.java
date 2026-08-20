package com.example.seunggu.notification.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.seunggu.notification.application.port.in.CursorPage;
import com.example.seunggu.notification.application.port.in.FindNotificationQuery;
import com.example.seunggu.notification.application.port.in.NotificationResult;
import com.example.seunggu.notification.application.port.out.NotificationPersistencePort;
import com.example.seunggu.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 조회 유스케이스. 조회 기간(7일)과 페이지 크기 상한 같은 정책이 여기 있다.
 */
@Service
@RequiredArgsConstructor
public class NotificationQueryService implements FindNotificationQuery {

    private final NotificationPersistencePort persistencePort;

    private static final int MAX_PAGE_SIZE = 100;
    private static final int HISTORY_DAYS = 7;

    @Override
    @Transactional(readOnly = true)
    public Optional<NotificationResult> findById(UUID id) {
        return persistencePort.findByIdIncludeArchive(id).map(NotificationResult::from);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResult> findRecentByRecipient(String recipient, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page 는 0 이상이어야 합니다.");
        }
        validateSize(size);
        return persistencePort
                .findByRecipient(recipient, since(), page, size)
                .stream()
                .map(NotificationResult::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage findRecentByRecipientWithCursor(String recipient, UUID cursor, int size) {
        validateSize(size);

        List<Notification> rows = persistencePort.findByRecipientWithCursor(recipient, since(), cursor, size + 1);

        if (rows.size() <= size) {
            return CursorPage.last(toResults(rows));
        }

        List<Notification> page = rows.subList(0, size);
        UUID nextCursor = page.get(page.size() - 1).getId();   // 마지막 항목의 id 가 다음 커서
        return CursorPage.of(toResults(page), nextCursor, true);
    }

    private List<NotificationResult> toResults(List<Notification> notifications) {
        return notifications.stream().map(NotificationResult::from).toList();
    }

    private LocalDateTime since() {
        return LocalDateTime.now().minusDays(HISTORY_DAYS);
    }

    private void validateSize(int size) {
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size 는 1~" + MAX_PAGE_SIZE + " 사이여야 합니다.");
        }
    }
}
