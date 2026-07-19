package com.example.seunggu.notification.domain;

/**
 * 알림 발송 채널. 설계안의 Mock 발송 API 분기 대상(카카오톡/이메일/SMS).
 */
public enum NotificationChannel {
    KAKAO,
    EMAIL,
    SMS
}
