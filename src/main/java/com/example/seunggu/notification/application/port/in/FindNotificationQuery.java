package com.example.seunggu.notification.application.port.in;

import java.util.Optional;

/**
 * 인바운드 포트: 알림 상태 조회.
 */
public interface FindNotificationQuery {

    Optional<NotificationResult> findById(Long id);
}
