package com.example.seunggu.notification.application.port.in;

import java.util.List;
import java.util.UUID;

import java.util.Optional;

/**
 * 인바운드 포트: 알림 상태 조회.
 */
public interface FindNotificationQuery {

    Optional<NotificationResult> findById(UUID id);

    /** 요청자(수신자)별 최근 7일 내역 — page 는 0부터, 최신순. */
    List<NotificationResult> findRecentByRecipient(String recipient, int page, int size);
}
