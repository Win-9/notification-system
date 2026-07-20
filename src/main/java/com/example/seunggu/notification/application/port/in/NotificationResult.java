package com.example.seunggu.notification.application.port.in;

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
    private final NotificationStatus status;

    public static NotificationResult from(Notification notification) {
        return new NotificationResult(
                notification.getId(),
                notification.getChannel(),
                notification.getRecipient(),
                notification.getStatus());
    }
}
