package com.example.seunggu.notification.application.port.in;

import com.example.seunggu.notification.domain.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 알림 등록 유스케이스의 입력. 웹 DTO 와 코어를 분리하는 경계 객체.
 */
@Getter
@AllArgsConstructor
public class RegisterNotificationCommand {

    private final NotificationChannel channel;
    private final String recipient;
    private final String title;
    private final String message;
}
