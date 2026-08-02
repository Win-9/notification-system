package com.example.seunggu.notification.application.port.in;

/**
 * 인바운드 포트: 알림 등록.
 * 멱등성 키로 중복을 판별하고, 최초 요청만 저장 후 발송을 트리거한다.
 */
public interface RegisterNotificationUseCase {

    NotificationResult register(String idempotencyKey, RegisterNotificationCommand command);
}
