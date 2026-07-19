package com.example.seunggu.notification.domain;

/**
 * 알림 처리 상태.
 * PENDING(접수) -> SENT(발송 완료) 또는 FAILED(발송 실패).
 */
public enum NotificationStatus {
    PENDING,
    SENT,
    FAILED
}
