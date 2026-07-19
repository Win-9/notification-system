package com.example.seunggu.notification.dto;

import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.domain.NotificationChannel;
import com.example.seunggu.notification.domain.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 알림 등록/조회 응답.
 */
@Getter
@AllArgsConstructor
public class NotificationResponse {

    private final Long id;
    private final NotificationChannel channel;
    private final String recipient;
    private final NotificationStatus status;

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getChannel(),
                n.getRecipient(),
                n.getStatus()
        );
    }
}
