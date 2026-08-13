package com.example.seunggu.notification.application.port.in;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 인바운드 포트: 알림 상태 조회.
 */
public interface FindNotificationQuery {

    Optional<NotificationResult> findById(UUID id);

    /**
     * 요청자별 최근 7일 내역 — offset 페이징. page 는 0부터, 최신순.
     * 깊은 페이지에서 건너뛰기 비용이 선형 증가한다 (성능 비교용으로 유지).
     */
    List<NotificationResult> findRecentByRecipient(String recipient, int page, int size);

    /**
     * 요청자별 최근 7일 내역 — 커서(keyset) 페이징. 최신순.
     * @param cursor 직전 응답의 nextCursor. 첫 요청은 null.
     */
    CursorPage findRecentByRecipientWithCursor(String recipient, UUID cursor, int size);
}
