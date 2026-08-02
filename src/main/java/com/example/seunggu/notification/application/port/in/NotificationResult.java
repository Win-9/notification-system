package com.example.seunggu.notification.application.port.in;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.domain.NotificationChannel;
import com.example.seunggu.notification.domain.NotificationStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 유스케이스의 출력. 어댑터(웹 등)가 응답으로 변환해 쓴다.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationResult {

    private final UUID id;
    private final NotificationChannel channel;
    private final String recipient;
    private final String title;
    private final String message;

    /** 발송 결과 — 처리 상태. */
    private final NotificationStatus status;

    /** 접수 시각. */
    private final LocalDateTime createdAt;

    /** 발송 완료 시각. SENT 가 아니면 null. */
    private final LocalDateTime sentAt;

    public static NotificationResult from(Notification notification) {
        return new NotificationResult(
                notification.getId(),
                notification.getChannel(),
                notification.getRecipient(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getStatus(),
                notification.getCreatedAt(),
                notification.getSentAt());
    }
}
