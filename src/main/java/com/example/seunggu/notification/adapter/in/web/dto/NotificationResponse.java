package com.example.seunggu.notification.adapter.in.web.dto;

import com.example.seunggu.notification.application.port.in.NotificationResult;
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

    public static NotificationResponse from(NotificationResult result) {
        return new NotificationResponse(
                result.getId(),
                result.getChannel(),
                result.getRecipient(),
                result.getStatus()
        );
    }
}
